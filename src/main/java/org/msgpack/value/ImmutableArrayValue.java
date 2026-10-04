package org.msgpack.value;

import java.util.Iterator;
import java.util.List;

public interface ImmutableArrayValue extends ArrayValue, ImmutableValue {
   @Override
   Iterator<Value> iterator();

   @Override
   List<Value> list();
}
