import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.io.IOException;

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


}
