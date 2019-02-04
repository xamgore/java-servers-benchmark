package common;

import java.util.Iterator;
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


    public long getRequestDuration() {
      return requestEnd - requestStart;
    }

    public long getSortingDuration() {
      return sortingEnd - sortingStart;
    }

    @Override public String toString() {
      return String.format("Timer{request=%d, sorting=%d}", getRequestDuration(), getSortingDuration());
    }

  }


  public static class Client {

    public final Queue<Timer> list = new ConcurrentLinkedQueue<>();

    public Timer newTimer() {
      Timer timer = new Timer();
      list.add(timer);
      return timer;
    }

    public long connectionStart() {
      return list.stream().mapToLong(t -> t.requestStart).min().orElse(Long.MAX_VALUE);
    }

    public long connectionEnd() {
      return list.stream().mapToLong(t -> t.requestEnd).max().orElse(Long.MIN_VALUE);
    }

    public Client remainInRange(long start, long end) {
      Iterator<Timer> iterator = list.iterator();

      while (iterator.hasNext()) {
        Timer timer = iterator.next();

        if (timer.requestStart >= start && timer.requestEnd <= end)
          continue;

        iterator.remove();
      }

      return this;
    }

  }


  public final Queue<Client> clients = new ConcurrentLinkedQueue<>();
  public long lastConnect, firstDisconnect;

  public Client newClient() {
    Client instance = new Client();
    clients.add(instance);
    return instance;
  }

  public Duration remainOnlyHotDurations() {
    lastConnect = clients.stream().mapToLong(Client::connectionStart).max().orElse(Long.MAX_VALUE);
    firstDisconnect = clients.stream().mapToLong(Client::connectionEnd).min().orElse(Long.MIN_VALUE);
    clients.forEach(client -> client.remainInRange(lastConnect, firstDisconnect));
    return this;
  }

  public double avgRequestDuration() {
    return clients.stream()
        .flatMap(client -> client.list.stream())
        .mapToLong(Timer::getRequestDuration)
        .average()
        .orElse(0);
  }

  public double avgSortingDuration() {
    return clients.stream()
        .flatMap(client -> client.list.stream())
        .mapToLong(Timer::getSortingDuration)
        .average()
        .orElse(0);
  }

}
