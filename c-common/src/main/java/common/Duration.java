package common;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Duration {

  public static class Timer {

    private volatile long requestStart;
    private volatile long requestEnd;
    private volatile long sortingStart;
    private volatile long sortingEnd;

    public Timer trackRequest() {
      requestStart = now();
      return this;
    }

    public void breakRequest() {
      requestEnd = now();
    }

    public void trackSorting() {
      sortingStart = now();
    }

    public void breakSorting() {
      sortingEnd = now();
    }

    public Timer trackAll() {
      requestStart = sortingStart = now();
      return this;
    }

    private static long now() {
      return System.currentTimeMillis();
    }

  }

  public static class Client {

    public final Queue<Timer> list = new ConcurrentLinkedQueue<>();

    public Timer newTimer() {
      Timer timer = new Timer();
      list.add(timer);
      return timer;
    }

  }

  public final Queue<Client> clients = new ConcurrentLinkedQueue<>();

  public Client newClient() {
    Client instance = new Client();
    clients.add(instance);
    return instance;
  }

}
