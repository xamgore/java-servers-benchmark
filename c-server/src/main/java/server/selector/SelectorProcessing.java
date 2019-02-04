package server.selector;

import common.Duration.Timer;
import common.IntArrayOuterClass.ArrayMsg;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class SelectorProcessing implements Runnable {

  static class QueElement {

    SocketChannel channel;
    ArrayMsg msg;
    Timer timer;

    QueElement(SocketChannel channel) {
      this.channel = channel;
    }

    QueElement(SocketChannel channel, ArrayMsg msg, Timer timer) {
      this.channel = channel;
      this.timer = timer;
      this.msg = msg;
    }

  }

  public boolean facedIOException;
  private final Thread thread = new Thread(this);

  final Selector selector = openSelector();
  final ConcurrentLinkedQueue<QueElement> queue = new ConcurrentLinkedQueue<>();


  protected abstract void register(Selector selector, QueElement channel) throws ClosedChannelException;

  protected abstract void processKey(SelectionKey key);


  public void start() {
    thread.start();
  }

  public void close() throws IOException {
    selector.close();
    thread.interrupt();
  }

  @Override public void run() {
    try {
      while (!Thread.interrupted() && selector.isOpen()) {
        attachSocketChannelsTo(selector);

        if (selector.select() == 0) continue;
        selector.selectedKeys().forEach(this::processKey);
        selector.selectedKeys().clear();
      }
    } catch (IOException e) {
      facedIOException = true;
      e.printStackTrace();
    }
  }

  private void attachSocketChannelsTo(Selector selector) throws ClosedChannelException {
    QueElement elem;
    while ((elem = queue.poll()) != null) {
      try {
        register(selector, elem);
      } catch (RuntimeException ex) {
        facedIOException = true;
        ex.printStackTrace();
      }
    }
  }

  private Selector openSelector() {
    try {
      return Selector.open();
    } catch (IOException e) {
      facedIOException = true;
      throw new RuntimeException("Can't initialize the selector", e);
    }
  }

}
