package org.msgpack.core.buffer;

import java.nio.ByteBuffer;
import org.msgpack.core.Preconditions;

public class MessageBufferU extends MessageBuffer {
   private final ByteBuffer wrap;

   MessageBufferU(byte[] arr, int offset, int length) {
      super(arr, offset, length);
      this.wrap = ByteBuffer.wrap(arr, offset, length).slice();
   }

   MessageBufferU(ByteBuffer bb) {
      super(bb);
      this.wrap = bb.slice();
   }

   private MessageBufferU(Object base, long address, int length, ByteBuffer wrap) {
      super(base, address, length);
      this.wrap = wrap;
   }

   public MessageBufferU slice(int offset, int length) {
      if (offset == 0 && length == this.size()) {
         return this;
      } else {
         Preconditions.checkArgument(offset + length <= this.size());

         MessageBufferU var3;
         try {
            this.wrap.position(offset);
            this.wrap.limit(offset + length);
            var3 = new MessageBufferU(this.base, this.address + (long)offset, length, this.wrap.slice());
         } finally {
            this.resetBufferPosition();
         }

         return var3;
      }
   }

   private void resetBufferPosition() {
      this.wrap.position(0);
      this.wrap.limit(this.size);
   }

   @Override
   public byte getByte(int index) {
      return this.wrap.get(index);
   }

   @Override
   public boolean getBoolean(int index) {
      return this.wrap.get(index) != 0;
   }

   @Override
   public short getShort(int index) {
      return this.wrap.getShort(index);
   }

   @Override
   public int getInt(int index) {
      return this.wrap.getInt(index);
   }

   @Override
   public float getFloat(int index) {
      return this.wrap.getFloat(index);
   }

   @Override
   public long getLong(int index) {
      return this.wrap.getLong(index);
   }

   @Override
   public double getDouble(int index) {
      return this.wrap.getDouble(index);
   }

   @Override
   public void getBytes(int index, int len, ByteBuffer dst) {
      try {
         this.wrap.position(index);
         this.wrap.limit(index + len);
         dst.put(this.wrap);
      } finally {
         this.resetBufferPosition();
      }
   }

   @Override
   public void putByte(int index, byte v) {
      this.wrap.put(index, v);
   }

   @Override
   public void putBoolean(int index, boolean v) {
      this.wrap.put(index, (byte)(v ? 1 : 0));
   }

   @Override
   public void putShort(int index, short v) {
      this.wrap.putShort(index, v);
   }

   @Override
   public void putInt(int index, int v) {
      this.wrap.putInt(index, v);
   }

   @Override
   public void putFloat(int index, float v) {
      this.wrap.putFloat(index, v);
   }

   @Override
   public void putLong(int index, long l) {
      this.wrap.putLong(index, l);
   }

   @Override
   public void putDouble(int index, double v) {
      this.wrap.putDouble(index, v);
   }

   @Override
   public ByteBuffer sliceAsByteBuffer(int index, int length) {
      ByteBuffer var3;
      try {
         this.wrap.position(index);
         this.wrap.limit(index + length);
         var3 = this.wrap.slice();
      } finally {
         this.resetBufferPosition();
      }

      return var3;
   }

   @Override
   public ByteBuffer sliceAsByteBuffer() {
      return this.sliceAsByteBuffer(0, this.size);
   }

   @Override
   public void getBytes(int index, byte[] dst, int dstOffset, int length) {
      try {
         this.wrap.position(index);
         this.wrap.get(dst, dstOffset, length);
      } finally {
         this.resetBufferPosition();
      }
   }

   @Override
   public void putByteBuffer(int index, ByteBuffer src, int len) {
      assert len <= src.remaining();

      if (src.hasArray()) {
         this.putBytes(index, src.array(), src.position() + src.arrayOffset(), len);
         src.position(src.position() + len);
      } else {
         int prevSrcLimit = src.limit();

         try {
            src.limit(src.position() + len);
            this.wrap.position(index);
            this.wrap.put(src);
         } finally {
            src.limit(prevSrcLimit);
         }
      }
   }

   @Override
   public void putBytes(int index, byte[] src, int srcOffset, int length) {
      try {
         this.wrap.position(index);
         this.wrap.put(src, srcOffset, length);
      } finally {
         this.resetBufferPosition();
      }
   }

   @Override
   public void copyTo(int index, MessageBuffer dst, int offset, int length) {
      try {
         this.wrap.position(index);
         dst.putByteBuffer(offset, this.wrap, length);
      } finally {
         this.resetBufferPosition();
      }
   }

   @Override
   public void putMessageBuffer(int index, MessageBuffer src, int srcOffset, int len) {
      this.putByteBuffer(index, src.sliceAsByteBuffer(srcOffset, len), len);
   }

   @Override
   public byte[] toByteArray() {
      byte[] b = new byte[this.size()];
      this.getBytes(0, b, 0, b.length);
      return b;
   }

   @Override
   public boolean hasArray() {
      return !this.wrap.isDirect();
   }

   @Override
   public byte[] array() {
      return this.hasArray() ? this.wrap.array() : null;
   }
}
