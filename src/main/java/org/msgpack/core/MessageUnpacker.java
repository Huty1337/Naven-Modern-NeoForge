package org.msgpack.core;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.time.Instant;
import org.msgpack.core.buffer.MessageBuffer;
import org.msgpack.core.buffer.MessageBufferInput;
import org.msgpack.value.ImmutableValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;
import org.msgpack.value.Variable;

public class MessageUnpacker implements Closeable {
   private static final MessageBuffer EMPTY_BUFFER = MessageBuffer.wrap(new byte[0]);
   private final boolean allowReadingStringAsBinary;
   private final boolean allowReadingBinaryAsString;
   private final CodingErrorAction actionOnMalformedString;
   private final CodingErrorAction actionOnUnmappableString;
   private final int stringSizeLimit;
   private final int stringDecoderBufferSize;
   private MessageBufferInput in;
   private MessageBuffer buffer = EMPTY_BUFFER;
   private int position;
   private long totalReadBytes;
   private final MessageBuffer numberBuffer = MessageBuffer.allocate(8);
   private int nextReadPosition;
   private StringBuilder decodeStringBuffer;
   private CharsetDecoder decoder;
   private CharBuffer decodeBuffer;
   private static final String EMPTY_STRING = "";

   protected MessageUnpacker(MessageBufferInput in, MessagePack.UnpackerConfig config) {
      this.in = Preconditions.checkNotNull(in, "MessageBufferInput is null");
      this.allowReadingStringAsBinary = config.getAllowReadingStringAsBinary();
      this.allowReadingBinaryAsString = config.getAllowReadingBinaryAsString();
      this.actionOnMalformedString = config.getActionOnMalformedString();
      this.actionOnUnmappableString = config.getActionOnUnmappableString();
      this.stringSizeLimit = config.getStringSizeLimit();
      this.stringDecoderBufferSize = config.getStringDecoderBufferSize();
   }

   public MessageBufferInput reset(MessageBufferInput in) throws IOException {
      MessageBufferInput newIn = Preconditions.checkNotNull(in, "MessageBufferInput is null");
      MessageBufferInput old = this.in;
      this.in = newIn;
      this.buffer = EMPTY_BUFFER;
      this.position = 0;
      this.totalReadBytes = 0L;
      return old;
   }

   public long getTotalReadBytes() {
      return this.totalReadBytes + (long)this.position;
   }

   private MessageBuffer getNextBuffer() throws IOException {
      MessageBuffer next = this.in.next();
      if (next == null) {
         throw new MessageInsufficientBufferException();
      } else {
         assert this.buffer != null;

         this.totalReadBytes = this.totalReadBytes + (long)this.buffer.size();
         return next;
      }
   }

   private void nextBuffer() throws IOException {
      this.buffer = this.getNextBuffer();
      this.position = 0;
   }

   private MessageBuffer prepareNumberBuffer(int readLength) throws IOException {
      int remaining = this.buffer.size() - this.position;
      if (remaining >= readLength) {
         this.nextReadPosition = this.position;
         this.position += readLength;
         return this.buffer;
      } else {
         int off = 0;
         if (remaining > 0) {
            this.numberBuffer.putMessageBuffer(0, this.buffer, this.position, remaining);
            readLength -= remaining;
            off += remaining;
         }

         while (true) {
            this.nextBuffer();
            int nextSize = this.buffer.size();
            if (nextSize >= readLength) {
               this.numberBuffer.putMessageBuffer(off, this.buffer, 0, readLength);
               this.position = readLength;
               this.nextReadPosition = 0;
               return this.numberBuffer;
            }

            this.numberBuffer.putMessageBuffer(off, this.buffer, 0, nextSize);
            readLength -= nextSize;
            off += nextSize;
         }
      }
   }

   private static int utf8MultibyteCharacterSize(byte firstByte) {
      return Integer.numberOfLeadingZeros(~(firstByte & 0xFF) << 24);
   }

   public boolean hasNext() throws IOException {
      return this.ensureBuffer();
   }

   private boolean ensureBuffer() throws IOException {
      while (this.buffer.size() <= this.position) {
         MessageBuffer next = this.in.next();
         if (next == null) {
            return false;
         }

         this.totalReadBytes = this.totalReadBytes + (long)this.buffer.size();
         this.buffer = next;
         this.position = 0;
      }

      return true;
   }

   public MessageFormat getNextFormat() throws IOException {
      if (!this.ensureBuffer()) {
         throw new MessageInsufficientBufferException();
      } else {
         byte b = this.buffer.getByte(this.position);
         return MessageFormat.valueOf(b);
      }
   }

   private byte readByte() throws IOException {
      if (this.buffer.size() > this.position) {
         byte b = this.buffer.getByte(this.position);
         this.position++;
         return b;
      } else {
         this.nextBuffer();
         if (this.buffer.size() > 0) {
            byte b = this.buffer.getByte(0);
            this.position = 1;
            return b;
         } else {
            return this.readByte();
         }
      }
   }

   private short readShort() throws IOException {
      MessageBuffer numberBuffer = this.prepareNumberBuffer(2);
      return numberBuffer.getShort(this.nextReadPosition);
   }

   private int readInt() throws IOException {
      MessageBuffer numberBuffer = this.prepareNumberBuffer(4);
      return numberBuffer.getInt(this.nextReadPosition);
   }

   private long readLong() throws IOException {
      MessageBuffer numberBuffer = this.prepareNumberBuffer(8);
      return numberBuffer.getLong(this.nextReadPosition);
   }

   private float readFloat() throws IOException {
      MessageBuffer numberBuffer = this.prepareNumberBuffer(4);
      return numberBuffer.getFloat(this.nextReadPosition);
   }

   private double readDouble() throws IOException {
      MessageBuffer numberBuffer = this.prepareNumberBuffer(8);
      return numberBuffer.getDouble(this.nextReadPosition);
   }

   public void skipValue() throws IOException {
      this.skipValue(1);
   }

   public void skipValue(int count) throws IOException {
      while (count > 0) {
         byte b = this.readByte();
         MessageFormat f = MessageFormat.valueOf(b);
         switch (f) {
            case POSFIXINT:
            case NEGFIXINT:
            case BOOLEAN:
            case NIL:
            default:
               break;
            case FIXMAP:
               int mapLen = b & 15;
               count += mapLen * 2;
               break;
            case FIXARRAY:
               int arrayLen = b & 15;
               count += arrayLen;
               break;
            case FIXSTR:
               int strLen = b & 31;
               this.skipPayload(strLen);
               break;
            case INT8:
            case UINT8:
               this.skipPayload(1);
               break;
            case INT16:
            case UINT16:
               this.skipPayload(2);
               break;
            case INT32:
            case UINT32:
            case FLOAT32:
               this.skipPayload(4);
               break;
            case INT64:
            case UINT64:
            case FLOAT64:
               this.skipPayload(8);
               break;
            case BIN8:
            case STR8:
               this.skipPayload(this.readNextLength8());
               break;
            case BIN16:
            case STR16:
               this.skipPayload(this.readNextLength16());
               break;
            case BIN32:
            case STR32:
               this.skipPayload(this.readNextLength32());
               break;
            case FIXEXT1:
               this.skipPayload(2);
               break;
            case FIXEXT2:
               this.skipPayload(3);
               break;
            case FIXEXT4:
               this.skipPayload(5);
               break;
            case FIXEXT8:
               this.skipPayload(9);
               break;
            case FIXEXT16:
               this.skipPayload(17);
               break;
            case EXT8:
               this.skipPayload(this.readNextLength8() + 1);
               break;
            case EXT16:
               this.skipPayload(this.readNextLength16() + 1);
               break;
            case EXT32:
               int extLen = this.readNextLength32();
               this.skipPayload(1);
               this.skipPayload(extLen);
               break;
            case ARRAY16:
               count += this.readNextLength16();
               break;
            case ARRAY32:
               count += this.readNextLength32();
               break;
            case MAP16:
               count += this.readNextLength16() * 2;
               break;
            case MAP32:
               count += this.readNextLength32() * 2;
               break;
            case NEVER_USED:
               throw new MessageNeverUsedFormatException("Encountered 0xC1 \"NEVER_USED\" byte");
         }

         count--;
      }
   }

   private static MessagePackException unexpected(String expected, byte b) {
      MessageFormat format = MessageFormat.valueOf(b);
      if (format == MessageFormat.NEVER_USED) {
         return new MessageNeverUsedFormatException(String.format("Expected %s, but encountered 0xC1 \"NEVER_USED\" byte", expected));
      } else {
         String name = format.getValueType().name();
         String typeName = name.substring(0, 1) + name.substring(1).toLowerCase();
         return new MessageTypeException(String.format("Expected %s, but got %s (%02x)", expected, typeName, b));
      }
   }

   private static MessagePackException unexpectedExtension(String expected, int expectedType, int actualType) {
      return new MessageTypeException(String.format("Expected extension type %s (%d), but got extension type %d", expected, expectedType, actualType));
   }

   public ImmutableValue unpackValue() throws IOException {
      MessageFormat mf = this.getNextFormat();
      switch (mf.getValueType()) {
         case NIL:
            this.readByte();
            return ValueFactory.newNil();
         case BOOLEAN:
            return ValueFactory.newBoolean(this.unpackBoolean());
         case INTEGER:
            if (mf == MessageFormat.UINT64) {
               return ValueFactory.newInteger(this.unpackBigInteger());
            }

            return ValueFactory.newInteger(this.unpackLong());
         case FLOAT:
            return ValueFactory.newFloat(this.unpackDouble());
         case STRING: {
            int length = this.unpackRawStringHeader();
            if (length > this.stringSizeLimit) {
               throw new MessageSizeException(String.format("cannot unpack a String of size larger than %,d: %,d", this.stringSizeLimit, length), (long)length);
            }

            return ValueFactory.newString(this.readPayload(length), true);
         }
         case BINARY: {
            int length = this.unpackBinaryHeader();
            return ValueFactory.newBinary(this.readPayload(length), true);
         }
         case ARRAY:
            int size = this.unpackArrayHeader();
            Value[] array = new Value[size];

            for (int i = 0; i < size; i++) {
               array[i] = this.unpackValue();
            }

            return ValueFactory.newArray(array, true);
         case MAP:
            int mapSize = this.unpackMapHeader();
            Value[] kvs = new Value[mapSize * 2];

            for (int i = 0; i < mapSize * 2; i++) {
               kvs[i] = this.unpackValue();
               kvs[i] = this.unpackValue();
               i++;
            }

            return ValueFactory.newMap(kvs, true);
         case EXTENSION:
            ExtensionTypeHeader extHeader = this.unpackExtensionTypeHeader();
            switch (extHeader.getType()) {
               case -1:
                  return ValueFactory.newTimestamp(this.unpackTimestamp(extHeader));
               default:
                  return ValueFactory.newExtension(extHeader.getType(), this.readPayload(extHeader.getLength()));
            }
         default:
            throw new MessageNeverUsedFormatException("Unknown value type");
      }
   }

   public Variable unpackValue(Variable var) throws IOException {
      MessageFormat mf = this.getNextFormat();
      switch (mf.getValueType()) {
         case NIL:
            this.readByte();
            var.setNilValue();
            return var;
         case BOOLEAN:
            var.setBooleanValue(this.unpackBoolean());
            return var;
         case INTEGER:
            switch (mf) {
               case UINT64:
                  var.setIntegerValue(this.unpackBigInteger());
                  return var;
               default:
                  var.setIntegerValue(this.unpackLong());
                  return var;
            }
         case FLOAT:
            var.setFloatValue(this.unpackDouble());
            return var;
         case STRING: {
            int length = this.unpackRawStringHeader();
            if (length > this.stringSizeLimit) {
               throw new MessageSizeException(String.format("cannot unpack a String of size larger than %,d: %,d", this.stringSizeLimit, length), (long)length);
            }

            var.setStringValue(this.readPayload(length));
            return var;
         }
         case BINARY: {
            int length = this.unpackBinaryHeader();
            var.setBinaryValue(this.readPayload(length));
            return var;
         }
         case ARRAY:
            int arraySize = this.unpackArrayHeader();
            Value[] arrayKvs = new Value[arraySize];

            for (int i = 0; i < arraySize; i++) {
               arrayKvs[i] = this.unpackValue();
            }

            var.setArrayValue(arrayKvs);
            return var;
         case MAP:
            int mapSize = this.unpackMapHeader();
            Value[] mapKvs = new Value[mapSize * 2];

            for (int i = 0; i < mapSize * 2; i++) {
               mapKvs[i] = this.unpackValue();
               mapKvs[i] = this.unpackValue();
               i++;
            }

            var.setMapValue(mapKvs);
            return var;
         case EXTENSION:
            ExtensionTypeHeader extHeader = this.unpackExtensionTypeHeader();
            switch (extHeader.getType()) {
               case -1:
                  var.setTimestampValue(this.unpackTimestamp(extHeader));
                  break;
               default:
                  var.setExtensionValue(extHeader.getType(), this.readPayload(extHeader.getLength()));
            }

            return var;
         default:
            throw new MessageFormatException("Unknown value type");
      }
   }

   public void unpackNil() throws IOException {
      byte b = this.readByte();
      if (b != -64) {
         throw unexpected("Nil", b);
      }
   }

   public boolean tryUnpackNil() throws IOException {
      if (!this.ensureBuffer()) {
         throw new MessageInsufficientBufferException();
      } else {
         byte b = this.buffer.getByte(this.position);
         if (b == -64) {
            this.readByte();
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean unpackBoolean() throws IOException {
      byte b = this.readByte();
      if (b == -62) {
         return false;
      } else if (b == -61) {
         return true;
      } else {
         throw unexpected("boolean", b);
      }
   }

   public byte unpackByte() throws IOException {
      byte b = this.readByte();
      if (MessagePack.Code.isFixInt(b)) {
         return b;
      } else {
         switch (b) {
            case -52:
               byte u8 = this.readByte();
               if (u8 < 0) {
                  throw overflowU8(u8);
               }

               return u8;
            case -51:
               short u16 = this.readShort();
               if (u16 >= 0 && u16 <= 127) {
                  return (byte)u16;
               }

               throw overflowU16(u16);
            case -50:
               int u32 = this.readInt();
               if (u32 >= 0 && u32 <= 127) {
                  return (byte)u32;
               }

               throw overflowU32(u32);
            case -49:
               long u64 = this.readLong();
               if (u64 >= 0L && u64 <= 127L) {
                  return (byte)((int)u64);
               }

               throw overflowU64(u64);
            case -48:
               return this.readByte();
            case -47:
               short i16 = this.readShort();
               if (i16 >= -128 && i16 <= 127) {
                  return (byte)i16;
               }

               throw overflowI16(i16);
            case -46:
               int i32 = this.readInt();
               if (i32 >= -128 && i32 <= 127) {
                  return (byte)i32;
               }

               throw overflowI32(i32);
            case -45:
               long i64 = this.readLong();
               if (i64 >= -128L && i64 <= 127L) {
                  return (byte)((int)i64);
               }

               throw overflowI64(i64);
            default:
               throw unexpected("Integer", b);
         }
      }
   }

   public short unpackShort() throws IOException {
      byte b = this.readByte();
      if (MessagePack.Code.isFixInt(b)) {
         return (short)b;
      } else {
         switch (b) {
            case -52:
               byte u8 = this.readByte();
               return (short)(u8 & 255);
            case -51:
               short u16 = this.readShort();
               if (u16 < 0) {
                  throw overflowU16(u16);
               }

               return u16;
            case -50:
               int u32 = this.readInt();
               if (u32 >= 0 && u32 <= 32767) {
                  return (short)u32;
               }

               throw overflowU32(u32);
            case -49:
               long u64 = this.readLong();
               if (u64 >= 0L && u64 <= 32767L) {
                  return (short)((int)u64);
               }

               throw overflowU64(u64);
            case -48:
               byte i8 = this.readByte();
               return (short)i8;
            case -47:
               return this.readShort();
            case -46:
               int i32 = this.readInt();
               if (i32 >= -32768 && i32 <= 32767) {
                  return (short)i32;
               }

               throw overflowI32(i32);
            case -45:
               long i64 = this.readLong();
               if (i64 >= -32768L && i64 <= 32767L) {
                  return (short)((int)i64);
               }

               throw overflowI64(i64);
            default:
               throw unexpected("Integer", b);
         }
      }
   }

   public int unpackInt() throws IOException {
      byte b = this.readByte();
      if (MessagePack.Code.isFixInt(b)) {
         return b;
      } else {
         switch (b) {
            case -52:
               byte u8 = this.readByte();
               return u8 & 0xFF;
            case -51:
               short u16 = this.readShort();
               return u16 & 65535;
            case -50:
               int u32 = this.readInt();
               if (u32 < 0) {
                  throw overflowU32(u32);
               }

               return u32;
            case -49:
               long u64 = this.readLong();
               if (u64 >= 0L && u64 <= 2147483647L) {
                  return (int)u64;
               }

               throw overflowU64(u64);
            case -48:
               return this.readByte();
            case -47:
               return this.readShort();
            case -46:
               return this.readInt();
            case -45:
               long i64 = this.readLong();
               if (i64 >= -2147483648L && i64 <= 2147483647L) {
                  return (int)i64;
               }

               throw overflowI64(i64);
            default:
               throw unexpected("Integer", b);
         }
      }
   }

   public long unpackLong() throws IOException {
      byte b = this.readByte();
      if (MessagePack.Code.isFixInt(b)) {
         return (long)b;
      } else {
         switch (b) {
            case -52:
               byte u8 = this.readByte();
               return (long)(u8 & 255);
            case -51:
               short u16 = this.readShort();
               return (long)(u16 & '\uffff');
            case -50:
               int u32 = this.readInt();
               if (u32 < 0) {
                  return (long)(u32 & 2147483647) + 2147483648L;
               }

               return (long)u32;
            case -49:
               long u64 = this.readLong();
               if (u64 < 0L) {
                  throw overflowU64(u64);
               }

               return u64;
            case -48:
               byte i8 = this.readByte();
               return (long)i8;
            case -47:
               short i16 = this.readShort();
               return (long)i16;
            case -46:
               int i32 = this.readInt();
               return (long)i32;
            case -45:
               return this.readLong();
            default:
               throw unexpected("Integer", b);
         }
      }
   }

   public BigInteger unpackBigInteger() throws IOException {
      byte b = this.readByte();
      if (MessagePack.Code.isFixInt(b)) {
         return BigInteger.valueOf((long)b);
      } else {
         switch (b) {
            case -52:
               byte u8 = this.readByte();
               return BigInteger.valueOf((long)(u8 & 255));
            case -51:
               short u16 = this.readShort();
               return BigInteger.valueOf((long)(u16 & '\uffff'));
            case -50:
               int u32 = this.readInt();
               if (u32 < 0) {
                  return BigInteger.valueOf((long)(u32 & 2147483647) + 2147483648L);
               }

               return BigInteger.valueOf((long)u32);
            case -49:
               long u64 = this.readLong();
               if (u64 < 0L) {
                  return BigInteger.valueOf(u64 + Long.MAX_VALUE + 1L).setBit(63);
               }

               return BigInteger.valueOf(u64);
            case -48:
               byte i8 = this.readByte();
               return BigInteger.valueOf((long)i8);
            case -47:
               short i16 = this.readShort();
               return BigInteger.valueOf((long)i16);
            case -46:
               int i32 = this.readInt();
               return BigInteger.valueOf((long)i32);
            case -45:
               long i64 = this.readLong();
               return BigInteger.valueOf(i64);
            default:
               throw unexpected("Integer", b);
         }
      }
   }

   public float unpackFloat() throws IOException {
      byte b = this.readByte();
      switch (b) {
         case -54:
            return this.readFloat();
         case -53:
            double dv = this.readDouble();
            return (float)dv;
         default:
            throw unexpected("Float", b);
      }
   }

   public double unpackDouble() throws IOException {
      byte b = this.readByte();
      switch (b) {
         case -54:
            float fv = this.readFloat();
            return (double)fv;
         case -53:
            return this.readDouble();
         default:
            throw unexpected("Float", b);
      }
   }

   private void resetDecoder() {
      if (this.decoder == null) {
         this.decodeBuffer = CharBuffer.allocate(this.stringDecoderBufferSize);
         this.decoder = MessagePack.UTF8.newDecoder().onMalformedInput(this.actionOnMalformedString).onUnmappableCharacter(this.actionOnUnmappableString);
      } else {
         this.decoder.reset();
      }

      if (this.decodeStringBuffer == null) {
         this.decodeStringBuffer = new StringBuilder();
      } else {
         this.decodeStringBuffer.setLength(0);
      }
   }

   public String unpackString() throws IOException {
      int len = this.unpackRawStringHeader();
      if (len == 0) {
         return "";
      } else if (len > this.stringSizeLimit) {
         throw new MessageSizeException(String.format("cannot unpack a String of size larger than %,d: %,d", this.stringSizeLimit, len), (long)len);
      } else {
         this.resetDecoder();
         if (this.buffer.size() - this.position >= len) {
            return this.decodeStringFastPath(len);
         } else {
            try {
               int rawRemaining = len;

               label70:
               while (rawRemaining > 0) {
                  int bufferRemaining = this.buffer.size() - this.position;
                  if (bufferRemaining < rawRemaining) {
                     if (bufferRemaining == 0) {
                        this.nextBuffer();
                        continue;
                     }

                     ByteBuffer bb = this.buffer.sliceAsByteBuffer(this.position, bufferRemaining);
                     int bbStartPosition = bb.position();
                     this.decodeBuffer.clear();
                     CoderResult cr = this.decoder.decode(bb, this.decodeBuffer, false);
                     int readLen = bb.position() - bbStartPosition;
                     this.position += readLen;
                     rawRemaining -= readLen;
                     this.decodeStringBuffer.append((CharSequence)this.decodeBuffer.flip());
                     if (cr.isError()) {
                        this.handleCoderError(cr);
                     }

                     if (!cr.isUnderflow() || readLen >= bufferRemaining) {
                        continue;
                     }

                     int incompleteMultiBytes = utf8MultibyteCharacterSize(this.buffer.getByte(this.position));
                     ByteBuffer multiByteBuffer = ByteBuffer.allocate(incompleteMultiBytes);
                     this.buffer.getBytes(this.position, this.buffer.size() - this.position, multiByteBuffer);

                     while (true) {
                        this.nextBuffer();
                        int more = multiByteBuffer.remaining();
                        if (this.buffer.size() >= more) {
                           this.buffer.getBytes(0, more, multiByteBuffer);
                           this.position = more;
                           multiByteBuffer.position(0);
                           this.decodeBuffer.clear();
                           cr = this.decoder.decode(multiByteBuffer, this.decodeBuffer, false);
                           if (cr.isError()) {
                              this.handleCoderError(cr);
                           }

                           if (cr.isOverflow() || cr.isUnderflow() && multiByteBuffer.position() < multiByteBuffer.limit()) {
                              try {
                                 cr.throwException();
                                 throw new MessageFormatException("Unexpected UTF-8 multibyte sequence");
                              } catch (Exception var11) {
                                 throw new MessageFormatException("Unexpected UTF-8 multibyte sequence", var11);
                              }
                           }

                           rawRemaining -= multiByteBuffer.limit();
                           this.decodeStringBuffer.append((CharSequence)this.decodeBuffer.flip());
                           continue label70;
                        }

                        this.buffer.getBytes(0, this.buffer.size(), multiByteBuffer);
                        this.position = this.buffer.size();
                     }
                  }

                  this.decodeStringBuffer.append(this.decodeStringFastPath(rawRemaining));
                  break;
               }

               return this.decodeStringBuffer.toString();
            } catch (CharacterCodingException var12) {
               throw new MessageStringCodingException(var12);
            }
         }
      }
   }

   private void handleCoderError(CoderResult cr) throws CharacterCodingException {
      if (cr.isMalformed() && this.actionOnMalformedString == CodingErrorAction.REPORT
         || cr.isUnmappable() && this.actionOnUnmappableString == CodingErrorAction.REPORT) {
         cr.throwException();
      }
   }

   private String decodeStringFastPath(int length) {
      if (this.actionOnMalformedString == CodingErrorAction.REPLACE && this.actionOnUnmappableString == CodingErrorAction.REPLACE && this.buffer.hasArray()) {
         String s = new String(this.buffer.array(), this.buffer.arrayOffset() + this.position, length, MessagePack.UTF8);
         this.position += length;
         return s;
      } else {
         ByteBuffer bb = this.buffer.sliceAsByteBuffer(this.position, length);

         CharBuffer cb;
         try {
            cb = this.decoder.decode(bb);
         } catch (CharacterCodingException var5) {
            throw new MessageStringCodingException(var5);
         }

         this.position += length;
         return cb.toString();
      }
   }

   public Instant unpackTimestamp() throws IOException {
      ExtensionTypeHeader ext = this.unpackExtensionTypeHeader();
      return this.unpackTimestamp(ext);
   }

   public Instant unpackTimestamp(ExtensionTypeHeader ext) throws IOException {
      if (ext.getType() != -1) {
         throw unexpectedExtension("Timestamp", -1, ext.getType());
      } else {
         switch (ext.getLength()) {
            case 4:
               long u32 = (long)this.readInt() & 4294967295L;
               return Instant.ofEpochSecond(u32);
            case 8: {
               long data64 = this.readLong();
               int nsec = (int)(data64 >>> 34);
               long sec = data64 & 17179869183L;
               return Instant.ofEpochSecond(sec, (long)nsec);
            }
            case 12: {
               long nsecU32 = (long)this.readInt() & 4294967295L;
               long sec = this.readLong();
               return Instant.ofEpochSecond(sec, nsecU32);
            }
            default:
               throw new MessageFormatException(
                  String.format("Timestamp extension type (%d) expects 4, 8, or 12 bytes of payload but got %d bytes", (byte)-1, ext.getLength())
               );
         }
      }
   }

   public int unpackArrayHeader() throws IOException {
      byte b = this.readByte();
      if (MessagePack.Code.isFixedArray(b)) {
         return b & 15;
      } else {
         switch (b) {
            case -36:
               return this.readNextLength16();
            case -35:
               return this.readNextLength32();
            default:
               throw unexpected("Array", b);
         }
      }
   }

   public int unpackMapHeader() throws IOException {
      byte b = this.readByte();
      if (MessagePack.Code.isFixedMap(b)) {
         return b & 15;
      } else {
         switch (b) {
            case -34:
               return this.readNextLength16();
            case -33:
               return this.readNextLength32();
            default:
               throw unexpected("Map", b);
         }
      }
   }

   public ExtensionTypeHeader unpackExtensionTypeHeader() throws IOException {
      byte b = this.readByte();
      switch (b) {
         case -57: {
            MessageBuffer numberBuffer = this.prepareNumberBuffer(2);
            int u8 = numberBuffer.getByte(this.nextReadPosition);
            int length = u8 & 0xFF;
            byte type = numberBuffer.getByte(this.nextReadPosition + 1);
            return new ExtensionTypeHeader(type, length);
         }
         case -56: {
            MessageBuffer numberBuffer = this.prepareNumberBuffer(3);
            int u16 = numberBuffer.getShort(this.nextReadPosition);
            int length = u16 & 65535;
            byte type = numberBuffer.getByte(this.nextReadPosition + 2);
            return new ExtensionTypeHeader(type, length);
         }
         case -55: {
            MessageBuffer numberBuffer = this.prepareNumberBuffer(5);
            int u32 = numberBuffer.getInt(this.nextReadPosition);
            if (u32 < 0) {
               throw overflowU32Size(u32);
            }

            byte type = numberBuffer.getByte(this.nextReadPosition + 4);
            return new ExtensionTypeHeader(type, u32);
         }
         case -54:
         case -53:
         case -52:
         case -51:
         case -50:
         case -49:
         case -48:
         case -47:
         case -46:
         case -45:
         default:
            throw unexpected("Ext", b);
         case -44: {
            byte type = this.readByte();
            return new ExtensionTypeHeader(type, 1);
         }
         case -43: {
            byte type = this.readByte();
            return new ExtensionTypeHeader(type, 2);
         }
         case -42: {
            byte type = this.readByte();
            return new ExtensionTypeHeader(type, 4);
         }
         case -41: {
            byte type = this.readByte();
            return new ExtensionTypeHeader(type, 8);
         }
         case -40: {
            byte type = this.readByte();
            return new ExtensionTypeHeader(type, 16);
         }
      }
   }

   private int tryReadStringHeader(byte b) throws IOException {
      switch (b) {
         case -39:
            return this.readNextLength8();
         case -38:
            return this.readNextLength16();
         case -37:
            return this.readNextLength32();
         default:
            return -1;
      }
   }

   private int tryReadBinaryHeader(byte b) throws IOException {
      switch (b) {
         case -60:
            return this.readNextLength8();
         case -59:
            return this.readNextLength16();
         case -58:
            return this.readNextLength32();
         default:
            return -1;
      }
   }

   public int unpackRawStringHeader() throws IOException {
      byte b = this.readByte();
      if (MessagePack.Code.isFixedRaw(b)) {
         return b & 31;
      } else {
         int len = this.tryReadStringHeader(b);
         if (len >= 0) {
            return len;
         } else {
            if (this.allowReadingBinaryAsString) {
               len = this.tryReadBinaryHeader(b);
               if (len >= 0) {
                  return len;
               }
            }

            throw unexpected("String", b);
         }
      }
   }

   public int unpackBinaryHeader() throws IOException {
      byte b = this.readByte();
      if (MessagePack.Code.isFixedRaw(b)) {
         return b & 31;
      } else {
         int len = this.tryReadBinaryHeader(b);
         if (len >= 0) {
            return len;
         } else {
            if (this.allowReadingStringAsBinary) {
               len = this.tryReadStringHeader(b);
               if (len >= 0) {
                  return len;
               }
            }

            throw unexpected("Binary", b);
         }
      }
   }

   private void skipPayload(int numBytes) throws IOException {
      if (numBytes < 0) {
         throw new IllegalArgumentException("payload size must be >= 0: " + numBytes);
      } else {
         while (true) {
            int bufferRemaining = this.buffer.size() - this.position;
            if (bufferRemaining >= numBytes) {
               this.position += numBytes;
               return;
            }

            this.position += bufferRemaining;
            numBytes -= bufferRemaining;
            this.nextBuffer();
         }
      }
   }

   public void readPayload(ByteBuffer dst) throws IOException {
      while (true) {
         int dstRemaining = dst.remaining();
         int bufferRemaining = this.buffer.size() - this.position;
         if (bufferRemaining >= dstRemaining) {
            this.buffer.getBytes(this.position, dstRemaining, dst);
            this.position += dstRemaining;
            return;
         }

         this.buffer.getBytes(this.position, bufferRemaining, dst);
         this.position += bufferRemaining;
         this.nextBuffer();
      }
   }

   public void readPayload(MessageBuffer dst, int off, int len) throws IOException {
      while (true) {
         int bufferRemaining = this.buffer.size() - this.position;
         if (bufferRemaining >= len) {
            dst.putMessageBuffer(off, this.buffer, this.position, len);
            this.position += len;
            return;
         }

         dst.putMessageBuffer(off, this.buffer, this.position, bufferRemaining);
         off += bufferRemaining;
         len -= bufferRemaining;
         this.position += bufferRemaining;
         this.nextBuffer();
      }
   }

   public void readPayload(byte[] dst) throws IOException {
      this.readPayload(dst, 0, dst.length);
   }

   public byte[] readPayload(int length) throws IOException {
      byte[] newArray = new byte[length];
      this.readPayload(newArray);
      return newArray;
   }

   public void readPayload(byte[] dst, int off, int len) throws IOException {
      while (true) {
         int bufferRemaining = this.buffer.size() - this.position;
         if (bufferRemaining >= len) {
            this.buffer.getBytes(this.position, dst, off, len);
            this.position += len;
            return;
         }

         this.buffer.getBytes(this.position, dst, off, bufferRemaining);
         off += bufferRemaining;
         len -= bufferRemaining;
         this.position += bufferRemaining;
         this.nextBuffer();
      }
   }

   public MessageBuffer readPayloadAsReference(int length) throws IOException {
      int bufferRemaining = this.buffer.size() - this.position;
      if (bufferRemaining >= length) {
         MessageBuffer slice = this.buffer.slice(this.position, length);
         this.position += length;
         return slice;
      } else {
         MessageBuffer dst = MessageBuffer.allocate(length);
         this.readPayload(dst, 0, length);
         return dst;
      }
   }

   private int readNextLength8() throws IOException {
      byte u8 = this.readByte();
      return u8 & 0xFF;
   }

   private int readNextLength16() throws IOException {
      short u16 = this.readShort();
      return u16 & 65535;
   }

   private int readNextLength32() throws IOException {
      int u32 = this.readInt();
      if (u32 < 0) {
         throw overflowU32Size(u32);
      } else {
         return u32;
      }
   }

   @Override
   public void close() throws IOException {
      this.totalReadBytes = this.totalReadBytes + (long)this.position;
      this.buffer = EMPTY_BUFFER;
      this.position = 0;
      this.in.close();
   }

   private static MessageIntegerOverflowException overflowU8(byte u8) {
      BigInteger bi = BigInteger.valueOf((long)(u8 & 255));
      return new MessageIntegerOverflowException(bi);
   }

   private static MessageIntegerOverflowException overflowU16(short u16) {
      BigInteger bi = BigInteger.valueOf((long)(u16 & '\uffff'));
      return new MessageIntegerOverflowException(bi);
   }

   private static MessageIntegerOverflowException overflowU32(int u32) {
      BigInteger bi = BigInteger.valueOf((long)(u32 & 2147483647) + 2147483648L);
      return new MessageIntegerOverflowException(bi);
   }

   private static MessageIntegerOverflowException overflowU64(long u64) {
      BigInteger bi = BigInteger.valueOf(u64 + Long.MAX_VALUE + 1L).setBit(63);
      return new MessageIntegerOverflowException(bi);
   }

   private static MessageIntegerOverflowException overflowI16(short i16) {
      BigInteger bi = BigInteger.valueOf((long)i16);
      return new MessageIntegerOverflowException(bi);
   }

   private static MessageIntegerOverflowException overflowI32(int i32) {
      BigInteger bi = BigInteger.valueOf((long)i32);
      return new MessageIntegerOverflowException(bi);
   }

   private static MessageIntegerOverflowException overflowI64(long i64) {
      BigInteger bi = BigInteger.valueOf(i64);
      return new MessageIntegerOverflowException(bi);
   }

   private static MessageSizeException overflowU32Size(int u32) {
      long lv = (long)(u32 & 2147483647) + 2147483648L;
      return new MessageSizeException(lv);
   }
}
