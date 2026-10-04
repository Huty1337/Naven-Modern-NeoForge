package org.msgpack.value;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public interface MapValue extends Value {
   int size();

   Set<Value> keySet();

   Set<Entry<Value, Value>> entrySet();

   Collection<Value> values();

   Map<Value, Value> map();

   Value[] getKeyValueArray();
}
