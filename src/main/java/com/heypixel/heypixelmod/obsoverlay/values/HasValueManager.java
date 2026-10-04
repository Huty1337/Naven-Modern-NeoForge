package com.heypixel.heypixelmod.obsoverlay.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HasValueManager {
   private final List<HasValue> hasValues = new ArrayList<>();
   private final Map<String, HasValue> nameMap = new HashMap<>();

   public void registerHasValue(HasValue hasValue) {
      this.hasValues.add(hasValue);
      this.nameMap.put(hasValue.getName().toLowerCase(), hasValue);
   }

   public HasValue getHasValue(String name) {
      return this.nameMap.get(name.toLowerCase());
   }

   public List<HasValue> getHasValues() {
      return this.hasValues;
   }
}
