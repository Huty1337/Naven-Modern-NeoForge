package org.msgpack.value.impl;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ImmutableMapValue;
import org.msgpack.value.MapValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableMapValueImpl extends AbstractImmutableValue implements ImmutableMapValue {
   private static final ImmutableMapValueImpl EMPTY = new ImmutableMapValueImpl(new Value[0]);
   private final Value[] kvs;

   public static ImmutableMapValue empty() {
      return EMPTY;
   }

   public ImmutableMapValueImpl(Value[] kvs) {
      this.kvs = kvs;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.MAP;
   }

   public ImmutableMapValue immutableValue() {
      return this;
   }

   @Override
   public ImmutableMapValue asMapValue() {
      return this;
   }

   @Override
   public Value[] getKeyValueArray() {
      return Arrays.copyOf(this.kvs, this.kvs.length);
   }

   @Override
   public int size() {
      return this.kvs.length / 2;
   }

   @Override
   public Set<Value> keySet() {
      return new ImmutableMapValueImpl.KeySet(this.kvs);
   }

   @Override
   public Set<Entry<Value, Value>> entrySet() {
      return new ImmutableMapValueImpl.EntrySet(this.kvs);
   }

   @Override
   public Collection<Value> values() {
      return new ImmutableMapValueImpl.ValueCollection(this.kvs);
   }

   @Override
   public Map<Value, Value> map() {
      return new ImmutableMapValueImpl.ImmutableMapValueMap(this.kvs);
   }

   @Override
   public void writeTo(MessagePacker pk) throws IOException {
      pk.packMapHeader(this.kvs.length / 2);

      for (int i = 0; i < this.kvs.length; i++) {
         this.kvs[i].writeTo(pk);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Value v)) {
         return false;
      } else if (!v.isMapValue()) {
         return false;
      } else {
         MapValue mv = v.asMapValue();
         return this.map().equals(mv.map());
      }
   }

   @Override
   public int hashCode() {
      int h = 0;

      for (int i = 0; i < this.kvs.length; i += 2) {
         h += this.kvs[i].hashCode() ^ this.kvs[i + 1].hashCode();
      }

      return h;
   }

   @Override
   public String toJson() {
      if (this.kvs.length == 0) {
         return "{}";
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append("{");
         appendJsonKey(sb, this.kvs[0]);
         sb.append(":");
         sb.append(this.kvs[1].toJson());

         for (int i = 2; i < this.kvs.length; i += 2) {
            sb.append(",");
            appendJsonKey(sb, this.kvs[i]);
            sb.append(":");
            sb.append(this.kvs[i + 1].toJson());
         }

         sb.append("}");
         return sb.toString();
      }
   }

   private static void appendJsonKey(StringBuilder sb, Value key) {
      if (key.isRawValue()) {
         sb.append(key.toJson());
      } else {
         ImmutableStringValueImpl.appendJsonString(sb, key.toString());
      }
   }

   @Override
   public String toString() {
      if (this.kvs.length == 0) {
         return "{}";
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append("{");
         appendString(sb, this.kvs[0]);
         sb.append(":");
         appendString(sb, this.kvs[1]);

         for (int i = 2; i < this.kvs.length; i += 2) {
            sb.append(",");
            appendString(sb, this.kvs[i]);
            sb.append(":");
            appendString(sb, this.kvs[i + 1]);
         }

         sb.append("}");
         return sb.toString();
      }
   }

   private static void appendString(StringBuilder sb, Value value) {
      if (value.isRawValue()) {
         sb.append(value.toJson());
      } else {
         sb.append(value.toString());
      }
   }

   private static class EntryIterator implements Iterator<Value> {
      private Value[] kvs;
      private int index;

      public EntryIterator(Value[] kvs, int offset) {
         this.kvs = kvs;
         this.index = offset;
      }

      @Override
      public boolean hasNext() {
         return this.index < this.kvs.length;
      }

      public Value next() {
         int i = this.index;
         if (i >= this.kvs.length) {
            throw new NoSuchElementException();
         } else {
            this.index = i + 2;
            return this.kvs[i];
         }
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   private static class EntrySet extends AbstractSet<Entry<Value, Value>> {
      private final Value[] kvs;

      EntrySet(Value[] kvs) {
         this.kvs = kvs;
      }

      @Override
      public int size() {
         return this.kvs.length / 2;
      }

      @Override
      public Iterator<Entry<Value, Value>> iterator() {
         return new ImmutableMapValueImpl.EntrySetIterator(this.kvs);
      }
   }

   private static class EntrySetIterator implements Iterator<Entry<Value, Value>> {
      private final Value[] kvs;
      private int index;

      EntrySetIterator(Value[] kvs) {
         this.kvs = kvs;
         this.index = 0;
      }

      @Override
      public boolean hasNext() {
         return this.index < this.kvs.length;
      }

      public Entry<Value, Value> next() {
         if (this.index >= this.kvs.length) {
            throw new NoSuchElementException();
         } else {
            Value key = this.kvs[this.index];
            Value value = this.kvs[this.index + 1];
            Entry<Value, Value> pair = new SimpleImmutableEntry<>(key, value);
            this.index += 2;
            return pair;
         }
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   private static class ImmutableMapValueMap extends AbstractMap<Value, Value> {
      private final Value[] kvs;

      public ImmutableMapValueMap(Value[] kvs) {
         this.kvs = kvs;
      }

      @Override
      public Set<Entry<Value, Value>> entrySet() {
         return new ImmutableMapValueImpl.EntrySet(this.kvs);
      }
   }

   private static class KeySet extends AbstractSet<Value> {
      private Value[] kvs;

      KeySet(Value[] kvs) {
         this.kvs = kvs;
      }

      @Override
      public int size() {
         return this.kvs.length / 2;
      }

      @Override
      public Iterator<Value> iterator() {
         return new ImmutableMapValueImpl.EntryIterator(this.kvs, 0);
      }
   }

   private static class ValueCollection extends AbstractCollection<Value> {
      private Value[] kvs;

      ValueCollection(Value[] kvs) {
         this.kvs = kvs;
      }

      @Override
      public int size() {
         return this.kvs.length / 2;
      }

      @Override
      public Iterator<Value> iterator() {
         return new ImmutableMapValueImpl.EntryIterator(this.kvs, 1);
      }
   }
}
