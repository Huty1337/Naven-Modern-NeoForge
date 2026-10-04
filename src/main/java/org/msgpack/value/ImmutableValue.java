package org.msgpack.value;

public interface ImmutableValue extends Value {
   ImmutableNilValue asNilValue();

   ImmutableBooleanValue asBooleanValue();

   ImmutableIntegerValue asIntegerValue();

   ImmutableFloatValue asFloatValue();

   ImmutableArrayValue asArrayValue();

   ImmutableMapValue asMapValue();

   ImmutableRawValue asRawValue();

   ImmutableBinaryValue asBinaryValue();

   ImmutableStringValue asStringValue();

   ImmutableTimestampValue asTimestampValue();
}
