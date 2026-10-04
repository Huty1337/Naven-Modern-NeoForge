package org.msgpack.value;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import org.msgpack.value.impl.ImmutableArrayValueImpl;
import org.msgpack.value.impl.ImmutableBigIntegerValueImpl;
import org.msgpack.value.impl.ImmutableBinaryValueImpl;
import org.msgpack.value.impl.ImmutableBooleanValueImpl;
import org.msgpack.value.impl.ImmutableDoubleValueImpl;
import org.msgpack.value.impl.ImmutableExtensionValueImpl;
import org.msgpack.value.impl.ImmutableLongValueImpl;
import org.msgpack.value.impl.ImmutableMapValueImpl;
import org.msgpack.value.impl.ImmutableNilValueImpl;
import org.msgpack.value.impl.ImmutableStringValueImpl;
import org.msgpack.value.impl.ImmutableTimestampValueImpl;

public final class ValueFactory {
   private ValueFactory() {
   }

   public static ImmutableNilValue newNil() {
      return ImmutableNilValueImpl.get();
   }

   public static ImmutableBooleanValue newBoolean(boolean v) {
      return v ? ImmutableBooleanValueImpl.TRUE : ImmutableBooleanValueImpl.FALSE;
   }

   public static ImmutableIntegerValue newInteger(byte v) {
      return new ImmutableLongValueImpl((long)v);
   }

   public static ImmutableIntegerValue newInteger(short v) {
      return new ImmutableLongValueImpl((long)v);
   }

   public static ImmutableIntegerValue newInteger(int v) {
      return new ImmutableLongValueImpl((long)v);
   }

   public static ImmutableIntegerValue newInteger(long v) {
      return new ImmutableLongValueImpl(v);
   }

   public static ImmutableIntegerValue newInteger(BigInteger v) {
      return new ImmutableBigIntegerValueImpl(v);
   }

   public static ImmutableFloatValue newFloat(float v) {
      return new ImmutableDoubleValueImpl((double)v);
   }

   public static ImmutableFloatValue newFloat(double v) {
      return new ImmutableDoubleValueImpl(v);
   }

   public static ImmutableBinaryValue newBinary(byte[] b) {
      return newBinary(b, false);
   }

   public static ImmutableBinaryValue newBinary(byte[] b, boolean omitCopy) {
      return omitCopy ? new ImmutableBinaryValueImpl(b) : new ImmutableBinaryValueImpl(Arrays.copyOf(b, b.length));
   }

   public static ImmutableBinaryValue newBinary(byte[] b, int off, int len) {
      return newBinary(b, off, len, false);
   }

   public static ImmutableBinaryValue newBinary(byte[] b, int off, int len, boolean omitCopy) {
      return omitCopy && off == 0 && len == b.length ? new ImmutableBinaryValueImpl(b) : new ImmutableBinaryValueImpl(Arrays.copyOfRange(b, off, len));
   }

   public static ImmutableStringValue newString(String s) {
      return new ImmutableStringValueImpl(s);
   }

   public static ImmutableStringValue newString(byte[] b) {
      return new ImmutableStringValueImpl(b);
   }

   public static ImmutableStringValue newString(byte[] b, boolean omitCopy) {
      return omitCopy ? new ImmutableStringValueImpl(b) : new ImmutableStringValueImpl(Arrays.copyOf(b, b.length));
   }

   public static ImmutableStringValue newString(byte[] b, int off, int len) {
      return newString(b, off, len, false);
   }

   public static ImmutableStringValue newString(byte[] b, int off, int len, boolean omitCopy) {
      return omitCopy && off == 0 && len == b.length ? new ImmutableStringValueImpl(b) : new ImmutableStringValueImpl(Arrays.copyOfRange(b, off, len));
   }

   public static ImmutableArrayValue newArray(List<? extends Value> list) {
      if (list.isEmpty()) {
         return ImmutableArrayValueImpl.empty();
      } else {
         Value[] array = list.toArray(new Value[list.size()]);
         return new ImmutableArrayValueImpl(array);
      }
   }

   public static ImmutableArrayValue newArray(Value... array) {
      return (ImmutableArrayValue)(array.length == 0 ? ImmutableArrayValueImpl.empty() : new ImmutableArrayValueImpl(Arrays.copyOf(array, array.length)));
   }

   public static ImmutableArrayValue newArray(Value[] array, boolean omitCopy) {
      if (array.length == 0) {
         return ImmutableArrayValueImpl.empty();
      } else {
         return omitCopy ? new ImmutableArrayValueImpl(array) : new ImmutableArrayValueImpl(Arrays.copyOf(array, array.length));
      }
   }

   public static ImmutableArrayValue emptyArray() {
      return ImmutableArrayValueImpl.empty();
   }

   public static <K extends Value, V extends Value> ImmutableMapValue newMap(Map<K, V> map) {
      Value[] kvs = new Value[map.size() * 2];
      int index = 0;

      for (Entry<K, V> pair : map.entrySet()) {
         kvs[index] = pair.getKey();
         index++;
         kvs[index] = pair.getValue();
         index++;
      }

      return new ImmutableMapValueImpl(kvs);
   }

   public static ImmutableMapValue newMap(Value... kvs) {
      return (ImmutableMapValue)(kvs.length == 0 ? ImmutableMapValueImpl.empty() : new ImmutableMapValueImpl(Arrays.copyOf(kvs, kvs.length)));
   }

   public static ImmutableMapValue newMap(Value[] kvs, boolean omitCopy) {
      if (kvs.length == 0) {
         return ImmutableMapValueImpl.empty();
      } else {
         return omitCopy ? new ImmutableMapValueImpl(kvs) : new ImmutableMapValueImpl(Arrays.copyOf(kvs, kvs.length));
      }
   }

   public static ImmutableMapValue emptyMap() {
      return ImmutableMapValueImpl.empty();
   }

   @SafeVarargs
   public static MapValue newMap(Entry<? extends Value, ? extends Value>... pairs) {
      Value[] kvs = new Value[pairs.length * 2];

      for (int i = 0; i < pairs.length; i++) {
         kvs[i * 2] = pairs[i].getKey();
         kvs[i * 2 + 1] = pairs[i].getValue();
      }

      return newMap(kvs, true);
   }

   public static ValueFactory.MapBuilder newMapBuilder() {
      return new ValueFactory.MapBuilder();
   }

   public static Entry<Value, Value> newMapEntry(Value key, Value value) {
      return new SimpleEntry<>(key, value);
   }

   public static ImmutableExtensionValue newExtension(byte type, byte[] data) {
      return new ImmutableExtensionValueImpl(type, data);
   }

   public static ImmutableTimestampValue newTimestamp(Instant timestamp) {
      return new ImmutableTimestampValueImpl(timestamp);
   }

   public static ImmutableTimestampValue newTimestamp(long millis) {
      return newTimestamp(Instant.ofEpochMilli(millis));
   }

   public static ImmutableTimestampValue newTimestamp(long epochSecond, int nanoAdjustment) {
      return newTimestamp(Instant.ofEpochSecond(epochSecond, (long)nanoAdjustment));
   }

   public static class MapBuilder {
      private final Map<Value, Value> map = new LinkedHashMap<>();

      public MapValue build() {
         return ValueFactory.newMap(this.map);
      }

      public ValueFactory.MapBuilder put(Entry<? extends Value, ? extends Value> pair) {
         this.put(pair.getKey(), pair.getValue());
         return this;
      }

      public ValueFactory.MapBuilder put(Value key, Value value) {
         this.map.put(key, value);
         return this;
      }

      public ValueFactory.MapBuilder putAll(Iterable<? extends Entry<? extends Value, ? extends Value>> entries) {
         for (Entry<? extends Value, ? extends Value> entry : entries) {
            this.put(entry.getKey(), entry.getValue());
         }

         return this;
      }

      public ValueFactory.MapBuilder putAll(Map<? extends Value, ? extends Value> map) {
         for (Entry<? extends Value, ? extends Value> entry : map.entrySet()) {
            this.put(entry);
         }

         return this;
      }
   }
}
