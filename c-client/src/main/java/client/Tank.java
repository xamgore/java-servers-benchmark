package client;

import common.IntArrayOuterClass.IntArray;
import common.SortingTask;
import common.SortingTask.Status;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static common.SortingTask.Status.OK;
import static common.SortingTask.checkIsCompleted;

// Runnable is for testing purpose
public class Tank implements Runnable {

  private int requestNum;
  private Status resultStatus;
  private final Config config;

  public Tank(Config config) {
    this.config = config;
  }

  @Override public void run() {
    resultStatus = createTaskAndCheckAnswer();
  }


  public Status createTaskAndCheckAnswer() {
    try (
        Socket socket = new Socket(config.server, config.port);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());
    ) {
      for (requestNum = 0; requestNum < config.requestsNumber; requestNum++) {
        // create and send a task
        IntArray task = SortingTask.create(config.arraySize);
        out.writeInt(task.getSerializedSize());
        task.writeTo(out);
        out.flush();

        // receive an answer, check it
        byte[] buffer = new byte[in.readInt()];
        in.readFully(buffer);
        resultStatus = checkIsCompleted(IntArray.parseFrom(buffer), config.arraySize);
        if (resultStatus != OK) return resultStatus;

        // todo: should catch a spurious wakeup?
        Thread.sleep(config.sleepDeltaMillis);
      }

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

    return OK;
  }


  // for debug purpose, case when execution was stopped
  // todo: remove
  public int getRequestNum() {
    return requestNum;
  }

  public Status getResultStatus() {
    return resultStatus;
  }

}
