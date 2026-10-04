package org.msgpack.core.buffer;

import org.msgpack.core.Preconditions;

public class ArrayBufferInput implements MessageBufferInput {
   private MessageBuffer buffer;
   private boolean isEmpty;

   public ArrayBufferInput(MessageBuffer buf) {
      this.buffer = buf;
      if (buf == null) {
         this.isEmpty = true;
      } else {
         this.isEmpty = false;
      }
   }

   public ArrayBufferInput(byte[] arr) {
      this(arr, 0, arr.length);
   }

   public ArrayBufferInput(byte[] arr, int offset, int length) {
      this(MessageBuffer.wrap(Preconditions.checkNotNull(arr, "input array is null"), offset, length));
   }

   public MessageBuffer reset(MessageBuffer buf) {
      MessageBuffer old = this.buffer;
      this.buffer = buf;
      if (buf == null) {
         this.isEmpty = true;
      } else {
         this.isEmpty = false;
      }

      return old;
   }

   public void reset(byte[] arr) {
      this.reset(MessageBuffer.wrap(Preconditions.checkNotNull(arr, "input array is null")));
   }

   public void reset(byte[] arr, int offset, int len) {
      this.reset(MessageBuffer.wrap(Preconditions.checkNotNull(arr, "input array is null"), offset, len));
   }

   @Override
   public MessageBuffer next() {
      if (this.isEmpty) {
         return null;
      } else {
         this.isEmpty = true;
         return this.buffer;
      }
   }

   @Override
   public void close() {
      this.buffer = null;
      this.isEmpty = true;
   }
}
