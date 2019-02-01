package server;

import com.google.common.util.concurrent.AtomicDouble;
import common.IntArrayOuterClass.ArrayMsg;
import common.SortingUtil;
import common.Stopwatch;

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
  private final Set<Connection> activeClients;
  private AtomicDouble commonSortingTime;
  private AtomicDouble commonRequestTime;
  private AtomicInteger clientsProcessed;

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
    commonSortingTime = new AtomicDouble();
    commonRequestTime = new AtomicDouble();
    clientsProcessed = new AtomicInteger();

    try (ServerSocket server = new ServerSocket(port)) {
      server.setSoTimeout(100);

      while (!Thread.interrupted()) {
        try {
          Connection connection = new Connection(server.accept());
          activeClients.add(connection);
          connection.runningThread.start();
        } catch (SocketTimeoutException ignored) {}
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    synchronized (activeClients) {
      activeClients.forEach(client -> client.runningThread.interrupt());
    }
  }


  private class Connection implements Runnable {

    final Socket socket;
    Thread runningThread;
    final Stopwatch sortingStopwatch;
    final Stopwatch requestStopwatch;

    public Connection(Socket socket) {
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
          {
            sortingStopwatch.start();
            ArrayMsg arrayToSort = ArrayMsg.parseFrom(buffer);
            ArrayMsg result = SortingUtil.process(arrayToSort);
            sortingStopwatch.stop();

            out.writeInt(result.getSerializedSize());
            result.writeTo(out);
            out.flush();
          }
          requestStopwatch.stop();
        }
      } catch (EOFException ignored) {
        // client stopped sending requests
      } catch (IOException e) {
        // todo: statistics is broken, must repeat
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
