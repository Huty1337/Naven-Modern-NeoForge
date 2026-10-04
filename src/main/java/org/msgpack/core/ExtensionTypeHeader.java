package org.msgpack.core;

public class ExtensionTypeHeader {
   private final byte type;
   private final int length;

   public ExtensionTypeHeader(byte type, int length) {
      Preconditions.checkArgument(length >= 0, "length must be >= 0");
      this.type = type;
      this.length = length;
   }

   public static byte checkedCastToByte(int code) {
      Preconditions.checkArgument(-128 <= code && code <= 127, "Extension type code must be within the range of byte");
      return (byte)code;
   }

   public byte getType() {
      return this.type;
   }

   public boolean isTimestampType() {
      return this.type == -1;
   }

   public int getLength() {
      return this.length;
   }

   @Override
   public int hashCode() {
      return (this.type + 31) * 31 + this.length;
   }

   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof ExtensionTypeHeader other) ? false : this.type == other.type && this.length == other.length;
   }

   @Override
   public String toString() {
      return String.format("ExtensionTypeHeader(type:%d, length:%,d)", this.type, this.length);
   }
}
