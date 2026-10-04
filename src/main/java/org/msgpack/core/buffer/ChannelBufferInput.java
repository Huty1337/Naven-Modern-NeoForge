package org.msgpack.core.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import org.msgpack.core.Preconditions;

public class ChannelBufferInput implements MessageBufferInput {
   private ReadableByteChannel channel;
   private final MessageBuffer buffer;

   public ChannelBufferInput(ReadableByteChannel channel) {
      this(channel, 8192);
   }

   public ChannelBufferInput(ReadableByteChannel channel, int bufferSize) {
      this.channel = Preconditions.checkNotNull(channel, "input channel is null");
      Preconditions.checkArgument(bufferSize > 0, "buffer size must be > 0: " + bufferSize);
      this.buffer = MessageBuffer.allocate(bufferSize);
   }

   public ReadableByteChannel reset(ReadableByteChannel channel) throws IOException {
      ReadableByteChannel old = this.channel;
      this.channel = channel;
      return old;
   }

   @Override
   public MessageBuffer next() throws IOException {
      ByteBuffer b = this.buffer.sliceAsByteBuffer();
      int ret = this.channel.read(b);
      if (ret == -1) {
         return null;
      } else {
         b.flip();
         return this.buffer.slice(0, b.limit());
      }
   }

   @Override
   public void close() throws IOException {
      this.channel.close();
   }
}
