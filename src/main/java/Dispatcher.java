import client.Tank;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Dispatcher {

  private static final Retrofit retrofit =
      new Retrofit.Builder().baseUrl("http://localhost:5400/").build();

  private static final RemoteDispatcherService remoteDispatcher =
      retrofit.create(RemoteDispatcherService.class);


  public void stopRemoteServer() throws IOException {
    remoteDispatcher.stop().execute();
  }

  public void startRemoteServer(int architectureIdx) throws IOException {
    remoteDispatcher.start(architectureIdx).execute();
  }


  public interface RemoteDispatcherService {

    @GET("/stop")
    Call<Void> stop();

    @GET("/start")
    Call<Void> start(@Query("arch") int archIdx);

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
    // todo: fetch statistics from the server

    System.out.println(result.clientAverageTimePerRequest);
    return result;
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

  public static class AttackResult {

    public double clientAverageTimePerRequest;
    public int varyingParameter;

    public AttackResult(int varyingParameter) {
      this.varyingParameter = varyingParameter;
    }

  }

}
