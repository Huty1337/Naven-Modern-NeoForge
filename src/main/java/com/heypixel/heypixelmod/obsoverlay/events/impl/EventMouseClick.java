package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;

public class EventMouseClick implements Event {
   private final int key;
   private final boolean state;

   public int getKey() {
      return this.key;
   }

   public boolean isState() {
      return this.state;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventMouseClick other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         return this.getKey() != other.getKey() ? false : this.isState() == other.isState();
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventMouseClick;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getKey();
      return result * 59 + (this.isState() ? 79 : 97);
   }

   @Override
   public String toString() {
      return "EventMouseClick(key=" + this.getKey() + ", state=" + this.isState() + ")";
   }

   public EventMouseClick(int key, boolean state) {
      this.key = key;
      this.state = state;
   }
}
