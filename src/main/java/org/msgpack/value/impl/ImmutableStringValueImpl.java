package org.msgpack.value.impl;

import java.io.IOException;
import java.util.Arrays;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ImmutableStringValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableStringValueImpl extends AbstractImmutableRawValue implements ImmutableStringValue {
   public ImmutableStringValueImpl(byte[] data) {
      super(data);
   }

   public ImmutableStringValueImpl(String string) {
      super(string);
   }

   @Override
   public ValueType getValueType() {
      return ValueType.STRING;
   }

   public ImmutableStringValue immutableValue() {
      return this;
   }

   @Override
   public ImmutableStringValue asStringValue() {
      return this;
   }

   @Override
   public void writeTo(MessagePacker pk) throws IOException {
      pk.packRawStringHeader(this.data.length);
      pk.writePayload(this.data);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof Value v)) {
         return false;
      } else if (!v.isStringValue()) {
         return false;
      } else {
         return v instanceof ImmutableStringValueImpl bv ? Arrays.equals(this.data, bv.data) : Arrays.equals(this.data, v.asStringValue().asByteArray());
      }
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }
}
