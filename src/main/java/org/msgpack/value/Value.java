package org.msgpack.value;

import java.io.IOException;
import org.msgpack.core.MessagePacker;

public interface Value {
   ValueType getValueType();

   ImmutableValue immutableValue();

   boolean isNilValue();

   boolean isBooleanValue();

   boolean isNumberValue();

   boolean isIntegerValue();

   boolean isFloatValue();

   boolean isRawValue();

   boolean isBinaryValue();

   boolean isStringValue();

   boolean isArrayValue();

   boolean isMapValue();

   boolean isExtensionValue();

   boolean isTimestampValue();

   NilValue asNilValue();

   BooleanValue asBooleanValue();

   NumberValue asNumberValue();

   IntegerValue asIntegerValue();

   FloatValue asFloatValue();

   RawValue asRawValue();

   BinaryValue asBinaryValue();

   StringValue asStringValue();

   ArrayValue asArrayValue();

   MapValue asMapValue();

   ExtensionValue asExtensionValue();

   TimestampValue asTimestampValue();

   void writeTo(MessagePacker var1) throws IOException;

   @Override
   boolean equals(Object var1);

   String toJson();
}
