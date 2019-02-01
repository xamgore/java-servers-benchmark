package server;

import client.Config;
import client.Tank;
import org.junit.Test;

import java.io.IOException;

import static common.SortingUtil.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class CommonTaskExecutorTest {

  @Test
  public void testWithOneClient() throws InterruptedException, IOException {
    Config config = Config.create().setHostAddress("localhost").setRequestsNumber(10).setArraySize(100).build();

    // server
    Architecture server = new OneThreadPerClient(config.port);
    server.start();

    Thread.sleep(1000);

    // client
    Tank tank1 = new Tank(config);
    Tank tank2 = new Tank(config);

    Thread thread1 = new Thread(tank1);
    Thread thread2 = new Thread(tank2);

    thread1.start();
    thread2.start();

    thread1.join();
    thread2.join();

    server.stop();
    assertFalse(server.hasFacedIOException());
    System.out.println("[Test] server stopped");

    assertEquals(OK, tank1.getResultStatus());
    assertEquals(config.requestsNumber, tank1.getRequestNum());

    assertEquals(OK, tank2.getResultStatus());
    assertEquals(config.requestsNumber, tank2.getRequestNum());
  }

}
