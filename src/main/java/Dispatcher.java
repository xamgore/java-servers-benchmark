import client.Tank;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Dispatcher {

  private static Retrofit retrofit =
      new Retrofit.Builder()
          .addConverterFactory(ScalarsConverterFactory.create())
          .baseUrl("http://localhost:5400/").build();

  private static final RemoteDispatcherService remoteDispatcher =
      retrofit.create(RemoteDispatcherService.class);

  static public void init(String host) {
    retrofit = new Retrofit.Builder()
        .addConverterFactory(ScalarsConverterFactory.create())
        .baseUrl("http://" + host + ":5400/").build();
  }


  public void stopRemoteServer() throws IOException {
    remoteDispatcher.stop().execute();
  }

  public void startRemoteServer(int architectureIdx) throws IOException {
    remoteDispatcher.start(architectureIdx).execute();
  }

  private AttackResult setServerStats(AttackResult result) {
    result.serverAverageRequestTime = -100; // errnous values
    result.serverAverageSortingTime = -100;

    try {
      String twoNumbersWithDelimeter = remoteDispatcher.stats().execute().body();
      if (twoNumbersWithDelimeter == null) return result;

      String[] numbers = twoNumbersWithDelimeter.split(":");
      result.serverAverageRequestTime = Double.parseDouble(numbers[0]);
      result.serverAverageSortingTime = Double.parseDouble(numbers[1]);
    } catch (IOException ignored) {}

    return result;
  }

  public interface RemoteDispatcherService {

    @GET("/stop")
    Call<Void> stop();

    @GET("/start")
    Call<Void> start(@Query("arch") int archIdx);

    @GET("/stats")
    Call<String> stats();

  }

  public List<AttackResult> attack(AttackConfig setup) {
    List<AttackResult> results = new ArrayList<>();

    for (AttackConfig config : setup) {
      results.add(doAttackAndGatherStatistics(config));
    }

    return results;
  }

  private AttackResult doAttackAndGatherStatistics(AttackConfig config) {
    int clientsNumber = config.getClientsNumber();
    List<Thread> threads = new ArrayList<>(clientsNumber);
    List<Tank> tanks = new ArrayList<>(clientsNumber);

    AttackResult result = new AttackResult(config.getVaryingParameter());
    result.clientAverageTimePerRequest = runNThreads(config, threads, tanks);
    return setServerStats(result);
  }

  /**
   * @return the average time of a request processed on a client.
   * avg [(start client - shutdown client) / requests per client]
   */
  private double runNThreads(AttackConfig config, List<Thread> threads, List<Tank> tanks) {
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

    return tanks.stream().mapToDouble(Tank::getAverageTimePerRequest).average().orElse(-1);
//        .filter(t -> (t.getResultStatus() != OK) || (t.getRequestNum() != config.getRequestsNumber())).count();
  }

}
