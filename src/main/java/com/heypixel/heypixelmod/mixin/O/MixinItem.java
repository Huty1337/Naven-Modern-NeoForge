package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUseItemRayTrace;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Item.class})
public class MixinItem {
   @Redirect(
      method = {"getPlayerPOVHitResult"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
      )
   )
   private static float hookRayTraceYRot(Player instance) {
      EventUseItemRayTrace event = new EventUseItemRayTrace(instance.getYRot(), instance.getXRot());
      Naven.getInstance().getEventManager().call(event);
      return event.getYaw();
   }

   @Redirect(
      method = {"getPlayerPOVHitResult"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"
      )
   )
   private static float hookRayTraceXRot(Player instance) {
      EventUseItemRayTrace event = new EventUseItemRayTrace(instance.getYRot(), instance.getXRot());
      Naven.getInstance().getEventManager().call(event);
      return event.getPitch();
   }
}
