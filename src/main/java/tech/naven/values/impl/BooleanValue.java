package tech.naven.values.impl;

import tech.naven.values.HasValue;
import tech.naven.values.Value;
import tech.naven.values.ValueType;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanValue extends Value {
   private final boolean defaultValue;
   private final Consumer<Value> update;
   public boolean currentValue;

   public BooleanValue(HasValue key, String name, boolean defaultValue, Consumer<Value> update, Supplier<Boolean> visibility) {
      super(key, name, visibility);
      this.update = update;
      this.currentValue = this.defaultValue = defaultValue;
   }

   @Override
   public ValueType getValueType() {
      return ValueType.BOOLEAN;
   }

   @Override
   public BooleanValue getBooleanValue() {
      return this;
   }

   public boolean getDefaultValue() {
      return this.defaultValue;
   }

   public boolean getCurrentValue() {
      return this.currentValue;
   }

   public void setCurrentValue(boolean currentValue) {
      this.currentValue = currentValue;
      if (this.update != null) {
         this.update.accept(this);
      }
   }
}
