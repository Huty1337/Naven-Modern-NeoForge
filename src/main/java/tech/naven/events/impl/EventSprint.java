package tech.naven.events.impl;

import tech.naven.events.api.events.Event;

public class EventSprint implements Event {
   private boolean sprint;

   public EventSprint() {
      this(true);
   }

   public EventSprint(boolean sprint) {
      this.sprint = sprint;
   }

   public boolean isSprint() {
      return this.sprint;
   }

   public void setSprint(boolean sprint) {
      this.sprint = sprint;
   }
}
