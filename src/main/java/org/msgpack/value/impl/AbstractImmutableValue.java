package org.msgpack.value.impl;

import org.msgpack.core.MessageTypeCastException;
import org.msgpack.value.ImmutableArrayValue;
import org.msgpack.value.ImmutableBinaryValue;
import org.msgpack.value.ImmutableBooleanValue;
import org.msgpack.value.ImmutableExtensionValue;
import org.msgpack.value.ImmutableFloatValue;
import org.msgpack.value.ImmutableIntegerValue;
import org.msgpack.value.ImmutableMapValue;
import org.msgpack.value.ImmutableNilValue;
import org.msgpack.value.ImmutableNumberValue;
import org.msgpack.value.ImmutableRawValue;
import org.msgpack.value.ImmutableStringValue;
import org.msgpack.value.ImmutableTimestampValue;
import org.msgpack.value.ImmutableValue;

abstract class AbstractImmutableValue implements ImmutableValue {
   @Override
   public boolean isNilValue() {
      return this.getValueType().isNilType();
   }

   @Override
   public boolean isBooleanValue() {
      return this.getValueType().isBooleanType();
   }

   @Override
   public boolean isNumberValue() {
      return this.getValueType().isNumberType();
   }

   @Override
   public boolean isIntegerValue() {
      return this.getValueType().isIntegerType();
   }

   @Override
   public boolean isFloatValue() {
      return this.getValueType().isFloatType();
   }

   @Override
   public boolean isRawValue() {
      return this.getValueType().isRawType();
   }

   @Override
   public boolean isBinaryValue() {
      return this.getValueType().isBinaryType();
   }

   @Override
   public boolean isStringValue() {
      return this.getValueType().isStringType();
   }

   @Override
   public boolean isArrayValue() {
      return this.getValueType().isArrayType();
   }

   @Override
   public boolean isMapValue() {
      return this.getValueType().isMapType();
   }

   @Override
   public boolean isExtensionValue() {
      return this.getValueType().isExtensionType();
   }

   @Override
   public boolean isTimestampValue() {
      return false;
   }

   @Override
   public ImmutableNilValue asNilValue() {
      throw new MessageTypeCastException();
   }

   @Override
   public ImmutableBooleanValue asBooleanValue() {
      throw new MessageTypeCastException();
   }

   public ImmutableNumberValue asNumberValue() {
      throw new MessageTypeCastException();
   }

   @Override
   public ImmutableIntegerValue asIntegerValue() {
      throw new MessageTypeCastException();
   }

   @Override
   public ImmutableFloatValue asFloatValue() {
      throw new MessageTypeCastException();
   }

   @Override
   public ImmutableRawValue asRawValue() {
      throw new MessageTypeCastException();
   }

   @Override
   public ImmutableBinaryValue asBinaryValue() {
      throw new MessageTypeCastException();
   }

   @Override
   public ImmutableStringValue asStringValue() {
      throw new MessageTypeCastException();
   }

   @Override
   public ImmutableArrayValue asArrayValue() {
      throw new MessageTypeCastException();
   }

   @Override
   public ImmutableMapValue asMapValue() {
      throw new MessageTypeCastException();
   }

   public ImmutableExtensionValue asExtensionValue() {
      throw new MessageTypeCastException();
   }

   @Override
   public ImmutableTimestampValue asTimestampValue() {
      throw new MessageTypeCastException();
   }
}
