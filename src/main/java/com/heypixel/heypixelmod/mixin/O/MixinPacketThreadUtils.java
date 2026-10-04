package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.server.RunningOnDifferentThreadException;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PacketUtils.class})
public class MixinPacketThreadUtils {
   @Shadow
   @Final
   private static Logger LOGGER;

   @Inject(
      method = {"ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static <T extends PacketListener> void onEnsureRunningOnSameThread(
      Packet<T> packet, T listener, PacketProcessor packetProcessor, CallbackInfo ci
   ) throws RunningOnDifferentThreadException {
      if (!packetProcessor.isSameThread()) {
         packetProcessor.scheduleIfPossible(() -> {
            if (listener.isAcceptingMessages()) {
               try {
                  EventHandlePacket event = new EventHandlePacket((Packet)packet);
                  Naven.getInstance().getEventManager().call(event);
                  if (!event.isCancelled()) {
                     packet.handle(listener);
                  }
               } catch (Exception exception) {
                  LOGGER.error("Failed to handle packet {}, suppressing error", packet, exception);
               }
            } else {
               LOGGER.debug("Ignoring packet due to disconnection: {}", packet);
            }
         });
         throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
      }
   }
}
