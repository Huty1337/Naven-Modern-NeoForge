package tech.naven.events.impl;

import tech.naven.events.api.events.Event;
import tech.naven.events.api.types.EventType;

public class EventRunTicks implements Event {
   private final EventType type;

   public EventType getType() {
      return this.type;
   }

   public EventRunTicks(EventType type) {
      this.type = type;
   }
}
