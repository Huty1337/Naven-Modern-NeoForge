package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventServerSetPosition;
import com.heypixel.heypixelmod.obsoverlay.utils.HttpUtils;
import java.io.IOException;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPacketListener.class})
public class MixinClientPacketListener {
   @Redirect(
      method = {"handleMovePlayer"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V",
         ordinal = 1
      )
   )
   public void onSendPacket(Connection instance, Packet<?> pPacket) {
      EventServerSetPosition event = new EventServerSetPosition(pPacket);
      Naven.getInstance().getEventManager().call(event);
      instance.send(event.getPacket());
   }

   @Inject(
      method = {"handleLogin"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/telemetry/WorldSessionTelemetryManager;onPlayerInfoReceived(Lnet/minecraft/world/level/GameType;Z)V",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void onLogin(ClientboundLoginPacket p_105030_, CallbackInfo ci) {
      try {
         HttpUtils.get("http://127.0.0.1:23233/api/setHook?hook=0");
      } catch (IOException var4) {
      }
   }
}
