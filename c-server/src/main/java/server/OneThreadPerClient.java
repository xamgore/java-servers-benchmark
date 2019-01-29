package server;

import com.google.common.util.concurrent.AtomicDouble;
import common.IntArrayOuterClass.IntArray;
import common.SortingTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class OneThreadPerClient implements Architecture {

  private final int port;
  private final Set<ClientHolder> activeClients;
  private AtomicDouble commonSortingTime = new AtomicDouble();
  private AtomicDouble commonRequestTime = new AtomicDouble();
  private AtomicInteger clientsProcessed = new AtomicInteger();

  public OneThreadPerClient(int port) {
    this.port = port;
    this.activeClients = Collections.synchronizedSet(new HashSet<>());
  }


  @Override public double getTotalSortingTime() {
    return commonSortingTime.get();
  }

  @Override public double getTotalRequestTime() {
    return commonRequestTime.get();
  }

  @Override public int getClientsNumberProcessed() {
    return clientsProcessed.get();
  }


  @Override public void run() {
    try (ServerSocket server = new ServerSocket(port)) {
      server.setSoTimeout(100);

      while (!Thread.interrupted()) {
        try {
          ClientHolder client = new ClientHolder(server.accept());
          activeClients.add(client);
          client.runningThread.start();
        } catch (SocketTimeoutException ignored) {}
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    synchronized (activeClients) {
      activeClients.forEach(client -> client.runningThread.interrupt());
    }
  }


  private class ClientHolder implements Runnable {

    final Socket socket;
    Thread runningThread;
    final Stopwatch sortingStopwatch;
    final Stopwatch requestStopwatch;

    public ClientHolder(Socket socket) {
      this.socket = socket;
      runningThread = new Thread(this);
      sortingStopwatch = new Stopwatch();
      requestStopwatch = new Stopwatch();
    }

    @Override public void run() {
      try (
          DataInputStream in = new DataInputStream(socket.getInputStream());
          DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      ) {
        // client makes a finite number of requests
        while (!Thread.interrupted()) {
          byte[] buffer = new byte[in.readInt()];
          in.readFully(buffer);
          requestStopwatch.start();

          sortingStopwatch.start();
          IntArray task = IntArray.parseFrom(buffer);
          IntArray result = SortingTask.complete(task);
          sortingStopwatch.stop();

          out.writeInt(result.getSerializedSize());
          result.writeTo(out);
          out.flush();

          requestStopwatch.stop();
        }
      } catch (EOFException ignored) {
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        // remove self from the tracking list
        activeClients.remove(this);

        try {
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }

        commonRequestTime.addAndGet(requestStopwatch.getDuration());
        commonSortingTime.addAndGet(sortingStopwatch.getDuration());
        clientsProcessed.incrementAndGet();
      }
    }

  }

}
