package com.heypixel.heypixelmod.obsoverlay.utils;

import java.util.Arrays;

final class FloatList {
   private float[] values = new float[64];
   private int size;

   void add(float value) {
      this.ensure(1);
      this.values[this.size++] = value;
   }

   void add(float x, float y) {
      this.ensure(2);
      this.values[this.size++] = x;
      this.values[this.size++] = y;
   }

   float get(int index) {
      return this.values[index];
   }

   int size() {
      return this.size;
   }

   void clear() {
      this.size = 0;
   }

   private void ensure(int additional) {
      int required = this.size + additional;
      if (required > this.values.length) {
         int capacity = Math.max(required, this.values.length * 2);
         this.values = Arrays.copyOf(this.values, capacity);
      }
   }

   float[] toArray() {
      return Arrays.copyOf(this.values, this.size);
   }
}
