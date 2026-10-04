package com.heypixel.heypixelmod.obsoverlay.values.impl;

import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.Value;
import com.heypixel.heypixelmod.obsoverlay.values.ValueType;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FloatValue extends Value {
   private final float defaultValue;
   private final float minValue;
   private final float maxValue;
   private final float step;
   private final Consumer<Value> update;
   private float currentValue;

   public FloatValue(
      HasValue key, String name, float defaultValue, float minValue, float maxValue, float step, Consumer<Value> update, Supplier<Boolean> visibility
   ) {
      super(key, name, visibility);
      this.update = update;
      this.currentValue = this.defaultValue = defaultValue;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.step = step;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.FLOAT;
   }

   @Override
   public FloatValue getFloatValue() {
      return this;
   }

   public void setCurrentValue(float currentValue) {
      this.currentValue = MathUtils.clampValue(currentValue, this.minValue, this.maxValue);
      if (this.update != null) {
         this.update.accept(this);
      }
   }

   public float getDefaultValue() {
      return this.defaultValue;
   }

   public float getMinValue() {
      return this.minValue;
   }

   public float getMaxValue() {
      return this.maxValue;
   }

   public float getStep() {
      return this.step;
   }

   public Consumer<Value> getUpdate() {
      return this.update;
   }

   public float getCurrentValue() {
      return this.currentValue;
   }
}
