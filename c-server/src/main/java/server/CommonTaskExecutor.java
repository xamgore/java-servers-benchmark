package server;

import com.google.protobuf.InvalidProtocolBufferException;
import common.Duration;
import common.Duration.Timer;
import common.IntArrayOuterClass.ArrayMsg;
import common.SortingUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import static common.IntArrayOuterClass.ArrayMsg.parseFrom;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class CommonTaskExecutor extends Architecture {

  private ServerSocket serverSocket;
  private boolean forceStopped = false;
  private final ExecutorService taskExecutor = newFixedThreadPool(4);


  public CommonTaskExecutor(int port) {
    super(port);
  }

  public void start() throws IOException {
    serverSocket = new ServerSocket(port);
    thread.start();
  }

  public void stop() throws IOException {
    forceStopped = true;
    serverSocket.close();
    thread.interrupt();
    taskExecutor.shutdown();
  }


  @Override public void run() {
    try {
      while (!(Thread.interrupted() || serverSocket.isClosed())) {
        new Connection(serverSocket.accept()).myThread.start();
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
    final ExecutorService sendResponseExecutor;
    final Duration.Client clientDuration;
    final Thread myThread;

    public Connection(Socket socket) throws IOException {
      this.socket = socket;
      in = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());
      sendResponseExecutor = newSingleThreadExecutor();
      clientDuration = commonDuration.newClient();
      myThread = new Thread(this);
    }

    @Override public void run() {
      try {
        // client makes a finite number of requests
        while (!Thread.interrupted()) {
          byte[] buffer = readMsg();
          if (buffer == null) break;

          Timer timer = clientDuration.newTimer().trackRequest();
          taskExecutor.execute(processMsg(buffer, timer));
        }
      } catch (IOException e) {
        facedIOException = true;
        e.printStackTrace();
      } finally {
        closeSocket(socket);
        this.sendResponseExecutor.shutdown();
      }
    }

    private Runnable processMsg(byte[] buffer, Timer timer) {
      return () -> {
        try {
          timer.trackSorting();
          ArrayMsg result = SortingUtil.sort(parseFrom(buffer));
          timer.breakSorting();

          sendResponseExecutor.execute(sendResponse(result, timer));
        } catch (InvalidProtocolBufferException e) {
          facedIOException = true;
          e.printStackTrace();
        }
      };
    }

    private Runnable sendResponse(ArrayMsg result, Timer timer) {
      return () -> {
        try {
          writeMsg(result);
        } catch (IOException e) {
          facedIOException = true;
          e.printStackTrace();
        } finally {
          timer.breakRequest();
        }
      };
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
