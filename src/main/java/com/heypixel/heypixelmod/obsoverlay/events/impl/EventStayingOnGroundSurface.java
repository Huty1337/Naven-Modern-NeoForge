package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;

public class EventStayingOnGroundSurface implements Event {
   private boolean stay;

   public boolean isStay() {
      return this.stay;
   }

   public void setStay(boolean stay) {
      this.stay = stay;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventStayingOnGroundSurface other)) {
         return false;
      } else {
         return !other.canEqual(this) ? false : this.isStay() == other.isStay();
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventStayingOnGroundSurface;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      return result * 59 + (this.isStay() ? 79 : 97);
   }

   @Override
   public String toString() {
      return "EventStayingOnGroundSurface(stay=" + this.isStay() + ")";
   }

   public EventStayingOnGroundSurface(boolean stay) {
      this.stay = stay;
   }
}
