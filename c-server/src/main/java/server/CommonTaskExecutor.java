package server;

import com.google.protobuf.InvalidProtocolBufferException;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static common.IntArrayOuterClass.IntArray.parseFrom;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class CommonTaskExecutor implements Runnable {

  private final int port;
  private final Set<ClientHolder> activeClients;
  private final Executor executor = newFixedThreadPool(4);

  public CommonTaskExecutor(int port) {
    this.port = port;
    this.activeClients = Collections.synchronizedSet(new HashSet<>());
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
      this.in = new DataInputStream(socket.getInputStream());
      this.out = new DataOutputStream(socket.getOutputStream());
      this.runningThread = new Thread(this);
      this.sendResponseExecutor = newSingleThreadExecutor();
    }

    @Override public void run() {
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
      }
    }

    private Runnable solveTask(byte[] buffer) {
      return () -> {
        try {
          IntArray task = parseFrom(buffer);
          IntArray result = SortingTask.complete(task);
          sendResponseExecutor.execute(sendResponse(result));
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }

      };
    }

    private Runnable sendResponse(IntArray result) {
      return () -> {
        try {
          out.writeInt(result.getSerializedSize());
          result.writeTo(out);
          out.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      };
    }

  }

}
