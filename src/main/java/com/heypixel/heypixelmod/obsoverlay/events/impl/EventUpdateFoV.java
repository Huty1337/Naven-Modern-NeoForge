package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;

public class EventUpdateFoV implements Event {
   private float fov;

   public EventUpdateFoV(float fov) {
      this.fov = fov;
   }

   public float getFov() {
      return this.fov;
   }

   public void setFov(float fov) {
      this.fov = fov;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventUpdateFoV other)) {
         return false;
      } else {
         return !other.canEqual(this) ? false : Float.compare(this.getFov(), other.getFov()) == 0;
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventUpdateFoV;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      return result * 59 + Float.floatToIntBits(this.getFov());
   }

   @Override
   public String toString() {
      return "EventUpdateFoV(fov=" + this.getFov() + ")";
   }
}
