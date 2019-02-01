package client;

import common.IntArrayOuterClass.ArrayMsg;
import common.SortingUtil;
import common.SortingUtil.Status;
import common.Stopwatch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import static common.SortingUtil.Status.OK;
import static common.SortingUtil.checkIsCompleted;

// Runnable is for testing purpose
public class Tank implements Runnable {

  private int requestNum;
  private Status resultStatus;
  private final Config config;
  private final CountDownLatch latch;
  private Stopwatch stopwatch;

  public Tank(Config config) {
    this.config = config;
    this.latch = null;
  }

  public Tank(Config config, CountDownLatch latch) {
    this.config = config;
    this.latch = latch;
  }

  @Override public void run() {
    if (latch != null) latch.countDown();
    stopwatch = new Stopwatch();
    resultStatus = createTaskAndCheckAnswers();
  }


  public Status createTaskAndCheckAnswers() {
    try (
        Socket socket = new Socket(config.server, config.port);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());
    ) {
      for (requestNum = 0; requestNum < config.requestsNumber; requestNum++) {
        stopwatch.start();

        // create and send a task
        ArrayMsg task = SortingUtil.create(config.arraySize);
        out.writeInt(task.getSerializedSize());
        task.writeTo(out);
        out.flush();

        // receive an answer, check it
        byte[] buffer = new byte[in.readInt()];
        in.readFully(buffer);
        resultStatus = checkIsCompleted(ArrayMsg.parseFrom(buffer), config.arraySize);

        stopwatch.stop();
        if (resultStatus != OK) return resultStatus;

        // todo: should catch a spurious wakeup?
        Thread.sleep(config.sleepDeltaMillis);
      }

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

    return OK;
  }


  public int getRequestNum() {
    return requestNum;
  }

  public Status getResultStatus() {
    return resultStatus;
  }

  public double getAverageTimePerRequest() {
    return stopwatch.getDuration();
  }

}
