package server;

import com.google.common.util.concurrent.AtomicDouble;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Architecture implements Runnable {

  protected final int port;
  protected final Thread thread = new Thread(this);
  protected boolean facedIOException = false; // todo: reject statistics
  protected AtomicDouble commonSortingTime = new AtomicDouble();
  protected AtomicDouble commonRequestTime = new AtomicDouble();
  protected AtomicInteger clientsProcessed = new AtomicInteger();

  public Architecture(int port) {
    this.port = port;
  }

  public double getTotalSortingTime() {
    return commonSortingTime.get();
  }

  public double getTotalRequestTime() {
    return commonRequestTime.get();
  }

  public int getClientsNumberProcessed() {
    return clientsProcessed.get();
  }

  public double getAvgSortingTime() {
    double totalTime = getTotalSortingTime();
    return Math.abs(totalTime) < 0.01 ? 0 : totalTime / getClientsNumberProcessed();
  }

  public double getAvgRequestTime() {
    double totalTime = getTotalRequestTime();
    return Math.abs(totalTime) < 0.01 ? 0 : totalTime / getClientsNumberProcessed();
  }

  public abstract void start() throws IOException;

  public abstract void stop() throws IOException;

  public boolean hasFacedIOException() {
    return facedIOException;
  }

}
