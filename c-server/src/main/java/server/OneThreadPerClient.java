package server;

import common.IntArrayOuterClass.ArrayMsg;
import common.SortingUtil;
import common.Stopwatch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static common.IntArrayOuterClass.ArrayMsg.parseFrom;

public class OneThreadPerClient extends Architecture {

  private ServerSocket serverSocket;
  private final Set<Connection> activeClients;
  private boolean forceStopped = false;


  public OneThreadPerClient(int port) {
    super(port);
    this.activeClients = Collections.synchronizedSet(new HashSet<>());
  }

  public void start() throws IOException {
    serverSocket = new ServerSocket(port);
    thread.start();
  }

  public void stop() throws IOException {
    forceStopped = true;
    serverSocket.close();
    thread.interrupt();

    synchronized (activeClients) {
      // close sockets, threads will be closed by themselves
      activeClients.forEach(client -> closeSocket(client.socket));
    }
  }


  @Override public void run() {
    try {
      while (!(Thread.interrupted() || serverSocket.isClosed())) {
        Connection connection = new Connection(serverSocket.accept());
        activeClients.add(connection);
        connection.myThread.start();
      }
    } catch (IOException ex) {
      if (!(forceStopped && serverSocket.isClosed())) {
        // this will reject already collected statistics
        facedIOException = true;
        ex.printStackTrace();
      }
    }
  }


  private class Connection implements Runnable {

    final Socket socket;
    final DataInputStream in;
    final DataOutputStream out;
    final Stopwatch sortingStopwatch;
    final Stopwatch requestStopwatch;
    final Thread myThread;

    public Connection(Socket socket) throws IOException {
      this.socket = socket;
      in = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());
      sortingStopwatch = new Stopwatch();
      requestStopwatch = new Stopwatch();
      myThread = new Thread(this);
    }

    @Override public void run() {
      try {
        // client makes a finite number of requests
        while (!Thread.interrupted()) {
          byte[] buffer = readMsg();
          if (buffer == null) break;
          processMsg(buffer);
        }
      } catch (IOException e) {
        facedIOException = true;
        e.printStackTrace();
      } finally {
        // remove self from the tracking list
        activeClients.remove(this);
        closeSocket(socket);

        commonRequestTime.addAndGet(requestStopwatch.getDuration());
        commonSortingTime.addAndGet(sortingStopwatch.getDuration());
        clientsProcessed.incrementAndGet();
      }
    }

    private void processMsg(byte[] buffer) throws IOException {
      requestStopwatch.start();
      sortingStopwatch.start();
      ArrayMsg result = SortingUtil.sort(parseFrom(buffer));
      sortingStopwatch.stop();

      writeMsg(result);
      requestStopwatch.stop();
    }

    int readMsgSize() throws IOException {
      try {
        return in.readInt();
      } catch (EOFException ignored) {
        return -1;
      }
    }

    byte[] readMsg() throws IOException {
      byte[] buffer = null;
      int msgSize = readMsgSize();

      if (msgSize >= 0) {
        buffer = new byte[msgSize];
        in.readFully(buffer);
      }

      return buffer;
    }

    private void writeMsg(ArrayMsg msg) throws IOException {
      out.writeInt(msg.getSerializedSize());
      msg.writeTo(out);
      out.flush();
    }

  }

  private void closeSocket(Socket socket) {
    try {
      socket.close();
    } catch (IOException ex) {
      facedIOException = true;
      ex.printStackTrace();
    }
  }

}
