package com.heypixel.heypixelmod.obsoverlay.utils;

public class SmoothAnimationTimer {
   public float target;
   public float speed = 0.4F;
   public float value;

   public SmoothAnimationTimer(float target) {
      this.target = target;
      this.value = target;
   }

   public SmoothAnimationTimer(float target, float value) {
      this.target = target;
      this.value = value;
   }

   public SmoothAnimationTimer(float target, float value, float speed) {
      this.target = target;
      this.speed = speed;
      this.value = value;
   }

   public void update(boolean increment) {
      this.value = AnimationUtils.getAnimationState(
         this.value, increment ? this.target : 0.0F, Math.max(10.0F, Math.abs(this.value - (increment ? this.target : 0.0F)) * 40.0F) * this.speed
      );
   }

   public boolean isAnimationDone(boolean increment) {
      return increment ? this.value == this.target : this.value == 0.0F;
   }
}
