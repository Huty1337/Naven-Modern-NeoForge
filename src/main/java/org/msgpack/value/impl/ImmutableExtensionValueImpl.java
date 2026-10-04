package org.msgpack.value.impl;

import java.io.IOException;
import java.util.Arrays;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ExtensionValue;
import org.msgpack.value.ImmutableExtensionValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableExtensionValueImpl extends AbstractImmutableValue implements ImmutableExtensionValue {
   private final byte type;
   private final byte[] data;

   public ImmutableExtensionValueImpl(byte type, byte[] data) {
      this.type = type;
      this.data = data;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.EXTENSION;
   }

   public ImmutableExtensionValue immutableValue() {
      return this;
   }

   @Override
   public ImmutableExtensionValue asExtensionValue() {
      return this;
   }

   @Override
   public byte getType() {
      return this.type;
   }

   @Override
   public byte[] getData() {
      return this.data;
   }

   @Override
   public void writeTo(MessagePacker packer) throws IOException {
      packer.packExtensionTypeHeader(this.type, this.data.length);
      packer.writePayload(this.data);
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Value v)) {
         return false;
      } else if (!v.isExtensionValue()) {
         return false;
      } else {
         ExtensionValue ev = v.asExtensionValue();
         return this.type == ev.getType() && Arrays.equals(this.data, ev.getData());
      }
   }

   @Override
   public int hashCode() {
      int hash = 31 + this.type;

      for (byte e : this.data) {
         hash = 31 * hash + e;
      }

      return hash;
   }

   @Override
   public String toJson() {
      StringBuilder sb = new StringBuilder();
      sb.append('[');
      sb.append(Byte.toString(this.type));
      sb.append(",\"");

      for (byte e : this.data) {
         sb.append(Integer.toString(e, 16));
      }

      sb.append("\"]");
      return sb.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('(');
      sb.append(Byte.toString(this.type));
      sb.append(",0x");

      for (byte e : this.data) {
         sb.append(Integer.toString(e, 16));
      }

      sb.append(")");
      return sb.toString();
   }
}
