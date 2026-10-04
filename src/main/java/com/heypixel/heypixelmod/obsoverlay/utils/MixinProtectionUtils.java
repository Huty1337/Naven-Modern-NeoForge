package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.util.thread.BlockableEventLoop;
import org.slf4j.Logger;

public class MixinProtectionUtils {
   public static <T extends PacketListener> void onEnsureRunningOnSameThread(Logger LOGGER, Packet<T> packet, T listener, BlockableEventLoop<?> executor) throws RunningOnDifferentThreadException {
      if (!executor.isSameThread()) {
         executor.executeIfPossible(() -> {
            if (listener.isAcceptingMessages()) {
               try {
                  EventHandlePacket event = new EventHandlePacket((Packet)packet);
                  if (executor.isSameThread()) {
                     Naven.getInstance().getEventManager().call(event);
                     if (event.isCancelled()) {
                        return;
                     }
                  }

                  packet.handle(listener);
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

   public static byte[] readByteArray(FriendlyByteBuf buf, int maxSize) {
      int i = buf.readVarInt() - 1;
      if (i > maxSize) {
         throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxSize);
      } else {
         byte[] abyte = new byte[i];
         buf.readBytes(abyte);
         return abyte;
      }
   }
}
