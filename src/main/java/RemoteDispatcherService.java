import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RemoteDispatcherService {

  @GET("/stop")
  Call<Void> stop();

  @GET("/start")
  Call<Void> start(@Query("arch") int archIdx);

  @GET("/stats")
  Call<String> stats();


  class Factory {

    public static RemoteDispatcherService create(String host) {
      Retrofit retrofit = new Retrofit.Builder()
          .addConverterFactory(ScalarsConverterFactory.create())
          .baseUrl("http://" + host + ":5400/").build();

      return retrofit.create(RemoteDispatcherService.class);
    }

  }

}
