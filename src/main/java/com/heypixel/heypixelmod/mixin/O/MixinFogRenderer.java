package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.AntiBlindness;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MobEffectFogEnvironment.class})
public class MixinFogRenderer {
   @Inject(
      method = {"isApplicable"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void naven$isApplicable(FogType fogType, Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this instanceof BlindnessFogEnvironment
         && Naven.getInstance().getModuleManager().getModule(AntiBlindness.class).isEnabled()) {
         cir.setReturnValue(false);
      }
   }
}
