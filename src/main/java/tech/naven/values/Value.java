package tech.naven.values;

import tech.naven.Naven;
import tech.naven.exceptions.BadValueTypeException;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;
import tech.naven.values.impl.ModeValue;
import tech.naven.values.impl.StringValue;
import java.util.function.Supplier;

public abstract class Value {
   private final HasValue key;
   private final String name;
   private final Supplier<Boolean> visibility;

   protected Value(HasValue key, String name, Supplier<Boolean> visibility) {
      this.key = key;
      this.name = name;
      this.visibility = visibility;
      Naven.getInstance().getValueManager().addValue(this);
   }

   public abstract ValueType getValueType();

   public BooleanValue getBooleanValue() {
      throw new BadValueTypeException();
   }

   public FloatValue getFloatValue() {
      throw new BadValueTypeException();
   }

   public StringValue getStringValue() {
      throw new BadValueTypeException();
   }

   public ModeValue getModeValue() {
      throw new BadValueTypeException();
   }

   public boolean isVisible() {
      return this.visibility == null || this.visibility.get();
   }

   public HasValue getKey() {
      return this.key;
   }

   public String getName() {
      return this.name;
   }

   public Supplier<Boolean> getVisibility() {
      return this.visibility;
   }
}
