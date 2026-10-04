package org.msgpack.core.buffer;

import java.nio.ByteBuffer;
import org.msgpack.core.Preconditions;

public class ByteBufferInput implements MessageBufferInput {
   private ByteBuffer input;
   private boolean isRead = false;

   public ByteBufferInput(ByteBuffer input) {
      this.input = Preconditions.checkNotNull(input, "input ByteBuffer is null").slice();
   }

   public ByteBuffer reset(ByteBuffer input) {
      ByteBuffer old = this.input;
      this.input = Preconditions.checkNotNull(input, "input ByteBuffer is null").slice();
      this.isRead = false;
      return old;
   }

   @Override
   public MessageBuffer next() {
      if (this.isRead) {
         return null;
      } else {
         MessageBuffer b = MessageBuffer.wrap(this.input);
         this.isRead = true;
         return b;
      }
   }

   @Override
   public void close() {
   }
}
