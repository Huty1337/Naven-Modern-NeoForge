package org.msgpack.value.impl;

import java.io.IOException;
import java.util.Arrays;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ImmutableBinaryValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableBinaryValueImpl extends AbstractImmutableRawValue implements ImmutableBinaryValue {
   public ImmutableBinaryValueImpl(byte[] data) {
      super(data);
   }

   @Override
   public ValueType getValueType() {
      return ValueType.BINARY;
   }

   public ImmutableBinaryValue immutableValue() {
      return this;
   }

   @Override
   public ImmutableBinaryValue asBinaryValue() {
      return this;
   }

   @Override
   public void writeTo(MessagePacker pk) throws IOException {
      pk.packBinaryHeader(this.data.length);
      pk.writePayload(this.data);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof Value v)) {
         return false;
      } else if (!v.isBinaryValue()) {
         return false;
      } else {
         return v instanceof ImmutableBinaryValueImpl bv ? Arrays.equals(this.data, bv.data) : Arrays.equals(this.data, v.asBinaryValue().asByteArray());
      }
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.data);
   }
}
