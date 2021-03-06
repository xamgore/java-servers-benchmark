package server.selector;

import server.Architecture;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class SelectorServer extends Architecture {

  private final ExecutorService taskExecutor = newFixedThreadPool(4);
  private final ResponseProcessing responseProcessing = new ResponseProcessing();
  private final RequestsProcessing requestsProcessing
      = new RequestsProcessing(taskExecutor, responseProcessing, commonDuration);
  protected boolean forceStopped = false;
  private ServerSocketChannel serverSocketChannel;


  public SelectorServer(int port) {
    super(port);
  }


  @Override public void start() throws IOException {
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(port));
    requestsProcessing.start();
    responseProcessing.start();
    thread.start();
  }

  @Override public void stop() throws IOException {
    forceStopped = true;
    taskExecutor.shutdown();
    serverSocketChannel.close();
    requestsProcessing.close();
    responseProcessing.close();
    thread.interrupt();
    facedIOException |= requestsProcessing.facedIOException || responseProcessing.facedIOException;
  }

  @Override public void run() {
    try {
      while (!Thread.interrupted() && serverSocketChannel.isOpen()) {
        requestsProcessing.addChannel(serverSocketChannel.accept());
      }
    } catch (IOException ex) {
      if (!serverSocketChannel.isOpen() && forceStopped)
        return;

      facedIOException = true;  // this will reject already collected statistics
      ex.printStackTrace();
    }
  }

}
