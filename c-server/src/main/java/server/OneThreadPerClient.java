package server;

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

public class OneThreadPerClient implements Runnable {

  private final int port;
  private final Set<ClientHolder> activeClients;

  public OneThreadPerClient(int port) {
    this.port = port;
    this.activeClients = Collections.synchronizedSet(new HashSet<>());
  }


  @Override public void run() {
    try (ServerSocket server = new ServerSocket(port)) {
      server.setSoTimeout(100);

      while (!Thread.interrupted()) {
        try {
          ClientHolder client = new ClientHolder(server.accept());
          client.runningThread = new Thread(client);
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

    public ClientHolder(Socket socket) { this.socket = socket; }

    @Override public void run() {
      try (
          DataInputStream in = new DataInputStream(socket.getInputStream());
          DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      ) {
        // client makes a finite number of requests
        while (!Thread.interrupted()) {
          byte[] buffer = new byte[in.readInt()];
          in.readFully(buffer);

          IntArray task = IntArray.parseFrom(buffer);
          IntArray result = SortingTask.complete(task);

          out.writeInt(result.getSerializedSize());
          result.writeTo(out);
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
      }
    }

  }


}
