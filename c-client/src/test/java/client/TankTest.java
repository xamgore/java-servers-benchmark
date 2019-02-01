package client;

import common.IntArrayOuterClass.ArrayMsg;
import common.SortingUtil;
import common.SortingUtil.Status;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Function;

import static common.SortingUtil.Status.LENGTH_DIFFERS;
import static common.SortingUtil.Status.OK;
import static org.junit.Assert.assertEquals;

public class TankTest {

  @Test
  public void testStandardCase() throws InterruptedException {
    Status status = runClientServer(SortingUtil::sort);
    assertEquals(OK, status);
  }

  @Test
  public void testFailingCase() throws InterruptedException {
    Status status = runClientServer(arr -> SortingUtil.create(arr.getNumbersCount() - 1));
    assertEquals(LENGTH_DIFFERS, status);
  }

  private Status runClientServer(Function<ArrayMsg, ArrayMsg> serverProcess) throws InterruptedException {
    Config config = Config.create().setHostAddress("localhost").setRequestsNumber(1).build();

    // server
    Thread server = new Thread(() -> runServer(serverProcess, config));
    server.start();
    Thread.sleep(1000);

    // client
    Tank tank = new Tank(config);
    tank.run();

    server.join();
    return tank.getResultStatus();
  }

  private void runServer(Function<ArrayMsg, ArrayMsg> process, Config config) {
    try (
        ServerSocket server = new ServerSocket(config.port);
        Socket socket = server.accept();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    ) {
      System.out.println("[Server] accepted");

      byte[] buffer = new byte[in.readInt()];
      in.readFully(buffer);
      System.out.println("[Server] got: " + buffer.length);

      ArrayMsg task = ArrayMsg.parseFrom(buffer);
      System.out.println("[Server] parsed");

      ArrayMsg result = process.apply(task);
      System.out.println("[Server] processed");

      out.writeInt(result.getSerializedSize());
      result.writeTo(out);
      System.out.println("[Server] answered");
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("[Server] bye");
  }

}
