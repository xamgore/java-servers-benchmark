package server.selector;

import com.google.protobuf.InvalidProtocolBufferException;
import common.Duration;
import common.Duration.Timer;
import common.IntArrayOuterClass;
import common.SortingUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import static common.IntArrayOuterClass.ArrayMsg.parseFrom;
import static java.nio.channels.SelectionKey.OP_READ;

public class RequestsProcessing extends SelectorProcessing {

  private final Duration duration;
  private final ExecutorService taskExecutor;
  private final ResponseProcessing responseProcessing;

  public RequestsProcessing(ExecutorService taskExecutor, ResponseProcessing responseProcessing, Duration duration) {
    this.taskExecutor = taskExecutor;
    this.responseProcessing = responseProcessing;
    this.duration = duration;
  }

  @Override protected void register(Selector selector, QueElement elem) throws ClosedChannelException {
    elem.channel.register(selector, OP_READ, new Attachment(duration.newClient()));
  }

  protected void processKey(SelectionKey key) {
    int arraySize = ((Attachment) key.attachment()).msgSize;

    try {
      boolean isPartWithMsgSizeExpected = arraySize < 0;
      boolean isEndOfStream = isPartWithMsgSizeExpected
          ? readMsgSizeAndAllocateBuffer(key) : readArrayAndMakeNewTask(key);
      if (isEndOfStream) key.cancel();
    } catch (IOException ex) {
      facedIOException = true;
      key.cancel();
    }
  }

  /**
   * returns true on end-of-stream
   */
  private boolean readMsgSizeAndAllocateBuffer(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    Attachment att = (Attachment) key.attachment();

    if (channel.read(att.bufferForMsgSize) == -1)
      return true;  // no more data is expected

    if (att.bufferForMsgSize.position() != att.bufferForMsgSize.limit())
      return false;  // not read completely

    att.bufferForMsgSize.flip();
    att.bufferForMsg = ByteBuffer.allocate(att.msgSize = att.bufferForMsgSize.getInt());
    att.bufferForMsgSize.clear();
    return false;
  }

  /**
   * returns true on end-of-stream
   */
  private boolean readArrayAndMakeNewTask(SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    Attachment att = (Attachment) key.attachment();

    if (channel.read(att.bufferForMsg) == -1)
      return true;  // no more data is expected

    if (att.bufferForMsg.position() != att.bufferForMsg.limit())
      return false;  // not read completely

    att.bufferForMsg.flip();
    byte[] buffer = new byte[att.msgSize];
    att.bufferForMsg.get(buffer);

    Timer timer = att.durationClient.newTimer().trackRequest();
    taskExecutor.execute(processMsg(buffer, channel, timer));

    // clean for the next message from the user
    att.bufferForMsg.clear();
    att.msgSize = -1;
    return false;
  }

  private Runnable processMsg(byte[] buffer, SocketChannel channel, Timer timer) {
    return () -> {
      try {
        timer.trackSorting();
        IntArrayOuterClass.ArrayMsg result = SortingUtil.sort(parseFrom(buffer));
        timer.breakSorting();

        responseProcessing.add(channel, result, timer);
      } catch (InvalidProtocolBufferException e) {
        facedIOException = true;
        e.printStackTrace();
      }
    };
  }

  public void addChannel(SocketChannel channel) throws IOException {
    channel.configureBlocking(false);
    queue.add(new QueElement(channel));
    selector.wakeup();
  }

  public static class Attachment {

    Duration.Client durationClient;
    ByteBuffer bufferForMsgSize = ByteBuffer.allocate(Integer.BYTES);
    ByteBuffer bufferForMsg = ByteBuffer.allocate(0);
    int msgSize = -1;

    public Attachment(Duration.Client durationClient) {
      this.durationClient = durationClient;
    }

  }

}
