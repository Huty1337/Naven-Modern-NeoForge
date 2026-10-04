package org.msgpack.value.impl;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageStringCodingException;
import org.msgpack.value.ImmutableRawValue;

public abstract class AbstractImmutableRawValue extends AbstractImmutableValue implements ImmutableRawValue {
   protected final byte[] data;
   private volatile String decodedStringCache;
   private volatile CharacterCodingException codingException;
   private static final char[] HEX_TABLE = "0123456789ABCDEF".toCharArray();

   public AbstractImmutableRawValue(byte[] data) {
      this.data = data;
   }

   public AbstractImmutableRawValue(String string) {
      this.decodedStringCache = string;
      this.data = string.getBytes(MessagePack.UTF8);
   }

   @Override
   public ImmutableRawValue asRawValue() {
      return this;
   }

   @Override
   public byte[] asByteArray() {
      return Arrays.copyOf(this.data, this.data.length);
   }

   @Override
   public ByteBuffer asByteBuffer() {
      return ByteBuffer.wrap(this.data).asReadOnlyBuffer();
   }

   @Override
   public String asString() {
      if (this.decodedStringCache == null) {
         this.decodeString();
      }

      if (this.codingException != null) {
         throw new MessageStringCodingException(this.codingException);
      } else {
         return this.decodedStringCache;
      }
   }

   @Override
   public String toJson() {
      StringBuilder sb = new StringBuilder();
      appendJsonString(sb, this.toString());
      return sb.toString();
   }

   private void decodeString() {
      synchronized (this.data) {
         if (this.decodedStringCache == null) {
            try {
               CharsetDecoder reportDecoder = MessagePack.UTF8
                  .newDecoder()
                  .onMalformedInput(CodingErrorAction.REPORT)
                  .onUnmappableCharacter(CodingErrorAction.REPORT);
               this.decodedStringCache = reportDecoder.decode(this.asByteBuffer()).toString();
            } catch (CharacterCodingException var6) {
               try {
                  CharsetDecoder replaceDecoder = MessagePack.UTF8
                     .newDecoder()
                     .onMalformedInput(CodingErrorAction.REPLACE)
                     .onUnmappableCharacter(CodingErrorAction.REPLACE);
                  this.decodedStringCache = replaceDecoder.decode(this.asByteBuffer()).toString();
               } catch (CharacterCodingException var5) {
                  throw new MessageStringCodingException(var5);
               }

               this.codingException = var6;
            }
         }
      }
   }

   @Override
   public String toString() {
      if (this.decodedStringCache == null) {
         this.decodeString();
      }

      return this.decodedStringCache;
   }

   static void appendJsonString(StringBuilder sb, String string) {
      sb.append("\"");

      for (int i = 0; i < string.length(); i++) {
         char ch = string.charAt(i);
         if (ch < ' ') {
            switch (ch) {
               case '\b':
                  sb.append("\\b");
                  break;
               case '\t':
                  sb.append("\\t");
                  break;
               case '\n':
                  sb.append("\\n");
                  break;
               case '\u000b':
               default:
                  escapeChar(sb, ch);
                  break;
               case '\f':
                  sb.append("\\f");
                  break;
               case '\r':
                  sb.append("\\r");
            }
         } else if (ch <= 127) {
            switch (ch) {
               case '"':
                  sb.append("\\\"");
                  break;
               case '\\':
                  sb.append("\\\\");
                  break;
               default:
                  sb.append(ch);
            }
         } else if (ch >= '\ud800' && ch <= '\udfff') {
            escapeChar(sb, ch);
         } else {
            sb.append(ch);
         }
      }

      sb.append("\"");
   }

   private static void escapeChar(StringBuilder sb, int ch) {
      sb.append("\\u");
      sb.append(HEX_TABLE[ch >> 12 & 15]);
      sb.append(HEX_TABLE[ch >> 8 & 15]);
      sb.append(HEX_TABLE[ch >> 4 & 15]);
      sb.append(HEX_TABLE[ch & 15]);
   }
}
