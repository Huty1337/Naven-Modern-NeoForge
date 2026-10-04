package org.msgpack.value.impl;

import java.io.IOException;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ImmutableNilValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableNilValueImpl extends AbstractImmutableValue implements ImmutableNilValue {
   private static ImmutableNilValue instance = new ImmutableNilValueImpl();

   public static ImmutableNilValue get() {
      return instance;
   }

   private ImmutableNilValueImpl() {
   }

   @Override
   public ValueType getValueType() {
      return ValueType.NIL;
   }

   public ImmutableNilValue immutableValue() {
      return this;
   }

   @Override
   public ImmutableNilValue asNilValue() {
      return this;
   }

   @Override
   public void writeTo(MessagePacker pk) throws IOException {
      pk.packNil();
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else {
         return !(o instanceof Value) ? false : ((Value)o).isNilValue();
      }
   }

   @Override
   public int hashCode() {
      return 0;
   }

   @Override
   public String toString() {
      return this.toJson();
   }

   @Override
   public String toJson() {
      return "null";
   }
}
