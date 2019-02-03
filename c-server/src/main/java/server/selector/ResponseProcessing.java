package server.selector;

import common.IntArrayOuterClass.ArrayMsg;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static java.nio.channels.SelectionKey.OP_WRITE;

public class ResponseProcessing extends SelectorProcessing {

  public static class Attachment {

    ByteBuffer bufferForMsgSize = ByteBuffer.allocate(Integer.BYTES);
    ByteBuffer bufferForMsg;
    ByteBuffer[] buffers;

    Attachment(ArrayMsg msg) {
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


  @Override protected void register(Selector selector, QueElement elem) throws ClosedChannelException {
    elem.channel.register(selector, OP_WRITE, new Attachment(elem.msg));
  }

  @Override protected void processKey(SelectionKey key) {
    SocketChannel channel = (SocketChannel) key.channel();
    Attachment att = (Attachment) key.attachment();

    try {
      channel.write(att.buffers);

      if (att.isCompletelyRead()) key.cancel();
    } catch (IOException ex) {
      facedIOException = true;
      key.cancel();
    }
  }

  public void add(SocketChannel channel, ArrayMsg result) {
    // channel is already set non-blocking
    queue.add(new QueElement(channel, result));
    selector.wakeup();
  }

}
