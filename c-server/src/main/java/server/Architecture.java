package server;

import common.Duration;

import java.io.IOException;

public abstract class Architecture implements Runnable {

  protected final int port;
  protected final Thread thread = new Thread(this);
  protected boolean facedIOException = false;
  public final Duration commonDuration = new Duration();


  public Architecture(int port) {
    this.port = port;
  }

  public abstract void start() throws IOException;

  public abstract void stop() throws IOException;

  public boolean hasFacedIOException() {
    return facedIOException;
  }

}
