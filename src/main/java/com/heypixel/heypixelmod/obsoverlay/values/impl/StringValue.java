package com.heypixel.heypixelmod.obsoverlay.values.impl;

import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringValue extends Value {
   private final String defaultValue;
   private final Consumer<Value> update;
   private String currentValue;

   public StringValue(HasValue key, String name, String defaultValue, Consumer<Value> update, Supplier<Boolean> visibility) {
      super(key, name, visibility);
      this.update = update;
      this.defaultValue = defaultValue;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.STRING;
   }

   @Override
   public StringValue getStringValue() {
      return this;
   }

   public void setCurrentValue(String currentValue) {
      this.currentValue = currentValue;
      if (this.update != null) {
         this.update.accept(this);
      }
   }

   public String getDefaultValue() {
      return this.defaultValue;
   }

   public Consumer<Value> getUpdate() {
      return this.update;
   }

   public String getCurrentValue() {
      return this.currentValue;
   }
}
