package server;

public interface Architecture extends Runnable {

  double getTotalSortingTime();

  double getTotalRequestTime();

  int getClientsNumberProcessed();

  default double getAvgSortingTime() {
    double totalTime = getTotalSortingTime();
    return Math.abs(totalTime) < 0.01 ? 0 : totalTime / getClientsNumberProcessed();
  }

  default double getAvgRequestTime() {
    double totalTime = getTotalRequestTime();
    return Math.abs(totalTime) < 0.01 ? 0 : totalTime / getClientsNumberProcessed();
  }

}
