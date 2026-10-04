package org.msgpack.core.buffer;

import java.nio.ByteBuffer;
import org.msgpack.core.Preconditions;

public class MessageBufferBE extends MessageBuffer {
   MessageBufferBE(byte[] arr, int offset, int length) {
      super(arr, offset, length);
   }

   MessageBufferBE(ByteBuffer bb) {
      super(bb);
   }

   private MessageBufferBE(Object base, long address, int length) {
      super(base, address, length);
   }

   public MessageBufferBE slice(int offset, int length) {
      if (offset == 0 && length == this.size()) {
         return this;
      } else {
         Preconditions.checkArgument(offset + length <= this.size());
         return new MessageBufferBE(this.base, this.address + (long)offset, length);
      }
   }

   @Override
   public short getShort(int index) {
      return unsafe.getShort(this.base, this.address + (long)index);
   }

   @Override
   public int getInt(int index) {
      return unsafe.getInt(this.base, this.address + (long)index);
   }

   @Override
   public long getLong(int index) {
      return unsafe.getLong(this.base, this.address + (long)index);
   }

   @Override
   public float getFloat(int index) {
      return unsafe.getFloat(this.base, this.address + (long)index);
   }

   @Override
   public double getDouble(int index) {
      return unsafe.getDouble(this.base, this.address + (long)index);
   }

   @Override
   public void putShort(int index, short v) {
      unsafe.putShort(this.base, this.address + (long)index, v);
   }

   @Override
   public void putInt(int index, int v) {
      unsafe.putInt(this.base, this.address + (long)index, v);
   }

   @Override
   public void putLong(int index, long v) {
      unsafe.putLong(this.base, this.address + (long)index, v);
   }

   @Override
   public void putDouble(int index, double v) {
      unsafe.putDouble(this.base, this.address + (long)index, v);
   }
}
