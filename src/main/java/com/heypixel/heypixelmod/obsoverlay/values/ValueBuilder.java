package com.heypixel.heypixelmod.obsoverlay.values;

import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.StringValue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ValueBuilder {
   private final HasValue key;
   private final String name;
   private ValueType valueType;
   private Consumer<Value> update;
   private Supplier<Boolean> visibility;
   private boolean defaultBooleanValue;
   private float defaultFloatValue;
   private float minFloatValue;
   private float maxFloatValue;
   private float step;
   private String[] modes;
   private int defaultModeIndex;
   private String defaultStringValue;

   private ValueBuilder(HasValue key, String name) {
      this.key = key;
      this.name = name;
   }

   public static ValueBuilder create(HasValue key, String name) {
      return new ValueBuilder(key, name);
   }

   public ValueBuilder setValueType(ValueType valueType) {
      this.valueType = valueType;
      return this;
   }

   public ValueBuilder setDefaultBooleanValue(boolean defaultBooleanValue) {
      if (this.valueType == null) {
         this.setValueType(ValueType.BOOLEAN);
      }

      if (this.valueType != ValueType.BOOLEAN) {
         throw new IllegalStateException("Value type is not boolean");
      } else {
         this.defaultBooleanValue = defaultBooleanValue;
         return this;
      }
   }

   public ValueBuilder setDefaultFloatValue(float defaultFloatValue) {
      if (this.valueType == null) {
         this.setValueType(ValueType.FLOAT);
      }

      if (this.valueType != ValueType.FLOAT) {
         throw new IllegalStateException("Value type is not float");
      } else {
         this.defaultFloatValue = defaultFloatValue;
         return this;
      }
   }

   public ValueBuilder setMinFloatValue(float minFloatValue) {
      if (this.valueType == null) {
         this.setValueType(ValueType.FLOAT);
      }

      if (this.valueType != ValueType.FLOAT) {
         throw new IllegalStateException("Value type is not float");
      } else {
         this.minFloatValue = minFloatValue;
         return this;
      }
   }

   public ValueBuilder setMaxFloatValue(float maxFloatValue) {
      if (this.valueType == null) {
         this.setValueType(ValueType.FLOAT);
      }

      if (this.valueType != ValueType.FLOAT) {
         throw new IllegalStateException("Value type is not float");
      } else {
         this.maxFloatValue = maxFloatValue;
         return this;
      }
   }

   public ValueBuilder setFloatStep(float step) {
      if (this.valueType == null) {
         this.setValueType(ValueType.FLOAT);
      }

      if (this.valueType != ValueType.FLOAT) {
         throw new IllegalStateException("Value type is not float");
      } else {
         this.step = step;
         return this;
      }
   }

   public ValueBuilder setModes(String... modes) {
      if (this.valueType == null) {
         this.setValueType(ValueType.MODE);
      }

      if (this.valueType != ValueType.MODE) {
         throw new IllegalStateException("Value type is not mode");
      } else {
         this.modes = modes;
         return this;
      }
   }

   public ValueBuilder setDefaultModeIndex(int defaultModeIndex) {
      if (this.valueType == null) {
         this.setValueType(ValueType.MODE);
      }

      if (this.valueType != ValueType.MODE) {
         throw new IllegalStateException("Value type is not mode");
      } else {
         this.defaultModeIndex = defaultModeIndex;
         return this;
      }
   }

   public ValueBuilder setDefaultStringValue(String defaultStringValue) {
      if (this.valueType == null) {
         this.setValueType(ValueType.STRING);
      }

      if (this.valueType != ValueType.STRING) {
         throw new IllegalStateException("Value type is not string");
      } else {
         this.defaultStringValue = defaultStringValue;
         return this;
      }
   }

   public ValueBuilder setOnUpdate(Consumer<Value> update) {
      this.update = update;
      return this;
   }

   public ValueBuilder setVisibility(Supplier<Boolean> visibility) {
      this.visibility = visibility;
      return this;
   }

   public Value build() {
      if (this.valueType == null) {
         throw new IllegalStateException("Value type is not set");
      } else {
         switch (this.valueType) {
            case BOOLEAN:
               return new BooleanValue(this.key, this.name, this.defaultBooleanValue, this.update, this.visibility);
            case FLOAT:
               return new FloatValue(
                  this.key, this.name, this.defaultFloatValue, this.minFloatValue, this.maxFloatValue, this.step, this.update, this.visibility
               );
            case MODE:
               if (this.modes == null) {
                  throw new IllegalStateException("Modes are not set");
               }

               return new ModeValue(this.key, this.name, this.modes, this.defaultModeIndex, this.update, this.visibility);
            case STRING:
               if (this.defaultStringValue == null) {
                  throw new IllegalStateException("Default string value is not set");
               }

               return new StringValue(this.key, this.name, this.defaultStringValue, this.update, this.visibility);
            default:
               throw new IllegalStateException("Unknown value type");
         }
      }
   }
}
