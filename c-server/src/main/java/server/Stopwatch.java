package server;

public class Stopwatch {

  private long startTime;
  private int numberOfStops = 0;
  private long totalDuration = 0;

  public Stopwatch start() {
    startTime = System.currentTimeMillis();
    return this;
  }

  public Stopwatch stop() {
    long endTime = System.currentTimeMillis();
    totalDuration += endTime - startTime;
    numberOfStops += 1;
    return this;
  }

  public double getDuration() {
    return numberOfStops == 0 ? 0 : (totalDuration + 0D) / numberOfStops;
  }

}
