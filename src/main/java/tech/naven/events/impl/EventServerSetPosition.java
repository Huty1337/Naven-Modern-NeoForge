package tech.naven.events.impl;

import tech.naven.events.api.events.Event;
import net.minecraft.network.protocol.Packet;

public class EventServerSetPosition implements Event {
   private Packet<?> packet;

   public Packet<?> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public EventServerSetPosition(Packet<?> packet) {
      this.packet = packet;
   }
}
