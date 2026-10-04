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

public class ImmutableBigIntegerValueImpl extends AbstractImmutableValue implements ImmutableIntegerValue {
   private final BigInteger value;
   private static final BigInteger BYTE_MIN = BigInteger.valueOf(-128L);
   private static final BigInteger BYTE_MAX = BigInteger.valueOf(127L);
   private static final BigInteger SHORT_MIN = BigInteger.valueOf(-32768L);
   private static final BigInteger SHORT_MAX = BigInteger.valueOf(32767L);
   private static final BigInteger INT_MIN = BigInteger.valueOf(-2147483648L);
   private static final BigInteger INT_MAX = BigInteger.valueOf(2147483647L);
   private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
   private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

   public static MessageFormat mostSuccinctMessageFormat(IntegerValue v) {
      if (v.isInByteRange()) {
         return MessageFormat.INT8;
      } else if (v.isInShortRange()) {
         return MessageFormat.INT16;
      } else if (v.isInIntRange()) {
         return MessageFormat.INT32;
      } else {
         return v.isInLongRange() ? MessageFormat.INT64 : MessageFormat.UINT64;
      }
   }

   public ImmutableBigIntegerValueImpl(BigInteger value) {
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
      return this.value.byteValue();
   }

   @Override
   public short toShort() {
      return this.value.shortValue();
   }

   @Override
   public int toInt() {
      return this.value.intValue();
   }

   @Override
   public long toLong() {
      return this.value.longValue();
   }

   @Override
   public BigInteger toBigInteger() {
      return this.value;
   }

   @Override
   public float toFloat() {
      return this.value.floatValue();
   }

   @Override
   public double toDouble() {
      return this.value.doubleValue();
   }

   @Override
   public boolean isInByteRange() {
      return 0 <= this.value.compareTo(BYTE_MIN) && this.value.compareTo(BYTE_MAX) <= 0;
   }

   @Override
   public boolean isInShortRange() {
      return 0 <= this.value.compareTo(SHORT_MIN) && this.value.compareTo(SHORT_MAX) <= 0;
   }

   @Override
   public boolean isInIntRange() {
      return 0 <= this.value.compareTo(INT_MIN) && this.value.compareTo(INT_MAX) <= 0;
   }

   @Override
   public boolean isInLongRange() {
      return 0 <= this.value.compareTo(LONG_MIN) && this.value.compareTo(LONG_MAX) <= 0;
   }

   @Override
   public MessageFormat mostSuccinctMessageFormat() {
      return mostSuccinctMessageFormat(this);
   }

   @Override
   public byte asByte() {
      if (!this.isInByteRange()) {
         throw new MessageIntegerOverflowException(this.value);
      } else {
         return this.value.byteValue();
      }
   }

   @Override
   public short asShort() {
      if (!this.isInShortRange()) {
         throw new MessageIntegerOverflowException(this.value);
      } else {
         return this.value.shortValue();
      }
   }

   @Override
   public int asInt() {
      if (!this.isInIntRange()) {
         throw new MessageIntegerOverflowException(this.value);
      } else {
         return this.value.intValue();
      }
   }

   @Override
   public long asLong() {
      if (!this.isInLongRange()) {
         throw new MessageIntegerOverflowException(this.value);
      } else {
         return this.value.longValue();
      }
   }

   @Override
   public BigInteger asBigInteger() {
      return this.value;
   }

   @Override
   public void writeTo(MessagePacker pk) throws IOException {
      pk.packBigInteger(this.value);
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
         return this.value.equals(iv.toBigInteger());
      }
   }

   @Override
   public int hashCode() {
      if (INT_MIN.compareTo(this.value) <= 0 && this.value.compareTo(INT_MAX) <= 0) {
         return (int)this.value.longValue();
      } else if (LONG_MIN.compareTo(this.value) <= 0 && this.value.compareTo(LONG_MAX) <= 0) {
         long v = this.value.longValue();
         return (int)(v ^ v >>> 32);
      } else {
         return this.value.hashCode();
      }
   }

   @Override
   public String toJson() {
      return this.value.toString();
   }

   @Override
   public String toString() {
      return this.toJson();
   }
}
