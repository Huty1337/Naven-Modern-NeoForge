package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PlayerUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   public static boolean movementInput() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }

   public static int getMoveSpeedEffectAmplifier() {
      return mc.player.hasEffect(MobEffects.SPEED) ? mc.player.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
   }

   public static Vec3 getVectorForRotation(Vector2f rotation) {
      float yawCos = (float)Math.cos((double)(-rotation.getX() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float yawSin = (float)Math.sin((double)(-rotation.getX() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float pitchCos = (float)(-Math.cos((double)(-rotation.getY() * (float) (Math.PI / 180.0))));
      float pitchSin = (float)Math.sin((double)(-rotation.getY() * (float) (Math.PI / 180.0)));
      return new Vec3((double)(yawSin * pitchCos), (double)pitchSin, (double)(yawCos * pitchCos));
   }

   public static HitResult pickCustom(double blockReachDistance, float yaw, float pitch) {
      if (mc.player != null && mc.level != null) {
         Vec3 vec3 = mc.player.getEyePosition(1.0F);
         Vec3 vec31 = getVectorForRotation(new Vector2f(yaw, pitch));
         Vec3 vec32 = vec3.add(vec31.x * blockReachDistance, vec31.y * blockReachDistance, vec31.z * blockReachDistance);
         return mc.level.clip(new ClipContext(vec3, vec32, Block.OUTLINE, Fluid.NONE, mc.player));
      } else {
         return null;
      }
   }
}
