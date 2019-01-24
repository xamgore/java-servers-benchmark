package server;

import client.Config;
import client.Tank;
import org.junit.Test;

import static common.SortingTask.Status.OK;
import static org.junit.Assert.assertEquals;


public class CommonTaskExecutorTest {

  @Test
  public void testWithOneClient() throws InterruptedException {
    Config config = Config.create().setServer("localhost").setRequestsNumber(10).build();

    // server
    Thread server = new Thread(new CommonTaskExecutor(config.port));
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

    System.out.println("[Test] tanks are closed");
    server.interrupt();
    System.out.println("[Test] server interrupted");
    server.join();
    System.out.println("[Test] server stopped");

    assertEquals(OK, tank1.getResultStatus());
    assertEquals(config.requestsNumber, tank1.getRequestNum());

    assertEquals(OK, tank2.getResultStatus());
    assertEquals(config.requestsNumber, tank2.getRequestNum());
  }

}
