package server;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.protobuf.InvalidProtocolBufferException;
import common.IntArrayOuterClass.IntArray;
import common.SortingTask;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static common.IntArrayOuterClass.IntArray.parseFrom;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class CommonTaskExecutor implements Architecture {

  private final int port;
  private final Set<ClientHolder> activeClients;
  private final Executor executor = newFixedThreadPool(4);
  private AtomicDouble commonSortingTime;
  private AtomicDouble commonRequestTime;
  private AtomicInteger clientsProcessed;


  public CommonTaskExecutor(int port) {
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
    private final DataInputStream in;
    private final DataOutputStream out;
    Thread runningThread;
    ExecutorService sendResponseExecutor;

    public ClientHolder(Socket socket) throws IOException {
      this.socket = socket;
      in = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());
      runningThread = new Thread(this);
      sendResponseExecutor = newSingleThreadExecutor();
    }

    @Override public void run() {
      commonSortingTime = new AtomicDouble();
      commonRequestTime = new AtomicDouble();
      clientsProcessed = new AtomicInteger();


      try {
        // client makes a finite number of requests
        while (!Thread.interrupted()) {
          byte[] buffer = new byte[in.readInt()];
          in.readFully(buffer);

          executor.execute(solveTask(buffer));
        }
      } catch (EOFException ignored) {
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        // remove self from the tracking list
        activeClients.remove(this);

        try {
          this.sendResponseExecutor.shutdown();
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }

        clientsProcessed.incrementAndGet();
      }
    }

    private Runnable solveTask(byte[] buffer) {
      return () -> {
        try {
          Stopwatch requestStopwatch = new Stopwatch().start();

          Stopwatch sortingStopwatch = new Stopwatch().start();
          IntArray task = parseFrom(buffer);
          IntArray result = SortingTask.complete(task);
          sortingStopwatch.stop();

          sendResponseExecutor.execute(sendResponse(result, requestStopwatch, sortingStopwatch));
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }

      };
    }

    private Runnable sendResponse(IntArray result, Stopwatch requestStopwatch, Stopwatch sortingStopwatch) {
      return () -> {
        try {
          out.writeInt(result.getSerializedSize());
          result.writeTo(out);
          out.flush();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          requestStopwatch.stop();

          commonRequestTime.addAndGet(requestStopwatch.getDuration());
          commonSortingTime.addAndGet(sortingStopwatch.getDuration());
        }
      };
    }

  }

}
