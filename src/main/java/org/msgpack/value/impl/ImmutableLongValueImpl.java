package org.msgpack.value.impl;

import java.io.IOException;
import java.math.BigInteger;
import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessageIntegerOverflowException;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ImmutableIntegerValue;
import org.msgpack.value.ImmutableNumberValue;
import org.msgpack.value.IntegerValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableLongValueImpl extends AbstractImmutableValue implements ImmutableIntegerValue {
   private final long value;
   private static final long BYTE_MIN = -128L;
   private static final long BYTE_MAX = 127L;
   private static final long SHORT_MIN = -32768L;
   private static final long SHORT_MAX = 32767L;
   private static final long INT_MIN = -2147483648L;
   private static final long INT_MAX = 2147483647L;

   public ImmutableLongValueImpl(long value) {
      this.value = value;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.INTEGER;
   }

   public ImmutableIntegerValue immutableValue() {
      return this;
   }

   @Override
   public ImmutableNumberValue asNumberValue() {
      return this;
   }

   @Override
   public ImmutableIntegerValue asIntegerValue() {
      return this;
   }

   @Override
   public byte toByte() {
      return (byte)((int)this.value);
   }

   @Override
   public short toShort() {
      return (short)((int)this.value);
   }

   @Override
   public int toInt() {
      return (int)this.value;
   }

   @Override
   public long toLong() {
      return this.value;
   }

   @Override
   public BigInteger toBigInteger() {
      return BigInteger.valueOf(this.value);
   }

   @Override
   public float toFloat() {
      return (float)this.value;
   }

   @Override
   public double toDouble() {
      return (double)this.value;
   }

   @Override
   public boolean isInByteRange() {
      return -128L <= this.value && this.value <= 127L;
   }

   @Override
   public boolean isInShortRange() {
      return -32768L <= this.value && this.value <= 32767L;
   }

   @Override
   public boolean isInIntRange() {
      return -2147483648L <= this.value && this.value <= 2147483647L;
   }

   @Override
   public boolean isInLongRange() {
      return true;
   }

   @Override
   public MessageFormat mostSuccinctMessageFormat() {
      return ImmutableBigIntegerValueImpl.mostSuccinctMessageFormat(this);
   }

   @Override
   public byte asByte() {
      if (!this.isInByteRange()) {
         throw new MessageIntegerOverflowException(this.value);
      } else {
         return (byte)((int)this.value);
      }
   }

   @Override
   public short asShort() {
      if (!this.isInShortRange()) {
         throw new MessageIntegerOverflowException(this.value);
      } else {
         return (short)((int)this.value);
      }
   }

   @Override
   public int asInt() {
      if (!this.isInIntRange()) {
         throw new MessageIntegerOverflowException(this.value);
      } else {
         return (int)this.value;
      }
   }

   @Override
   public long asLong() {
      return this.value;
   }

   @Override
   public BigInteger asBigInteger() {
      return BigInteger.valueOf(this.value);
   }

   @Override
   public void writeTo(MessagePacker pk) throws IOException {
      pk.packLong(this.value);
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Value v)) {
         return false;
      } else if (!v.isIntegerValue()) {
         return false;
      } else {
         IntegerValue iv = v.asIntegerValue();
         return !iv.isInLongRange() ? false : this.value == iv.toLong();
      }
   }

   @Override
   public int hashCode() {
      return -2147483648L <= this.value && this.value <= 2147483647L ? (int)this.value : (int)(this.value ^ this.value >>> 32);
   }

   @Override
   public String toJson() {
      return Long.toString(this.value);
   }

   @Override
   public String toString() {
      return this.toJson();
   }
}
