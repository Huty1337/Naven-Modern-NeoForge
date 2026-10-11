package tech.naven.events.impl;

import tech.naven.events.api.events.Event;

public class EventAttackYaw implements Event {
   private float yaw;

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public EventAttackYaw(float yaw) {
      this.yaw = yaw;
   }
}
