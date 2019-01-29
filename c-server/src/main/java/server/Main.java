package server;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Map;

import static java.lang.Integer.parseUnsignedInt;

public class Main extends NanoHTTPD {

  public static final int SERVER_PORT = 5400;

  private Thread serverInstance;
  private Architecture architecture;

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

//    String msg = "<html><body><h1>Hello server</h1>\n";
//    Map<String, String> parms = session.getParms();
//    if (parms.get("username") == null) {
//      msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
//    } else {
//      msg += "<p>Hello, " + parms.get("username") + "!</p>";
//    }
    return newFixedLengthResponse("");
  }

  private void startServer(int architectureIdx) {
    System.out.printf("/start arch=%d\n", architectureIdx);
    stopServer();

    architecture = architectureIdx == 0
        ? new OneThreadPerClient(8080)
        : new CommonTaskExecutor(8080);

    serverInstance = new Thread(architecture);
    serverInstance.start();
    System.out.printf("%s was started\n", architecture.getClass().getSimpleName());
  }

  private void stopServer() {
    System.out.println("/stop the current server");

    try {
      if (serverInstance == null) return;
      serverInstance.interrupt();
      serverInstance.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private Response getStats() {
    if (architecture == null) return null;
    String msg = String.format("%.2f:%.2f", architecture.getAvgRequestTime(), architecture.getAvgSortingTime());
    System.out.println(msg);
    return newFixedLengthResponse(msg);
  }

}
