package server.selector;

import common.Duration;
import common.IntArrayOuterClass.ArrayMsg;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static java.nio.channels.SelectionKey.OP_WRITE;

public class ResponseProcessing extends SelectorProcessing {

  @Override protected void register(Selector selector, QueElement elem) throws ClosedChannelException {
    elem.channel.register(selector, OP_WRITE, new Attachment(elem.msg, elem.timer));
  }

  @Override protected void processKey(SelectionKey key) {
    SocketChannel channel = (SocketChannel) key.channel();
    Attachment att = (Attachment) key.attachment();

    try {
      channel.write(att.buffers);

      if (att.isCompletelyRead()) {
        att.timer.breakRequest();
        key.cancel();
      }
    } catch (IOException ex) {
      facedIOException = true;
      key.cancel();
    }
  }

  public void add(SocketChannel channel, ArrayMsg result, Duration.Timer timer) {
    // channel is already set non-blocking
    queue.add(new QueElement(channel, result, timer));
    selector.wakeup();
  }

  public static class Attachment {

    Duration.Client durationClient;
    ByteBuffer bufferForMsgSize = ByteBuffer.allocate(Integer.BYTES);
    ByteBuffer bufferForMsg;
    ByteBuffer[] buffers;
    private Duration.Timer timer;

    Attachment(ArrayMsg msg, Duration.Timer timer) {
      this.timer = timer;
      bufferForMsgSize.putInt(msg.getSerializedSize());
      bufferForMsgSize.flip();
      bufferForMsg = ByteBuffer.wrap(msg.toByteArray());
      buffers = new ByteBuffer[]{bufferForMsgSize, bufferForMsg};
    }

    boolean isCompletelyRead() {
      for (ByteBuffer buffer : buffers)
        if (buffer.position() != buffer.limit())
          return false;
      return true;
    }

  }

}
