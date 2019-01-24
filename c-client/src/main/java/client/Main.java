package client;

import static common.SortingTask.Status.OK;
import static java.lang.Integer.parseUnsignedInt;
import static java.lang.Long.parseUnsignedLong;

public class Main {

  public static void main(String[] args) {
    // Usage: client.sh  server port size sleep count

    Config config = Config.create()
        .setServer(args[0])
        .setPort(parseUnsignedInt(args[1]))
        .setArraySize(parseUnsignedInt(args[2]))
        .setSleepDelta(parseUnsignedLong(args[3]))
        .setRequestsNumber(parseUnsignedInt(args[4]))
        .build();

    Tank tank = new Tank(config);
    tank.run();
    if (tank.getResultStatus() != OK) System.exit(0);
  }

}
