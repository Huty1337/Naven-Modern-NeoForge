package org.msgpack.core.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import org.msgpack.core.Preconditions;

public class ChannelBufferOutput implements MessageBufferOutput {
   private WritableByteChannel channel;
   private MessageBuffer buffer;

   public ChannelBufferOutput(WritableByteChannel channel) {
      this(channel, 8192);
   }

   public ChannelBufferOutput(WritableByteChannel channel, int bufferSize) {
      this.channel = Preconditions.checkNotNull(channel, "output channel is null");
      this.buffer = MessageBuffer.allocate(bufferSize);
   }

   public WritableByteChannel reset(WritableByteChannel channel) throws IOException {
      WritableByteChannel old = this.channel;
      this.channel = channel;
      return old;
   }

   @Override
   public MessageBuffer next(int minimumSize) throws IOException {
      if (this.buffer.size() < minimumSize) {
         this.buffer = MessageBuffer.allocate(minimumSize);
      }

      return this.buffer;
   }

   @Override
   public void writeBuffer(int length) throws IOException {
      ByteBuffer bb = this.buffer.sliceAsByteBuffer(0, length);

      while (bb.hasRemaining()) {
         this.channel.write(bb);
      }
   }

   @Override
   public void write(byte[] buffer, int offset, int length) throws IOException {
      ByteBuffer bb = ByteBuffer.wrap(buffer, offset, length);

      while (bb.hasRemaining()) {
         this.channel.write(bb);
      }
   }

   @Override
   public void add(byte[] buffer, int offset, int length) throws IOException {
      this.write(buffer, offset, length);
   }

   @Override
   public void close() throws IOException {
      this.channel.close();
   }

   @Override
   public void flush() throws IOException {
   }
}
