package org.msgpack.value.impl;

import java.io.IOException;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.ArrayValue;
import org.msgpack.value.ImmutableArrayValue;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

public class ImmutableArrayValueImpl extends AbstractImmutableValue implements ImmutableArrayValue {
   private static final ImmutableArrayValueImpl EMPTY = new ImmutableArrayValueImpl(new Value[0]);
   private final Value[] array;

   public static ImmutableArrayValue empty() {
      return EMPTY;
   }

   public ImmutableArrayValueImpl(Value[] array) {
      this.array = array;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.ARRAY;
   }

   public ImmutableArrayValue immutableValue() {
      return this;
   }

   @Override
   public ImmutableArrayValue asArrayValue() {
      return this;
   }

   @Override
   public int size() {
      return this.array.length;
   }

   @Override
   public Value get(int index) {
      return this.array[index];
   }

   @Override
   public Value getOrNilValue(int index) {
      return (Value)(index < this.array.length && index >= 0 ? this.array[index] : ImmutableNilValueImpl.get());
   }

   @Override
   public Iterator<Value> iterator() {
      return new ImmutableArrayValueImpl.Ite(this.array);
   }

   @Override
   public List<Value> list() {
      return new ImmutableArrayValueImpl.ImmutableArrayValueList(this.array);
   }

   @Override
   public void writeTo(MessagePacker pk) throws IOException {
      pk.packArrayHeader(this.array.length);

      for (int i = 0; i < this.array.length; i++) {
         this.array[i].writeTo(pk);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Value v)) {
         return false;
      } else if (v instanceof ImmutableArrayValueImpl oa) {
         return Arrays.equals((Object[])this.array, (Object[])oa.array);
      } else if (!v.isArrayValue()) {
         return false;
      } else {
         ArrayValue av = v.asArrayValue();
         if (this.size() != av.size()) {
            return false;
         } else {
            Iterator<Value> oi = av.iterator();

            for (int i = 0; i < this.array.length; i++) {
               if (!oi.hasNext() || !this.array[i].equals(oi.next())) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   @Override
   public int hashCode() {
      int h = 1;

      for (int i = 0; i < this.array.length; i++) {
         Value obj = this.array[i];
         h = 31 * h + obj.hashCode();
      }

      return h;
   }

   @Override
   public String toJson() {
      if (this.array.length == 0) {
         return "[]";
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append("[");
         sb.append(this.array[0].toJson());

         for (int i = 1; i < this.array.length; i++) {
            sb.append(",");
            sb.append(this.array[i].toJson());
         }

         sb.append("]");
         return sb.toString();
      }
   }

   @Override
   public String toString() {
      if (this.array.length == 0) {
         return "[]";
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append("[");
         appendString(sb, this.array[0]);

         for (int i = 1; i < this.array.length; i++) {
            sb.append(",");
            appendString(sb, this.array[i]);
         }

         sb.append("]");
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

   private static class ImmutableArrayValueList extends AbstractList<Value> {
      private final Value[] array;

      public ImmutableArrayValueList(Value[] array) {
         this.array = array;
      }

      public Value get(int index) {
         return this.array[index];
      }

      @Override
      public int size() {
         return this.array.length;
      }
   }

   private static class Ite implements Iterator<Value> {
      private final Value[] array;
      private int index;

      public Ite(Value[] array) {
         this.array = array;
         this.index = 0;
      }

      @Override
      public boolean hasNext() {
         return this.index != this.array.length;
      }

      public Value next() {
         int i = this.index;
         if (i >= this.array.length) {
            throw new NoSuchElementException();
         } else {
            this.index = i + 1;
            return this.array[i];
         }
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
}
