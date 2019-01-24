package client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Config {

  public final InetAddress server;
  public final int port;
  public final int arraySize;
  public final long sleepDeltaMillis;
  public final int requestsNumber;

  private Config(InetAddress server, int port, int arraySize, long sleepDelta, int requestsNumber) {
    this.server = server;
    this.port = port;
    this.arraySize = arraySize;
    this.sleepDeltaMillis = sleepDelta;
    this.requestsNumber = requestsNumber;
  }

  static public ConfigBuilder create() {
    return new ConfigBuilder();
  }


  static class ConfigBuilder {

    private InetAddress server = setServer("localhost").server;
    private int port = 8080;
    private int arraySize = 20000;
    private long sleepDelta = 100;
    private int requestsNumber = 1;


    public Config build() {
      return new Config(server, port, arraySize, sleepDelta, requestsNumber);
    }

    public ConfigBuilder setServer(String server) {
      try {
        this.server = InetAddress.getByName(server);
      } catch (UnknownHostException e) {
        throw new RuntimeException("Can't parse \"" + server + "\" inet address");
      }
      return this;
    }

    public ConfigBuilder setPort(int port) {
      if (port > 0)
        this.port = port;
      return this;
    }

    public ConfigBuilder setArraySize(int arraySize) {
      if (arraySize > 0)
        this.arraySize = arraySize;
      return this;
    }

    public ConfigBuilder setSleepDelta(long sleepDelta) {
      if (sleepDelta > 0)
        this.sleepDelta = sleepDelta;
      return this;
    }

    public ConfigBuilder setRequestsNumber(int requestsNumber) {
      if (requestsNumber > 0)
        this.requestsNumber = requestsNumber;
      return this;
    }

  }

}
