package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRayTrace;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventStrafe;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventStuckInBlock;
import com.heypixel.heypixelmod.obsoverlay.utils.BlinkingPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class MixinEntity {
   @Shadow
   protected Vec3 stuckSpeedMultiplier;

   @Shadow
   public abstract float getViewXRot(float var1);

   @Shadow
   public abstract float getViewYRot(float var1);

   @Shadow
   protected abstract Vec3 calculateViewVector(float var1, float var2);

   @Inject(
      method = {"getViewVector"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void hookViewVector(float p_20253_, CallbackInfoReturnable<Vec3> cir) {
      float pitch = this.getViewXRot(p_20253_);
      float yaw = this.getViewYRot(p_20253_);
      Entity thisEntity = (Entity)(Object)this;
      if (thisEntity == Minecraft.getInstance().player) {
         EventRayTrace lookEvent = new EventRayTrace(thisEntity, yaw, pitch);
         Naven.getInstance().getEventManager().call(lookEvent);
         yaw = lookEvent.yaw;
         pitch = lookEvent.pitch;
         cir.setReturnValue(this.calculateViewVector(pitch, yaw));
      }
   }

   @ModifyArg(
      method = {"moveRelative"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;",
         ordinal = 0
      ),
      index = 2
   )
   private float modifyYaw(float yaw) {
      EventStrafe strafe = new EventStrafe(yaw);
      Naven.getInstance().getEventManager().call(strafe);
      return strafe.getYaw();
   }

   @Inject(
      method = {"makeStuckInBlock"},
      at = {@At("RETURN")}
   )
   private void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier, CallbackInfo ci) {
      Entity thisEntity = (Entity)(Object)this;
      if (Minecraft.getInstance().player == thisEntity) {
         EventStuckInBlock event = new EventStuckInBlock(pState, pMotionMultiplier);
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            this.stuckSpeedMultiplier = Vec3.ZERO;
            return;
         }

         this.stuckSpeedMultiplier = event.getStuckSpeedMultiplier();
      }
   }

   @Inject(
      method = {"push(Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void push(Entity pEntity, CallbackInfo ci) {
      if (pEntity instanceof BlinkingPlayer) {
         ci.cancel();
      }
   }
}
