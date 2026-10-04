package org.msgpack.value.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.buffer.MessageBuffer;
import org.msgpack.value.ExtensionValue;
import org.msgpack.value.ImmutableExtensionValue;
import org.msgpack.value.ImmutableTimestampValue;
import org.msgpack.value.TimestampValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableTimestampValueImpl extends AbstractImmutableValue implements ImmutableExtensionValue, ImmutableTimestampValue {
   private final Instant instant;
   private byte[] data;

   public ImmutableTimestampValueImpl(Instant timestamp) {
      this.instant = timestamp;
   }

   @Override
   public boolean isTimestampValue() {
      return true;
   }

   @Override
   public byte getType() {
      return -1;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.EXTENSION;
   }

   public ImmutableTimestampValue immutableValue() {
      return this;
   }

   @Override
   public ImmutableExtensionValue asExtensionValue() {
      return this;
   }

   @Override
   public ImmutableTimestampValue asTimestampValue() {
      return this;
   }

   @Override
   public byte[] getData() {
      if (this.data == null) {
         long sec = this.getEpochSecond();
         int nsec = this.getNano();
         byte[] bytes;
         if (sec >>> 34 == 0L) {
            long data64 = (long)nsec << 34 | sec;
            if ((data64 & -4294967296L) == 0L) {
               bytes = new byte[4];
               MessageBuffer.wrap(bytes).putInt(0, (int)sec);
            } else {
               bytes = new byte[8];
               MessageBuffer.wrap(bytes).putLong(0, data64);
            }
         } else {
            bytes = new byte[12];
            MessageBuffer buffer = MessageBuffer.wrap(bytes);
            buffer.putInt(0, nsec);
            buffer.putLong(4, sec);
         }

         this.data = bytes;
      }

      return this.data;
   }

   @Override
   public long getEpochSecond() {
      return this.instant.getEpochSecond();
   }

   @Override
   public int getNano() {
      return this.instant.getNano();
   }

   @Override
   public long toEpochMillis() {
      return this.instant.toEpochMilli();
   }

   @Override
   public Instant toInstant() {
      return this.instant;
   }

   @Override
   public void writeTo(MessagePacker packer) throws IOException {
      packer.packTimestamp(this.instant);
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
         return ev instanceof TimestampValue tv ? this.instant.equals(tv.toInstant()) : -1 == ev.getType() && Arrays.equals(this.getData(), ev.getData());
      }
   }

   @Override
   public int hashCode() {
      int hash = -1;
      hash *= 31;
      return this.instant.hashCode();
   }

   @Override
   public String toJson() {
      return "\"" + this.toInstant().toString() + "\"";
   }

   @Override
   public String toString() {
      return this.toInstant().toString();
   }
}
