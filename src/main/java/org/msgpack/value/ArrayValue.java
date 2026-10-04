package org.msgpack.value;

import java.util.Iterator;
import java.util.List;

public interface ArrayValue extends Value, Iterable<Value> {
   int size();

   Value get(int var1);

   Value getOrNilValue(int var1);

   @Override
   Iterator<Value> iterator();

   List<Value> list();
}
