package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.callables.EventCancellable;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import net.minecraft.network.protocol.Packet;

public class EventGlobalPacket extends EventCancellable {
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

   public EventGlobalPacket(EventType type, Packet<?> packet) {
      this.type = type;
      this.packet = packet;
   }
}
