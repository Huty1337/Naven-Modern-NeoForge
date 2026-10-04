package org.msgpack.core.buffer;

import java.io.IOException;
import java.io.OutputStream;
import org.msgpack.core.Preconditions;

public class OutputStreamBufferOutput implements MessageBufferOutput {
   private OutputStream out;
   private MessageBuffer buffer;

   public OutputStreamBufferOutput(OutputStream out) {
      this(out, 8192);
   }

   public OutputStreamBufferOutput(OutputStream out, int bufferSize) {
      this.out = Preconditions.checkNotNull(out, "output is null");
      this.buffer = MessageBuffer.allocate(bufferSize);
   }

   public OutputStream reset(OutputStream out) throws IOException {
      OutputStream old = this.out;
      this.out = out;
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
      this.write(this.buffer.array(), this.buffer.arrayOffset(), length);
   }

   @Override
   public void write(byte[] buffer, int offset, int length) throws IOException {
      this.out.write(buffer, offset, length);
   }

   @Override
   public void add(byte[] buffer, int offset, int length) throws IOException {
      this.write(buffer, offset, length);
   }

   @Override
   public void close() throws IOException {
      this.out.close();
   }

   @Override
   public void flush() throws IOException {
      this.out.flush();
   }
}
