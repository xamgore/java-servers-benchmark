import client.Tank;
import common.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Dispatcher {

  private void stopRemoteServer(String host) throws IOException {
    RemoteDispatcherService.Factory.create(host).stop().execute();
  }

  private void startRemoteServer(String host, int architectureIdx) throws IOException {
    RemoteDispatcherService.Factory.create(host).start(architectureIdx).execute();
  }


  public List<AttackResult> attack(AttackConfig config) throws IOException {
    List<AttackResult> results = new ArrayList<>();

    for (AttackConfig configWithChangingParameter : config) {
      for (int attempts = 0; attempts < 3; attempts++) {
        startRemoteServer(config.host, config.getArchitecture());
        AttackResult result = doAttackAndGatherStatistics(configWithChangingParameter);
        stopRemoteServer(config.host);

        if (result.hasFailed) {
          continue;
        } else {
          results.add(result);
        }

        break;
      }
    }

    return results;
  }

  private AttackResult doAttackAndGatherStatistics(AttackConfig config) {
    int clientsNumber = config.getClientsNumber();
    List<Thread> threads = new ArrayList<>(clientsNumber);
    List<Tank> tanks = new ArrayList<>(clientsNumber);

    AttackResult result = new AttackResult(config.getVaryingParameter());
    Duration clientDuration = runNThreads(config, threads, tanks);
    result.clientAverageTimePerRequest = clientDuration.avgRequestDuration();

    return applyServerStats(config.host, result);
  }

  private AttackResult applyServerStats(String host, AttackResult result) {
    try {
      String twoNumbersWithDelimeter =
          RemoteDispatcherService.Factory.create(host).stats().execute().body();

      if (twoNumbersWithDelimeter == null) return result;

      String[] numbers = twoNumbersWithDelimeter.split(":");
      result.hasFailed = Integer.parseInt(numbers[0]) == 1;
      result.serverAverageRequestTime = Double.parseDouble(numbers[1]);
      result.serverAverageSortingTime = Double.parseDouble(numbers[2]);
    } catch (IOException ignored) {}

    return result;
  }

  /**
   * @return the average time of a request processed on a client.
   * avg [(client start - client shutdown) / requests per client]
   */
  private Duration runNThreads(AttackConfig config, List<Thread> threads, List<Tank> tanks) {
    CountDownLatch latch = new CountDownLatch(config.getClientsNumber());

    for (int idx = 0; idx < config.getClientsNumber(); idx++) {
      Tank tank = new Tank(config.toClientConfig(), latch);

      tanks.add(tank);
      threads.add(new Thread(tank));
    }

    threads.forEach(Thread::start);

    threads.forEach(thread -> {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    Duration duration = new Duration();
    tanks.forEach(tank -> duration.clients.add(tank.getClientDuration()));
    return duration.remainOnlyHotDurations();
  }

}
