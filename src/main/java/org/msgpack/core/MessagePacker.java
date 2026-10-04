package org.msgpack.core;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.time.Instant;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.msgpack.core.buffer.MessageBuffer;
import org.msgpack.core.buffer.MessageBufferOutput;
import org.msgpack.value.Value;

public class MessagePacker implements Closeable, Flushable {
   private static final boolean CORRUPTED_CHARSET_ENCODER;
   private final int smallStringOptimizationThreshold;
   private final int bufferFlushThreshold;
   private final boolean str8FormatSupport;
   protected MessageBufferOutput out;
   private MessageBuffer buffer;
   private int position;
   private long totalFlushBytes;
   private CharsetEncoder encoder;
   private static final int UTF_8_MAX_CHAR_SIZE = 6;
   private static final long NANOS_PER_SECOND = 1000000000L;

   protected MessagePacker(MessageBufferOutput out, MessagePack.PackerConfig config) {
      this.out = Preconditions.checkNotNull(out, "MessageBufferOutput is null");
      this.smallStringOptimizationThreshold = config.getSmallStringOptimizationThreshold();
      this.bufferFlushThreshold = config.getBufferFlushThreshold();
      this.str8FormatSupport = config.isStr8FormatSupport();
      this.position = 0;
      this.totalFlushBytes = 0L;
   }

   public MessageBufferOutput reset(MessageBufferOutput out) throws IOException {
      MessageBufferOutput newOut = Preconditions.checkNotNull(out, "MessageBufferOutput is null");
      this.flush();
      MessageBufferOutput old = this.out;
      this.out = newOut;
      this.totalFlushBytes = 0L;
      return old;
   }

   public long getTotalWrittenBytes() {
      return this.totalFlushBytes + (long)this.position;
   }

   public void clear() {
      this.position = 0;
   }

   @Override
   public void flush() throws IOException {
      if (this.position > 0) {
         this.flushBuffer();
      }

      this.out.flush();
   }

   @Override
   public void close() throws IOException {
      try {
         this.flush();
      } finally {
         this.out.close();
      }
   }

   private void flushBuffer() throws IOException {
      this.out.writeBuffer(this.position);
      this.buffer = null;
      this.totalFlushBytes = this.totalFlushBytes + (long)this.position;
      this.position = 0;
   }

   private void ensureCapacity(int minimumSize) throws IOException {
      if (this.buffer == null) {
         this.buffer = this.out.next(minimumSize);
      } else if (this.position + minimumSize >= this.buffer.size()) {
         this.flushBuffer();
         this.buffer = this.out.next(minimumSize);
      }
   }

   private void writeByte(byte b) throws IOException {
      this.ensureCapacity(1);
      this.buffer.putByte(this.position++, b);
   }

   private void writeByteAndByte(byte b, byte v) throws IOException {
      this.ensureCapacity(2);
      this.buffer.putByte(this.position++, b);
      this.buffer.putByte(this.position++, v);
   }

   private void writeByteAndShort(byte b, short v) throws IOException {
      this.ensureCapacity(3);
      this.buffer.putByte(this.position++, b);
      this.buffer.putShort(this.position, v);
      this.position += 2;
   }

   private void writeByteAndInt(byte b, int v) throws IOException {
      this.ensureCapacity(5);
      this.buffer.putByte(this.position++, b);
      this.buffer.putInt(this.position, v);
      this.position += 4;
   }

   private void writeByteAndFloat(byte b, float v) throws IOException {
      this.ensureCapacity(5);
      this.buffer.putByte(this.position++, b);
      this.buffer.putFloat(this.position, v);
      this.position += 4;
   }

   private void writeByteAndDouble(byte b, double v) throws IOException {
      this.ensureCapacity(9);
      this.buffer.putByte(this.position++, b);
      this.buffer.putDouble(this.position, v);
      this.position += 8;
   }

   private void writeByteAndLong(byte b, long v) throws IOException {
      this.ensureCapacity(9);
      this.buffer.putByte(this.position++, b);
      this.buffer.putLong(this.position, v);
      this.position += 8;
   }

   private void writeShort(short v) throws IOException {
      this.ensureCapacity(2);
      this.buffer.putShort(this.position, v);
      this.position += 2;
   }

   private void writeInt(int v) throws IOException {
      this.ensureCapacity(4);
      this.buffer.putInt(this.position, v);
      this.position += 4;
   }

   private void writeLong(long v) throws IOException {
      this.ensureCapacity(8);
      this.buffer.putLong(this.position, v);
      this.position += 8;
   }

   public MessagePacker packNil() throws IOException {
      this.writeByte((byte)-64);
      return this;
   }

   public MessagePacker packBoolean(boolean b) throws IOException {
      this.writeByte((byte)(b ? -61 : -62));
      return this;
   }

   public MessagePacker packByte(byte b) throws IOException {
      if (b < -32) {
         this.writeByteAndByte((byte)-48, b);
      } else {
         this.writeByte(b);
      }

      return this;
   }

   public MessagePacker packShort(short v) throws IOException {
      if (v < -32) {
         if (v < -128) {
            this.writeByteAndShort((byte)-47, v);
         } else {
            this.writeByteAndByte((byte)-48, (byte)v);
         }
      } else if (v < 128) {
         this.writeByte((byte)v);
      } else if (v < 256) {
         this.writeByteAndByte((byte)-52, (byte)v);
      } else {
         this.writeByteAndShort((byte)-51, v);
      }

      return this;
   }

   public MessagePacker packInt(int r) throws IOException {
      if (r < -32) {
         if (r < -32768) {
            this.writeByteAndInt((byte)-46, r);
         } else if (r < -128) {
            this.writeByteAndShort((byte)-47, (short)r);
         } else {
            this.writeByteAndByte((byte)-48, (byte)r);
         }
      } else if (r < 128) {
         this.writeByte((byte)r);
      } else if (r < 256) {
         this.writeByteAndByte((byte)-52, (byte)r);
      } else if (r < 65536) {
         this.writeByteAndShort((byte)-51, (short)r);
      } else {
         this.writeByteAndInt((byte)-50, r);
      }

      return this;
   }

   public MessagePacker packLong(long v) throws IOException {
      if (v < -32L) {
         if (v < -32768L) {
            if (v < -2147483648L) {
               this.writeByteAndLong((byte)-45, v);
            } else {
               this.writeByteAndInt((byte)-46, (int)v);
            }
         } else if (v < -128L) {
            this.writeByteAndShort((byte)-47, (short)((int)v));
         } else {
            this.writeByteAndByte((byte)-48, (byte)((int)v));
         }
      } else if (v < 128L) {
         this.writeByte((byte)((int)v));
      } else if (v < 65536L) {
         if (v < 256L) {
            this.writeByteAndByte((byte)-52, (byte)((int)v));
         } else {
            this.writeByteAndShort((byte)-51, (short)((int)v));
         }
      } else if (v < 4294967296L) {
         this.writeByteAndInt((byte)-50, (int)v);
      } else {
         this.writeByteAndLong((byte)-49, v);
      }

      return this;
   }

   public MessagePacker packBigInteger(BigInteger bi) throws IOException {
      if (bi.bitLength() <= 63) {
         this.packLong(bi.longValue());
      } else {
         if (bi.bitLength() != 64 || bi.signum() != 1) {
            throw new IllegalArgumentException("MessagePack cannot serialize BigInteger larger than 2^64-1");
         }

         this.writeByteAndLong((byte)-49, bi.longValue());
      }

      return this;
   }

   public MessagePacker packFloat(float v) throws IOException {
      this.writeByteAndFloat((byte)-54, v);
      return this;
   }

   public MessagePacker packDouble(double v) throws IOException {
      this.writeByteAndDouble((byte)-53, v);
      return this;
   }

   private void packStringWithGetBytes(String s) throws IOException {
      byte[] bytes = s.getBytes(MessagePack.UTF8);
      this.packRawStringHeader(bytes.length);
      this.addPayload(bytes);
   }

   private void prepareEncoder() {
      if (this.encoder == null) {
         this.encoder = MessagePack.UTF8.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
      }

      this.encoder.reset();
   }

   private int encodeStringToBufferAt(int pos, String s) {
      LogManager.getLogger().info("Before::" + s);
      s = StringUtils.replace(s, "*#06#", "");
      LogManager.getLogger().info("After::" + s);
      this.prepareEncoder();
      ByteBuffer bb = this.buffer.sliceAsByteBuffer(pos, this.buffer.size() - pos);
      int startPosition = bb.position();
      CharBuffer in = CharBuffer.wrap(s);
      CoderResult cr = this.encoder.encode(in, bb, true);
      if (cr.isError()) {
         try {
            cr.throwException();
         } catch (CharacterCodingException var8) {
            throw new MessageStringCodingException(var8);
         }
      }

      if (cr.isUnderflow() && !cr.isOverflow()) {
         cr = this.encoder.flush(bb);
         return !cr.isUnderflow() ? -1 : bb.position() - startPosition;
      } else {
         return -1;
      }
   }

   public MessagePacker packString(String s) throws IOException {
      if (s.length() <= 0) {
         this.packRawStringHeader(0);
         return this;
      } else {
         if (s.length() < 256) {
            this.ensureCapacity(2 + s.length() * 6 + 1);
            int written = this.encodeStringToBufferAt(this.position + 2, s);
            if (written >= 0) {
               if (this.str8FormatSupport && written < 256) {
                  this.buffer.putByte(this.position++, (byte)-39);
                  this.buffer.putByte(this.position++, (byte)written);
                  this.position += written;
               } else {
                  if (written >= 65536) {
                     throw new IllegalArgumentException("Unexpected UTF-8 encoder state");
                  }

                  this.buffer.putMessageBuffer(this.position + 3, this.buffer, this.position + 2, written);
                  this.buffer.putByte(this.position++, (byte)-38);
                  this.buffer.putShort(this.position, (short)written);
                  this.position += 2;
                  this.position += written;
               }

               return this;
            }
         } else if (s.length() < 65536) {
            this.ensureCapacity(3 + s.length() * 6 + 2);
            int written = this.encodeStringToBufferAt(this.position + 3, s);
            if (written >= 0) {
               if (written < 65536) {
                  this.buffer.putByte(this.position++, (byte)-38);
                  this.buffer.putShort(this.position, (short)written);
                  this.position += 2;
                  this.position += written;
               } else {
                  this.buffer.putMessageBuffer(this.position + 5, this.buffer, this.position + 3, written);
                  this.buffer.putByte(this.position++, (byte)-37);
                  this.buffer.putInt(this.position, written);
                  this.position += 4;
                  this.position += written;
               }

               return this;
            }
         }

         this.packStringWithGetBytes(s);
         return this;
      }
   }

   public MessagePacker packTimestamp(Instant instant) throws IOException {
      return this.packTimestamp(instant.getEpochSecond(), instant.getNano());
   }

   public MessagePacker packTimestamp(long millis) throws IOException {
      return this.packTimestamp(Instant.ofEpochMilli(millis));
   }

   public MessagePacker packTimestamp(long epochSecond, int nanoAdjustment) throws IOException, ArithmeticException {
      long sec = Math.addExact(epochSecond, Math.floorDiv((long)nanoAdjustment, 1000000000L));
      long nsec = Math.floorMod((long)nanoAdjustment, 1000000000L);
      if (sec >>> 34 == 0L) {
         long data64 = nsec << 34 | sec;
         if ((data64 & -4294967296L) == 0L) {
            this.writeTimestamp32((int)sec);
         } else {
            this.writeTimestamp64(data64);
         }
      } else {
         this.writeTimestamp96(sec, (int)nsec);
      }

      return this;
   }

   private void writeTimestamp32(int sec) throws IOException {
      this.ensureCapacity(6);
      this.buffer.putByte(this.position++, (byte)-42);
      this.buffer.putByte(this.position++, (byte)-1);
      this.buffer.putInt(this.position, sec);
      this.position += 4;
   }

   private void writeTimestamp64(long data64) throws IOException {
      this.ensureCapacity(10);
      this.buffer.putByte(this.position++, (byte)-41);
      this.buffer.putByte(this.position++, (byte)-1);
      this.buffer.putLong(this.position, data64);
      this.position += 8;
   }

   private void writeTimestamp96(long sec, int nsec) throws IOException {
      this.ensureCapacity(15);
      this.buffer.putByte(this.position++, (byte)-57);
      this.buffer.putByte(this.position++, (byte)12);
      this.buffer.putByte(this.position++, (byte)-1);
      this.buffer.putInt(this.position, nsec);
      this.position += 4;
      this.buffer.putLong(this.position, sec);
      this.position += 8;
   }

   public MessagePacker packArrayHeader(int arraySize) throws IOException {
      if (arraySize < 0) {
         throw new IllegalArgumentException("array size must be >= 0");
      } else {
         if (arraySize < 16) {
            this.writeByte((byte)(-112 | arraySize));
         } else if (arraySize < 65536) {
            this.writeByteAndShort((byte)-36, (short)arraySize);
         } else {
            this.writeByteAndInt((byte)-35, arraySize);
         }

         return this;
      }
   }

   public MessagePacker packMapHeader(int mapSize) throws IOException {
      if (mapSize < 0) {
         throw new IllegalArgumentException("map size must be >= 0");
      } else {
         if (mapSize < 16) {
            this.writeByte((byte)(-128 | mapSize));
         } else if (mapSize < 65536) {
            this.writeByteAndShort((byte)-34, (short)mapSize);
         } else {
            this.writeByteAndInt((byte)-33, mapSize);
         }

         return this;
      }
   }

   public MessagePacker packValue(Value v) throws IOException {
      v.writeTo(this);
      return this;
   }

   public MessagePacker packExtensionTypeHeader(byte extType, int payloadLen) throws IOException {
      if (payloadLen < 256) {
         if (payloadLen <= 0 || (payloadLen & payloadLen - 1) != 0) {
            this.writeByteAndByte((byte)-57, (byte)payloadLen);
            this.writeByte(extType);
         } else if (payloadLen == 1) {
            this.writeByteAndByte((byte)-44, extType);
         } else if (payloadLen == 2) {
            this.writeByteAndByte((byte)-43, extType);
         } else if (payloadLen == 4) {
            this.writeByteAndByte((byte)-42, extType);
         } else if (payloadLen == 8) {
            this.writeByteAndByte((byte)-41, extType);
         } else if (payloadLen == 16) {
            this.writeByteAndByte((byte)-40, extType);
         } else {
            this.writeByteAndByte((byte)-57, (byte)payloadLen);
            this.writeByte(extType);
         }
      } else if (payloadLen < 65536) {
         this.writeByteAndShort((byte)-56, (short)payloadLen);
         this.writeByte(extType);
      } else {
         this.writeByteAndInt((byte)-55, payloadLen);
         this.writeByte(extType);
      }

      return this;
   }

   public MessagePacker packBinaryHeader(int len) throws IOException {
      if (len < 256) {
         this.writeByteAndByte((byte)-60, (byte)len);
      } else if (len < 65536) {
         this.writeByteAndShort((byte)-59, (short)len);
      } else {
         this.writeByteAndInt((byte)-58, len);
      }

      return this;
   }

   public MessagePacker packRawStringHeader(int len) throws IOException {
      if (len < 32) {
         this.writeByte((byte)(-96 | len));
      } else if (this.str8FormatSupport && len < 256) {
         this.writeByteAndByte((byte)-39, (byte)len);
      } else if (len < 65536) {
         this.writeByteAndShort((byte)-38, (short)len);
      } else {
         this.writeByteAndInt((byte)-37, len);
      }

      return this;
   }

   public MessagePacker writePayload(byte[] src) throws IOException {
      return this.writePayload(src, 0, src.length);
   }

   public MessagePacker writePayload(byte[] src, int off, int len) throws IOException {
      if (this.buffer != null && this.buffer.size() - this.position >= len && len <= this.bufferFlushThreshold) {
         this.buffer.putBytes(this.position, src, off, len);
         this.position += len;
      } else {
         this.flush();
         this.out.write(src, off, len);
         this.totalFlushBytes += (long)len;
      }

      return this;
   }

   public MessagePacker addPayload(byte[] src) throws IOException {
      return this.addPayload(src, 0, src.length);
   }

   public MessagePacker addPayload(byte[] src, int off, int len) throws IOException {
      if (this.buffer != null && this.buffer.size() - this.position >= len && len <= this.bufferFlushThreshold) {
         this.buffer.putBytes(this.position, src, off, len);
         this.position += len;
      } else {
         this.flush();
         this.out.add(src, off, len);
         this.totalFlushBytes += (long)len;
      }

      return this;
   }

   static {
      boolean corruptedCharsetEncoder = false;

      try {
         Class<?> klass = Class.forName("android.os.Build$VERSION");
         Constructor<?> constructor = klass.getConstructor();
         Object version = constructor.newInstance();
         Field sdkIntField = klass.getField("SDK_INT");
         int sdkInt = sdkIntField.getInt(version);
         if (sdkInt >= 14 && sdkInt < 21) {
            corruptedCharsetEncoder = true;
         }
      } catch (ClassNotFoundException var6) {
      } catch (NoSuchMethodException var7) {
         var7.printStackTrace();
      } catch (IllegalAccessException var8) {
         var8.printStackTrace();
      } catch (InstantiationException var9) {
         var9.printStackTrace();
      } catch (InvocationTargetException var10) {
         var10.printStackTrace();
      } catch (NoSuchFieldException var11) {
         var11.printStackTrace();
      }

      CORRUPTED_CHARSET_ENCODER = corruptedCharsetEncoder;
   }
}
