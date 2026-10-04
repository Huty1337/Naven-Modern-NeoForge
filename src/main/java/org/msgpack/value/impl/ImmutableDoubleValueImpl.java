package org.msgpack.value.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ImmutableFloatValue;
import org.msgpack.value.ImmutableNumberValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableDoubleValueImpl extends AbstractImmutableValue implements ImmutableFloatValue {
   private final double value;

   public ImmutableDoubleValueImpl(double value) {
      this.value = value;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.FLOAT;
   }

   public ImmutableDoubleValueImpl immutableValue() {
      return this;
   }

   @Override
   public ImmutableNumberValue asNumberValue() {
      return this;
   }

   @Override
   public ImmutableFloatValue asFloatValue() {
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
      return (long)this.value;
   }

   @Override
   public BigInteger toBigInteger() {
      return new BigDecimal(this.value).toBigInteger();
   }

   @Override
   public float toFloat() {
      return (float)this.value;
   }

   @Override
   public double toDouble() {
      return this.value;
   }

   @Override
   public void writeTo(MessagePacker pk) throws IOException {
      pk.packDouble(this.value);
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Value v)) {
         return false;
      } else {
         return !v.isFloatValue() ? false : this.value == v.asFloatValue().toDouble();
      }
   }

   @Override
   public int hashCode() {
      long v = Double.doubleToLongBits(this.value);
      return (int)(v ^ v >>> 32);
   }

   @Override
   public String toJson() {
      return !Double.isNaN(this.value) && !Double.isInfinite(this.value) ? Double.toString(this.value) : "null";
   }

   @Override
   public String toString() {
      return Double.toString(this.value);
   }
}
