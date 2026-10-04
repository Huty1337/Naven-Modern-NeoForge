package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRotationAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public class MixinLivingEntityRenderer {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"},
      at = {@At("HEAD")}
   )
   private void renderHead(LivingEntity pEntity, LivingEntityRenderState pState, float pPartialTicks, CallbackInfo ci) {
      EventRotationAnimation.currentEntity = pEntity;
   }

   @Redirect(
      method = {"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F",
         ordinal = 0
      )
   )
   private float rotAnimationYaw(float pDelta, float pStart, float pEnd) {
      if (EventRotationAnimation.currentEntity != Minecraft.getInstance().player) {
         return Mth.rotLerp(pDelta, pStart, pEnd);
      }

      EventRotationAnimation event = new EventRotationAnimation(pEnd, pStart, 0.0F, 0.0F);
      Naven.getInstance().getEventManager().call(event);
      return Mth.rotLerp(pDelta, event.getLastYaw(), event.getYaw());
   }

   @Redirect(
      method = {"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F",
         ordinal = 0
      )
   )
   private float rotAnimationPitch(LivingEntity instance, float pPartialTicks) {
      float pitch = instance.getXRot(pPartialTicks);
      if (EventRotationAnimation.currentEntity != Minecraft.getInstance().player) {
         return pitch;
      }

      EventRotationAnimation event = new EventRotationAnimation(0.0F, 0.0F, pitch, pitch);
      Naven.getInstance().getEventManager().call(event);
      return Mth.lerp(pPartialTicks, event.getLastPitch(), event.getPitch());
   }
}
