package com.heypixel.heypixelmod.obsoverlay.values.impl;

import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModeValue extends Value {
   private final String[] values;
   private final Consumer<Value> update;
   private int currentValue;

   public ModeValue(HasValue key, String name, String[] values, int defaultValue, Consumer<Value> update, Supplier<Boolean> visibility) {
      super(key, name, visibility);
      this.update = update;
      this.values = values;
      this.currentValue = defaultValue;
   }

   public boolean isCurrentMode(String mode) {
      return this.getCurrentMode().equalsIgnoreCase(mode);
   }

   @Override
   public ValueType getValueType() {
      return ValueType.MODE;
   }

   @Override
   public ModeValue getModeValue() {
      return this;
   }

   public String getCurrentMode() {
      return this.values[this.currentValue];
   }

   public void setCurrentValue(int currentValue) {
      this.currentValue = currentValue;
      if (this.update != null) {
         this.update.accept(this);
      }
   }

   public String[] getValues() {
      return this.values;
   }

   public Consumer<Value> getUpdate() {
      return this.update;
   }

   public int getCurrentValue() {
      return this.currentValue;
   }
}
