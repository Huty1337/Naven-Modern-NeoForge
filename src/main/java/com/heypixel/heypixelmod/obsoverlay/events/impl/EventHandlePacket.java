package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.callables.EventCancellable;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

public class EventHandlePacket extends EventCancellable {
   private Packet<ClientGamePacketListener> packet;

   public Packet<ClientGamePacketListener> getPacket() {
      return this.packet;
   }

   public void setPacket(Packet<ClientGamePacketListener> packet) {
      this.packet = packet;
   }

   public EventHandlePacket(Packet<ClientGamePacketListener> packet) {
      this.packet = packet;
   }
}
