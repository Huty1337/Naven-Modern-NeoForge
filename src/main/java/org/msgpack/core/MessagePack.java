package org.msgpack.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import org.msgpack.core.buffer.ArrayBufferInput;
import org.msgpack.core.buffer.ByteBufferInput;
import org.msgpack.core.buffer.ChannelBufferInput;
import org.msgpack.core.buffer.ChannelBufferOutput;
import org.msgpack.core.buffer.InputStreamBufferInput;
import org.msgpack.core.buffer.MessageBufferInput;
import org.msgpack.core.buffer.MessageBufferOutput;
import org.msgpack.core.buffer.OutputStreamBufferOutput;

public class MessagePack {
   public static final Charset UTF8 = Charset.forName("UTF-8");
   public static final MessagePack.PackerConfig DEFAULT_PACKER_CONFIG = new MessagePack.PackerConfig();
   public static final MessagePack.UnpackerConfig DEFAULT_UNPACKER_CONFIG = new MessagePack.UnpackerConfig();

   private MessagePack() {
   }

   public static MessagePacker newDefaultPacker(MessageBufferOutput out) {
      return DEFAULT_PACKER_CONFIG.newPacker(out);
   }

   public static MessagePacker newDefaultPacker(OutputStream out) {
      return DEFAULT_PACKER_CONFIG.newPacker(out);
   }

   public static MessagePacker newDefaultPacker(WritableByteChannel channel) {
      return DEFAULT_PACKER_CONFIG.newPacker(channel);
   }

   public static MessageBufferPacker newDefaultBufferPacker() {
      return DEFAULT_PACKER_CONFIG.newBufferPacker();
   }

   public static MessageUnpacker newDefaultUnpacker(MessageBufferInput in) {
      return DEFAULT_UNPACKER_CONFIG.newUnpacker(in);
   }

   public static MessageUnpacker newDefaultUnpacker(InputStream in) {
      return DEFAULT_UNPACKER_CONFIG.newUnpacker(in);
   }

   public static MessageUnpacker newDefaultUnpacker(ReadableByteChannel channel) {
      return DEFAULT_UNPACKER_CONFIG.newUnpacker(channel);
   }

   public static MessageUnpacker newDefaultUnpacker(byte[] contents) {
      return DEFAULT_UNPACKER_CONFIG.newUnpacker(contents);
   }

   public static MessageUnpacker newDefaultUnpacker(byte[] contents, int offset, int length) {
      return DEFAULT_UNPACKER_CONFIG.newUnpacker(contents, offset, length);
   }

   public static MessageUnpacker newDefaultUnpacker(ByteBuffer contents) {
      return DEFAULT_UNPACKER_CONFIG.newUnpacker(contents);
   }

   public static final class Code {
      public static final byte POSFIXINT_MASK = -128;
      public static final byte FIXMAP_PREFIX = -128;
      public static final byte FIXARRAY_PREFIX = -112;
      public static final byte FIXSTR_PREFIX = -96;
      public static final byte NIL = -64;
      public static final byte NEVER_USED = -63;
      public static final byte FALSE = -62;
      public static final byte TRUE = -61;
      public static final byte BIN8 = -60;
      public static final byte BIN16 = -59;
      public static final byte BIN32 = -58;
      public static final byte EXT8 = -57;
      public static final byte EXT16 = -56;
      public static final byte EXT32 = -55;
      public static final byte FLOAT32 = -54;
      public static final byte FLOAT64 = -53;
      public static final byte UINT8 = -52;
      public static final byte UINT16 = -51;
      public static final byte UINT32 = -50;
      public static final byte UINT64 = -49;
      public static final byte INT8 = -48;
      public static final byte INT16 = -47;
      public static final byte INT32 = -46;
      public static final byte INT64 = -45;
      public static final byte FIXEXT1 = -44;
      public static final byte FIXEXT2 = -43;
      public static final byte FIXEXT4 = -42;
      public static final byte FIXEXT8 = -41;
      public static final byte FIXEXT16 = -40;
      public static final byte STR8 = -39;
      public static final byte STR16 = -38;
      public static final byte STR32 = -37;
      public static final byte ARRAY16 = -36;
      public static final byte ARRAY32 = -35;
      public static final byte MAP16 = -34;
      public static final byte MAP32 = -33;
      public static final byte NEGFIXINT_PREFIX = -32;
      public static final byte EXT_TIMESTAMP = -1;

      public static final boolean isFixInt(byte b) {
         int v = b & 255;
         return v <= 127 || v >= 224;
      }

      public static final boolean isPosFixInt(byte b) {
         return (b & -128) == 0;
      }

      public static final boolean isNegFixInt(byte b) {
         return (b & -32) == -32;
      }

      public static final boolean isFixStr(byte b) {
         return (b & -32) == -96;
      }

      public static final boolean isFixedArray(byte b) {
         return (b & -16) == -112;
      }

      public static final boolean isFixedMap(byte b) {
         return (b & -16) == -128;
      }

      public static final boolean isFixedRaw(byte b) {
         return (b & -32) == -96;
      }
   }

   public static class PackerConfig implements Cloneable {
      private int smallStringOptimizationThreshold = 512;
      private int bufferFlushThreshold = 8192;
      private int bufferSize = 8192;
      private boolean str8FormatSupport = true;

      public PackerConfig() {
      }

      private PackerConfig(MessagePack.PackerConfig copy) {
         this.smallStringOptimizationThreshold = copy.smallStringOptimizationThreshold;
         this.bufferFlushThreshold = copy.bufferFlushThreshold;
         this.bufferSize = copy.bufferSize;
         this.str8FormatSupport = copy.str8FormatSupport;
      }

      public MessagePack.PackerConfig clone() {
         return new MessagePack.PackerConfig(this);
      }

      @Override
      public int hashCode() {
         int result = this.smallStringOptimizationThreshold;
         result = 31 * result + this.bufferFlushThreshold;
         result = 31 * result + this.bufferSize;
         return 31 * result + (this.str8FormatSupport ? 1 : 0);
      }

      @Override
      public boolean equals(Object obj) {
         return !(obj instanceof MessagePack.PackerConfig o)
            ? false
            : this.smallStringOptimizationThreshold == o.smallStringOptimizationThreshold
               && this.bufferFlushThreshold == o.bufferFlushThreshold
               && this.bufferSize == o.bufferSize
               && this.str8FormatSupport == o.str8FormatSupport;
      }

      public MessagePacker newPacker(MessageBufferOutput out) {
         return new MessagePacker(out, this);
      }

      public MessagePacker newPacker(OutputStream out) {
         return this.newPacker(new OutputStreamBufferOutput(out, this.bufferSize));
      }

      public MessagePacker newPacker(WritableByteChannel channel) {
         return this.newPacker(new ChannelBufferOutput(channel, this.bufferSize));
      }

      public MessageBufferPacker newBufferPacker() {
         return new MessageBufferPacker(this);
      }

      public MessagePack.PackerConfig withSmallStringOptimizationThreshold(int length) {
         MessagePack.PackerConfig copy = this.clone();
         copy.smallStringOptimizationThreshold = length;
         return copy;
      }

      public int getSmallStringOptimizationThreshold() {
         return this.smallStringOptimizationThreshold;
      }

      public MessagePack.PackerConfig withBufferFlushThreshold(int bytes) {
         MessagePack.PackerConfig copy = this.clone();
         copy.bufferFlushThreshold = bytes;
         return copy;
      }

      public int getBufferFlushThreshold() {
         return this.bufferFlushThreshold;
      }

      public MessagePack.PackerConfig withBufferSize(int bytes) {
         MessagePack.PackerConfig copy = this.clone();
         copy.bufferSize = bytes;
         return copy;
      }

      public int getBufferSize() {
         return this.bufferSize;
      }

      public MessagePack.PackerConfig withStr8FormatSupport(boolean str8FormatSupport) {
         MessagePack.PackerConfig copy = this.clone();
         copy.str8FormatSupport = str8FormatSupport;
         return copy;
      }

      public boolean isStr8FormatSupport() {
         return this.str8FormatSupport;
      }
   }

   public static class UnpackerConfig implements Cloneable {
      private boolean allowReadingStringAsBinary = true;
      private boolean allowReadingBinaryAsString = true;
      private CodingErrorAction actionOnMalformedString = CodingErrorAction.REPLACE;
      private CodingErrorAction actionOnUnmappableString = CodingErrorAction.REPLACE;
      private int stringSizeLimit = Integer.MAX_VALUE;
      private int bufferSize = 8192;
      private int stringDecoderBufferSize = 8192;

      public UnpackerConfig() {
      }

      private UnpackerConfig(MessagePack.UnpackerConfig copy) {
         this.allowReadingStringAsBinary = copy.allowReadingStringAsBinary;
         this.allowReadingBinaryAsString = copy.allowReadingBinaryAsString;
         this.actionOnMalformedString = copy.actionOnMalformedString;
         this.actionOnUnmappableString = copy.actionOnUnmappableString;
         this.stringSizeLimit = copy.stringSizeLimit;
         this.bufferSize = copy.bufferSize;
      }

      public MessagePack.UnpackerConfig clone() {
         return new MessagePack.UnpackerConfig(this);
      }

      @Override
      public int hashCode() {
         int result = this.allowReadingStringAsBinary ? 1 : 0;
         result = 31 * result + (this.allowReadingBinaryAsString ? 1 : 0);
         result = 31 * result + (this.actionOnMalformedString != null ? this.actionOnMalformedString.hashCode() : 0);
         result = 31 * result + (this.actionOnUnmappableString != null ? this.actionOnUnmappableString.hashCode() : 0);
         result = 31 * result + this.stringSizeLimit;
         result = 31 * result + this.bufferSize;
         return 31 * result + this.stringDecoderBufferSize;
      }

      @Override
      public boolean equals(Object obj) {
         return !(obj instanceof MessagePack.UnpackerConfig o)
            ? false
            : this.allowReadingStringAsBinary == o.allowReadingStringAsBinary
               && this.allowReadingBinaryAsString == o.allowReadingBinaryAsString
               && this.actionOnMalformedString == o.actionOnMalformedString
               && this.actionOnUnmappableString == o.actionOnUnmappableString
               && this.stringSizeLimit == o.stringSizeLimit
               && this.stringDecoderBufferSize == o.stringDecoderBufferSize
               && this.bufferSize == o.bufferSize;
      }

      public MessageUnpacker newUnpacker(MessageBufferInput in) {
         return new MessageUnpacker(in, this);
      }

      public MessageUnpacker newUnpacker(InputStream in) {
         return this.newUnpacker(new InputStreamBufferInput(in, this.bufferSize));
      }

      public MessageUnpacker newUnpacker(ReadableByteChannel channel) {
         return this.newUnpacker(new ChannelBufferInput(channel, this.bufferSize));
      }

      public MessageUnpacker newUnpacker(byte[] contents) {
         return this.newUnpacker(new ArrayBufferInput(contents));
      }

      public MessageUnpacker newUnpacker(byte[] contents, int offset, int length) {
         return this.newUnpacker(new ArrayBufferInput(contents, offset, length));
      }

      public MessageUnpacker newUnpacker(ByteBuffer contents) {
         return this.newUnpacker(new ByteBufferInput(contents));
      }

      public MessagePack.UnpackerConfig withAllowReadingStringAsBinary(boolean enable) {
         MessagePack.UnpackerConfig copy = this.clone();
         copy.allowReadingStringAsBinary = enable;
         return copy;
      }

      public boolean getAllowReadingStringAsBinary() {
         return this.allowReadingStringAsBinary;
      }

      public MessagePack.UnpackerConfig withAllowReadingBinaryAsString(boolean enable) {
         MessagePack.UnpackerConfig copy = this.clone();
         copy.allowReadingBinaryAsString = enable;
         return copy;
      }

      public boolean getAllowReadingBinaryAsString() {
         return this.allowReadingBinaryAsString;
      }

      public MessagePack.UnpackerConfig withActionOnMalformedString(CodingErrorAction action) {
         MessagePack.UnpackerConfig copy = this.clone();
         copy.actionOnMalformedString = action;
         return copy;
      }

      public CodingErrorAction getActionOnMalformedString() {
         return this.actionOnMalformedString;
      }

      public MessagePack.UnpackerConfig withActionOnUnmappableString(CodingErrorAction action) {
         MessagePack.UnpackerConfig copy = this.clone();
         copy.actionOnUnmappableString = action;
         return copy;
      }

      public CodingErrorAction getActionOnUnmappableString() {
         return this.actionOnUnmappableString;
      }

      public MessagePack.UnpackerConfig withStringSizeLimit(int bytes) {
         MessagePack.UnpackerConfig copy = this.clone();
         copy.stringSizeLimit = bytes;
         return copy;
      }

      public int getStringSizeLimit() {
         return this.stringSizeLimit;
      }

      public MessagePack.UnpackerConfig withStringDecoderBufferSize(int bytes) {
         MessagePack.UnpackerConfig copy = this.clone();
         copy.stringDecoderBufferSize = bytes;
         return copy;
      }

      public int getStringDecoderBufferSize() {
         return this.stringDecoderBufferSize;
      }

      public MessagePack.UnpackerConfig withBufferSize(int bytes) {
         MessagePack.UnpackerConfig copy = this.clone();
         copy.bufferSize = bytes;
         return copy;
      }

      public int getBufferSize() {
         return this.bufferSize;
      }
   }
}
