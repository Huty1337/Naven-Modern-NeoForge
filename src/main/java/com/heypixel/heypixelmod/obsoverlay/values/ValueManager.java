package com.heypixel.heypixelmod.obsoverlay.values;

import com.heypixel.heypixelmod.obsoverlay.exceptions.NoSuchValueException;
import java.util.ArrayList;
import java.util.List;

public class ValueManager {
   private final List<Value> values = new ArrayList<>();

   public void addValue(Value value) {
      this.values.add(value);
   }

   public List<Value> getValuesByHasValue(HasValue key) {
      List<Value> values = new ArrayList<>();

      for (Value value : this.values) {
         if (value.getKey() == key) {
            values.add(value);
         }
      }

      return values;
   }

   public Value getValue(HasValue key, String name) {
      for (Value value : this.values) {
         if (value.getKey() == key && value.getName().equals(name)) {
            return value;
         }
      }

      throw new NoSuchValueException();
   }

   public List<Value> getValues() {
      return this.values;
   }
}
