package org.msgpack.core.buffer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.msgpack.core.Preconditions;
import sun.misc.Unsafe;

public class MessageBuffer {
   static final boolean isUniversalBuffer;
   static final Unsafe unsafe;
   static final int javaVersion = getJavaVersion();
   private static final Constructor<?> mbArrConstructor;
   private static final Constructor<?> mbBBConstructor;
   static final int ARRAY_BYTE_BASE_OFFSET;
   private static final String UNIVERSAL_MESSAGE_BUFFER = "org.msgpack.core.buffer.MessageBufferU";
   private static final String BIGENDIAN_MESSAGE_BUFFER = "org.msgpack.core.buffer.MessageBufferBE";
   private static final String DEFAULT_MESSAGE_BUFFER = "org.msgpack.core.buffer.MessageBuffer";
   protected final Object base;
   protected final long address;
   protected final int size;
   protected final ByteBuffer reference;

   private static int getJavaVersion() {
      String javaVersion = System.getProperty("java.specification.version", "");
      int dotPos = javaVersion.indexOf(46);
      if (dotPos != -1) {
         try {
            int major = Integer.parseInt(javaVersion.substring(0, dotPos));
            int minor = Integer.parseInt(javaVersion.substring(dotPos + 1));
            return major > 1 ? major : minor;
         } catch (NumberFormatException var4) {
            var4.printStackTrace(System.err);
         }
      } else {
         try {
            return Integer.parseInt(javaVersion);
         } catch (NumberFormatException var5) {
            var5.printStackTrace(System.err);
         }
      }

      return 6;
   }

   public static MessageBuffer allocate(int size) {
      if (size < 0) {
         throw new IllegalArgumentException("size must not be negative");
      } else {
         return wrap(new byte[size]);
      }
   }

   public static MessageBuffer wrap(byte[] array) {
      return newMessageBuffer(array, 0, array.length);
   }

   public static MessageBuffer wrap(byte[] array, int offset, int length) {
      return newMessageBuffer(array, offset, length);
   }

   public static MessageBuffer wrap(ByteBuffer bb) {
      return newMessageBuffer(bb);
   }

   private static MessageBuffer newMessageBuffer(byte[] arr, int off, int len) {
      Preconditions.checkNotNull(arr);
      return mbArrConstructor != null ? newInstance(mbArrConstructor, arr, off, len) : new MessageBuffer(arr, off, len);
   }

   private static MessageBuffer newMessageBuffer(ByteBuffer bb) {
      Preconditions.checkNotNull(bb);
      return mbBBConstructor != null ? newInstance(mbBBConstructor, bb) : new MessageBuffer(bb);
   }

   private static MessageBuffer newInstance(Constructor<?> constructor, Object... args) {
      try {
         return (MessageBuffer)constructor.newInstance(args);
      } catch (InstantiationException var3) {
         throw new IllegalStateException(var3);
      } catch (IllegalAccessException var4) {
         throw new IllegalStateException(var4);
      } catch (InvocationTargetException var5) {
         if (var5.getCause() instanceof RuntimeException) {
            throw (RuntimeException)var5.getCause();
         } else if (var5.getCause() instanceof Error) {
            throw (Error)var5.getCause();
         } else {
            throw new IllegalStateException(var5.getCause());
         }
      }
   }

   public static void releaseBuffer(MessageBuffer buffer) {
      if (!isUniversalBuffer && !buffer.hasArray()) {
         if (DirectBufferAccess.isDirectByteBufferInstance(buffer.reference)) {
            DirectBufferAccess.clean(buffer.reference);
         } else {
            unsafe.freeMemory(buffer.address);
         }
      }
   }

   MessageBuffer(byte[] arr, int offset, int length) {
      this.base = arr;
      this.address = (long)(ARRAY_BYTE_BASE_OFFSET + offset);
      this.size = length;
      this.reference = null;
   }

   MessageBuffer(ByteBuffer bb) {
      if (bb.hasArray()) {
         this.base = bb.array();
         this.address = (long)(ARRAY_BYTE_BASE_OFFSET + bb.arrayOffset() + bb.position());
         this.size = bb.remaining();
         this.reference = null;
      } else {
         throw new IllegalArgumentException("Only the array-backed ByteBuffer or DirectBuffer is supported");
      }
   }

   protected MessageBuffer(Object base, long address, int length) {
      this.base = base;
      this.address = address;
      this.size = length;
      this.reference = null;
   }

   public int size() {
      return this.size;
   }

   public MessageBuffer slice(int offset, int length) {
      if (offset == 0 && length == this.size()) {
         return this;
      } else {
         Preconditions.checkArgument(offset + length <= this.size());
         return new MessageBuffer(this.base, this.address + (long)offset, length);
      }
   }

   public byte getByte(int index) {
      return unsafe.getByte(this.base, this.address + (long)index);
   }

   public boolean getBoolean(int index) {
      return unsafe.getBoolean(this.base, this.address + (long)index);
   }

   public short getShort(int index) {
      short v = unsafe.getShort(this.base, this.address + (long)index);
      return Short.reverseBytes(v);
   }

   public int getInt(int index) {
      int i = unsafe.getInt(this.base, this.address + (long)index);
      return Integer.reverseBytes(i);
   }

   public float getFloat(int index) {
      return Float.intBitsToFloat(this.getInt(index));
   }

   public long getLong(int index) {
      long l = unsafe.getLong(this.base, this.address + (long)index);
      return Long.reverseBytes(l);
   }

   public double getDouble(int index) {
      return Double.longBitsToDouble(this.getLong(index));
   }

   public void getBytes(int index, byte[] dst, int dstOffset, int length) {
      unsafe.copyMemory(this.base, this.address + (long)index, dst, (long)(ARRAY_BYTE_BASE_OFFSET + dstOffset), (long)length);
   }

   public void getBytes(int index, int len, ByteBuffer dst) {
      if (dst.remaining() < len) {
         throw new BufferOverflowException();
      } else {
         ByteBuffer src = this.sliceAsByteBuffer(index, len);
         dst.put(src);
      }
   }

   public void putByte(int index, byte v) {
      unsafe.putByte(this.base, this.address + (long)index, v);
   }

   public void putBoolean(int index, boolean v) {
      unsafe.putBoolean(this.base, this.address + (long)index, v);
   }

   public void putShort(int index, short v) {
      v = Short.reverseBytes(v);
      unsafe.putShort(this.base, this.address + (long)index, v);
   }

   public void putInt(int index, int v) {
      v = Integer.reverseBytes(v);
      unsafe.putInt(this.base, this.address + (long)index, v);
   }

   public void putFloat(int index, float v) {
      this.putInt(index, Float.floatToRawIntBits(v));
   }

   public void putLong(int index, long l) {
      l = Long.reverseBytes(l);
      unsafe.putLong(this.base, this.address + (long)index, l);
   }

   public void putDouble(int index, double v) {
      this.putLong(index, Double.doubleToRawLongBits(v));
   }

   public void putBytes(int index, byte[] src, int srcOffset, int length) {
      unsafe.copyMemory(src, (long)(ARRAY_BYTE_BASE_OFFSET + srcOffset), this.base, this.address + (long)index, (long)length);
   }

   public void putByteBuffer(int index, ByteBuffer src, int len) {
      assert len <= src.remaining();

      assert !isUniversalBuffer;

      if (src.hasArray()) {
         byte[] srcArray = src.array();
         unsafe.copyMemory(srcArray, (long)(ARRAY_BYTE_BASE_OFFSET + src.position()), this.base, this.address + (long)index, (long)len);
         src.position(src.position() + len);
      } else if (this.hasArray()) {
         src.get((byte[])this.base, index, len);
      } else {
         for (int i = 0; i < len; i++) {
            unsafe.putByte(this.base, this.address + (long)index, src.get());
         }
      }
   }

   public void putMessageBuffer(int index, MessageBuffer src, int srcOffset, int len) {
      unsafe.copyMemory(src.base, src.address + (long)srcOffset, this.base, this.address + (long)index, (long)len);
   }

   public ByteBuffer sliceAsByteBuffer(int index, int length) {
      if (this.hasArray()) {
         return ByteBuffer.wrap((byte[])this.base, (int)(this.address - (long)ARRAY_BYTE_BASE_OFFSET + (long)index), length);
      } else {
         assert !isUniversalBuffer;

         return DirectBufferAccess.newByteBuffer(this.address, index, length, this.reference);
      }
   }

   public ByteBuffer sliceAsByteBuffer() {
      return this.sliceAsByteBuffer(0, this.size());
   }

   public boolean hasArray() {
      return this.base != null;
   }

   public byte[] toByteArray() {
      byte[] b = new byte[this.size()];
      unsafe.copyMemory(this.base, this.address, b, (long)ARRAY_BYTE_BASE_OFFSET, (long)this.size());
      return b;
   }

   public byte[] array() {
      return (byte[])this.base;
   }

   public int arrayOffset() {
      return (int)this.address - ARRAY_BYTE_BASE_OFFSET;
   }

   public void copyTo(int index, MessageBuffer dst, int offset, int length) {
      unsafe.copyMemory(this.base, this.address + (long)index, dst.base, dst.address + (long)offset, (long)length);
   }

   public String toHexString(int offset, int length) {
      StringBuilder s = new StringBuilder();

      for (int i = offset; i < length; i++) {
         if (i != offset) {
            s.append(" ");
         }

         s.append(String.format("%02x", this.getByte(i)));
      }

      return s.toString();
   }

   static {
      boolean useUniversalBuffer = false;
      Unsafe unsafeInstance = null;
      int arrayByteBaseOffset = 16;

      try {
         boolean hasUnsafe = false;

         try {
            hasUnsafe = Class.forName("sun.misc.Unsafe") != null;
         } catch (Exception var20) {
         }

         boolean isAndroid = System.getProperty("java.runtime.name", "").toLowerCase().contains("android");
         boolean isGAE = System.getProperty("com.google.appengine.runtime.version") != null;
         useUniversalBuffer = Boolean.parseBoolean(System.getProperty("msgpack.universal-buffer", "false"))
            || isAndroid
            || isGAE
            || javaVersion < 7
            || !hasUnsafe;
         if (!useUniversalBuffer) {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafeInstance = (Unsafe)field.get(null);
            if (unsafeInstance == null) {
               throw new RuntimeException("Unsafe is unavailable");
            }

            arrayByteBaseOffset = unsafeInstance.arrayBaseOffset(byte[].class);
            int arrayByteIndexScale = unsafeInstance.arrayIndexScale(byte[].class);
            if (arrayByteIndexScale != 1) {
               throw new IllegalStateException("Byte array index scale must be 1, but is " + arrayByteIndexScale);
            }
         }
      } catch (Exception var21) {
         var21.printStackTrace(System.err);
         useUniversalBuffer = true;
      } finally {
         unsafe = unsafeInstance;
         ARRAY_BYTE_BASE_OFFSET = arrayByteBaseOffset;
         isUniversalBuffer = useUniversalBuffer;
         String bufferClsName;
         if (isUniversalBuffer) {
            bufferClsName = "org.msgpack.core.buffer.MessageBufferU";
         } else {
            boolean isLittleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
            bufferClsName = isLittleEndian ? "org.msgpack.core.buffer.MessageBuffer" : "org.msgpack.core.buffer.MessageBufferBE";
         }

         if ("org.msgpack.core.buffer.MessageBuffer".equals(bufferClsName)) {
            mbArrConstructor = null;
            mbBBConstructor = null;
         } else {
            try {
               Class<?> bufferCls = Class.forName(bufferClsName);
               Constructor<?> mbArrCstr = bufferCls.getDeclaredConstructor(byte[].class, int.class, int.class);
               mbArrCstr.setAccessible(true);
               mbArrConstructor = mbArrCstr;
               Constructor<?> mbBBCstr = bufferCls.getDeclaredConstructor(ByteBuffer.class);
               mbBBCstr.setAccessible(true);
               mbBBConstructor = mbBBCstr;
            } catch (Exception var19) {
               var19.printStackTrace(System.err);
               throw new RuntimeException(var19);
            }
         }
      }
   }
}
