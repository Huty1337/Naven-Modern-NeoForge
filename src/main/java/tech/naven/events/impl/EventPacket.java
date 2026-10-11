package tech.naven.events.impl;

import tech.naven.events.api.events.callables.EventCancellable;
import tech.naven.events.api.types.EventType;
import net.minecraft.network.protocol.Packet;

public class EventPacket extends EventCancellable {
   private final EventType type;
   private Packet<?> packet;

   public EventType getType() {
      return this.type;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public EventPacket(EventType type, Packet<?> packet) {
      this.type = type;
      this.packet = packet;
   }
}
