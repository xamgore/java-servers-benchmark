import client.Config;

import java.util.Iterator;

import static java.lang.Integer.parseUnsignedInt;

public class AttackConfig implements Iterable<AttackConfig> {

  private static final int SLEEP_PARAM = 0;
  private static final int CLIENTS_PARAM = 2;
  private static final int SIZE_PARAM = 1;

  private int requestsNumber;
  private int clientsNumber;
  private int arraySize;
  private int sleepDelta;
  private int to;
  private int step;
  private int architecture;
  private int param;
  private int paramValue;

  public AttackConfig(String requestsNumber, String sleepDelta, String clientsNumber, String arraySize, String from, String to, String step, int architecture, int param) {
    this.paramValue = parseUnsignedInt(from);
    this.sleepDelta = parseUnsignedInt(sleepDelta);
    this.requestsNumber = parseUnsignedInt(requestsNumber);
    this.clientsNumber = parseUnsignedInt(clientsNumber);
    this.arraySize = parseUnsignedInt(arraySize);
    this.to = parseUnsignedInt(to);
    this.step = parseUnsignedInt(step);
    this.architecture = architecture;
    this.param = param;
  }

  public int getRequestsNumber() { return this.requestsNumber; }

  public int getSleepDelta() {
    return param == SLEEP_PARAM ? paramValue : sleepDelta;
  }

  public int getClientsNumber() {
    return param == CLIENTS_PARAM ? paramValue : clientsNumber;
  }

  public int getArraySize() {
    return param == SIZE_PARAM ? paramValue : arraySize;
  }

  public int getArchitecture() {
    return architecture;
  }

  public Config toClientConfig() {
    return Config.create()
        .setRequestsNumber(requestsNumber)
        .setArraySize(getArraySize())
        .setSleepDelta(getSleepDelta())
        .build();
  }

  private static class ConfigIterator implements Iterator<AttackConfig> {

    AttackConfig config;

    private ConfigIterator(AttackConfig config) {
      this.config = config;
    }

    @Override public boolean hasNext() {
      return config.paramValue < config.to;
    }

    @Override public AttackConfig next() {
      config.paramValue += config.step;
      return config;
    }

  }


  @Override public Iterator<AttackConfig> iterator() {
    return new ConfigIterator(this);
  }

}
