package tech.naven.events.impl;

import tech.naven.events.api.events.callables.EventCancellable;
import tech.naven.events.api.types.EventType;
import net.minecraft.world.entity.Entity;

public class EventAttack extends EventCancellable {
   private final Entity target;
   private final EventType type;

   public EventAttack(Entity target, EventType type) {
      this.target = target;
      this.type = type;
   }

   public Entity getTarget() {
      return this.target;
   }

   public EventType getType() {
      return this.type;
   }
}
