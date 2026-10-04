package com.heypixel.heypixelmod.obsoverlay.utils;

public class AnimationUtils {
   public static int delta;

   public static float getAnimationState(float animation, float finalState, float speed) {
      float add = (float)delta * (speed / 1000.0F);
      if (animation < finalState) {
         if (animation + add < finalState) {
            animation += add;
         } else {
            animation = finalState;
         }
      } else if (animation - add > finalState) {
         animation -= add;
      } else {
         animation = finalState;
      }

      return animation;
   }
}
