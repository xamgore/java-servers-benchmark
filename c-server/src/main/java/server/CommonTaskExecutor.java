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
  private final Set<Connection> activeClients;
  private final Executor taskExecutor = newFixedThreadPool(4);

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
          Connection connection = new Connection(server.accept());
          activeClients.add(connection);
          connection.runningThread.start();
        } catch (SocketTimeoutException ignored) {}
      }

    } catch (IOException e) {
      // todo: statistics is broken, must repeat
      e.printStackTrace();
    }

    stopActiveConnections();
  }

  private void stopActiveConnections() {
    synchronized (activeClients) {
      activeClients.forEach(client -> client.runningThread.interrupt());
    }
  }


  private class Connection implements Runnable {

    final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    Thread runningThread;
    ExecutorService sendResponseExecutor;

    public Connection(Socket socket) throws IOException {
      sendResponseExecutor = newSingleThreadExecutor();
      runningThread = new Thread(this);

      this.socket = socket;
      in = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());
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

          taskExecutor.execute(processMessage(buffer));
        }
      } catch (EOFException ignored) {
        // client stopped sending requests
      } catch (IOException e) {
        // todo: statistics is broken, must repeat
        e.printStackTrace();
      } finally {
        activeClients.remove(this);

        try {
          this.sendResponseExecutor.shutdown();
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          clientsProcessed.incrementAndGet();
        }
      }
    }

    private Runnable processMessage(byte[] buffer) {
      return () -> {
        try {
          Stopwatch requestTime = new Stopwatch().start();

          Stopwatch sortingTime = new Stopwatch().start();
          IntArray tasktoDo = parseFrom(buffer);
          IntArray result = SortingTask.complete(tasktoDo);
          sortingTime.stop();

          sendResponseExecutor.execute(sendResponse(result, requestTime, sortingTime));
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }

      };
    }

    private Runnable sendResponse(IntArray result, Stopwatch requestTime, Stopwatch sortingTime) {
      return () -> {
        try {
          out.writeInt(result.getSerializedSize());
          result.writeTo(out);
          out.flush();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          requestTime.stop();

          commonRequestTime.addAndGet(requestTime.getDuration());
          commonSortingTime.addAndGet(sortingTime.getDuration());
        }
      };
    }

  }

}
