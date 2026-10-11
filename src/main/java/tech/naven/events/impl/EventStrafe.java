package tech.naven.events.impl;

import tech.naven.events.api.events.Event;

public class EventStrafe implements Event {
   private float yaw;

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getYaw() {
      return this.yaw;
   }

   public EventStrafe(float yaw) {
      this.yaw = yaw;
   }
}
