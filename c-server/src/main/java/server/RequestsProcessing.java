package server;

import com.google.protobuf.InvalidProtocolBufferException;
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

  public static class Attachment {

    ByteBuffer bufferForMsgSize = ByteBuffer.allocate(Integer.BYTES);
    ByteBuffer bufferForMsg = ByteBuffer.allocate(0);
    int msgSize = -1;

  }


  private final ExecutorService taskExecutor;
  private final ResponseProcessing responseProcessing;


  public RequestsProcessing(ExecutorService taskExecutor, ResponseProcessing responseProcessing) {
    this.taskExecutor = taskExecutor;
    this.responseProcessing = responseProcessing;
  }


  @Override protected void register(Selector selector, QueElement elem) throws ClosedChannelException {
    elem.channel.register(selector, OP_READ, new Attachment());
  }

  protected void processKey(SelectionKey key) {
    int arraySize = ((Attachment) key.attachment()).msgSize;

    try {
      // read size, read array
      boolean isEndOfStream = (arraySize < 0 && readMsgSizeAndAllocateBuffer(key)) || readArrayAndMakeNewTask(key);
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
    taskExecutor.execute(processMsg(buffer, channel));

    // clean for the next message from the user
    att.bufferForMsg.clear();
    att.msgSize = -1;
    return false;
  }

  private Runnable processMsg(byte[] buffer, SocketChannel channel) {
    return () -> {
      try {
        IntArrayOuterClass.ArrayMsg result = SortingUtil.sort(parseFrom(buffer));
        responseProcessing.add(channel, result);
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

}