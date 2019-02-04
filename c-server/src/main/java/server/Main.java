package server;

import common.Duration;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;

import static java.lang.Integer.parseUnsignedInt;

public class Main extends NanoHTTPD {

  public static final int SERVER_PORT = 5400;

  private Architecture serverInstance;

  private Main(int port) {
    super(port);
  }

  public static void main(String[] args) {
    try {
      Main app = new Main(SERVER_PORT);
      app.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
      System.out.printf("\nRunning! Point your browsers to http://localhost:%d/\n\n", app.getListeningPort());
    } catch (IOException err) {
      System.err.println("Couldn't start server:\n");
      err.printStackTrace(System.err);
    }
  }

  @Override
  public Response serve(IHTTPSession session) {
    String url = session.getUri();
    Map<String, String> params = session.getParms();

    if (url.equals("/start")) {
      int archIdx = parseUnsignedInt(params.get("arch"));
      startServer(archIdx);
    } else if (url.equals("/stop")) {
      stopServer();
    } else if (url.equals("/stats")) {
      return getStats();
    } else if (!url.equals("/favicon.ico")) {
      System.out.println(session.getUri());
    }

    return newFixedLengthResponse("");
  }

  private void startServer(int architectureIdx) {
    System.out.printf("/start arch=%d\n", architectureIdx);
    stopServer();

    serverInstance = architectureIdx == 0
        ? new OneThreadPerClient(8080)
        : new CommonTaskExecutor(8080);

    try {
      serverInstance.start();
      System.out.printf("%s started\n", serverInstance.getClass().getSimpleName());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void stopServer() {
    System.out.println("/stop the current server");

    try {
      if (serverInstance != null)
        serverInstance.stop();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Response getStats() {
    if (serverInstance == null) return null;

    Duration timing = serverInstance.commonDuration.remainOnlyHotDurations();

    String msg = String.format("%d:%.2f:%.2f",
        serverInstance.hasFacedIOException() ? 1 : 0,
        timing.avgRequestDuration(), timing.avgSortingDuration());

    System.out.println(msg);
    return newFixedLengthResponse(msg);
  }

}
