import client.Tank;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static common.SortingTask.Status.OK;

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

  public void attack(AttackConfig setup) {
    for (AttackConfig config : setup) {
      int clientsNumber = setup.getClientsNumber();
      List<Thread> threads = new ArrayList<>(clientsNumber);
      List<Tank> tanks = new ArrayList<>(clientsNumber);
      long countFailedTanks = runNThreads(config, threads, tanks);
      System.out.println(countFailedTanks);
    }
  }

  private long runNThreads(AttackConfig config, List<Thread> threads, List<Tank> tanks) {
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

    return tanks.stream()
        .filter(t -> (t.getResultStatus() != OK) || (t.getRequestNum() != config.getRequestsNumber()))
        .count();
  }

}
