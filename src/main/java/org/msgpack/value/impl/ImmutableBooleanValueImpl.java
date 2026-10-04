package org.msgpack.value.impl;

import java.io.IOException;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ImmutableBooleanValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableBooleanValueImpl extends AbstractImmutableValue implements ImmutableBooleanValue {
   public static final ImmutableBooleanValue TRUE = new ImmutableBooleanValueImpl(true);
   public static final ImmutableBooleanValue FALSE = new ImmutableBooleanValueImpl(false);
   private final boolean value;

   private ImmutableBooleanValueImpl(boolean value) {
      this.value = value;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.BOOLEAN;
   }

   @Override
   public ImmutableBooleanValue asBooleanValue() {
      return this;
   }

   public ImmutableBooleanValue immutableValue() {
      return this;
   }

   @Override
   public boolean getBoolean() {
      return this.value;
   }

   @Override
   public void writeTo(MessagePacker packer) throws IOException {
      packer.packBoolean(this.value);
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Value v)) {
         return false;
      } else {
         return !v.isBooleanValue() ? false : this.value == v.asBooleanValue().getBoolean();
      }
   }

   @Override
   public int hashCode() {
      return this.value ? 1231 : 1237;
   }

   @Override
   public String toJson() {
      return Boolean.toString(this.value);
   }

   @Override
   public String toString() {
      return this.toJson();
   }
}
