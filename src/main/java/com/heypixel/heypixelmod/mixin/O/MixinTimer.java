package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DeltaTracker.Timer.class})
public class MixinTimer {
   @Shadow
   private float deltaTicks;
   @Shadow
   private float deltaTickResidual;
   @Shadow
   private long lastMs;
   @Shadow
   private long lastUiMs;
   @Shadow
   private float realtimeDeltaTicks;
   @Final
   @Shadow
   private float msPerTick;

   @Inject(
      method = {"advanceTime"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void beginRenderTickHook(long timeMillis, boolean advanceGameTime, CallbackInfoReturnable<Integer> cir) {
      if (Naven.TICK_TIMER != 1.0F) {
         this.realtimeDeltaTicks = (float)(timeMillis - this.lastUiMs) / this.msPerTick;
         this.lastUiMs = timeMillis;
         if (!advanceGameTime) {
            cir.setReturnValue(0);
         } else {
            this.deltaTicks = (float)(timeMillis - this.lastMs) / this.msPerTick * Naven.TICK_TIMER;
            this.lastMs = timeMillis;
            this.deltaTickResidual = this.deltaTickResidual + this.deltaTicks;
            int i = (int)this.deltaTickResidual;
            this.deltaTickResidual -= (float)i;
            cir.setReturnValue(i);
         }
      }
   }
}
