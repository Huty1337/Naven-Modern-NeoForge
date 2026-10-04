package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.ViewClip;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public class MixinCamera {
   @Inject(
      at = {@At("HEAD")},
      method = {"getMaxZoom"},
      cancellable = true
   )
   private void getMaxZoom(float pStartingDistance, CallbackInfoReturnable<Float> cir) {
      if (Naven.getInstance() != null && Naven.getInstance().getModuleManager() != null) {
         ViewClip module = (ViewClip)Naven.getInstance().getModuleManager().getModule(ViewClip.class);
         if (module.isEnabled()) {
            cir.setReturnValue((float)((double)pStartingDistance * (double)module.scale.getCurrentValue() * (double)module.personViewAnimation.value / 100.0));
            cir.cancel();
         }
      }
   }
}
