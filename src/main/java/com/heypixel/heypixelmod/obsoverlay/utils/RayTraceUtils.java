package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RayTraceUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   public static HitResult rayCast(float partialTicks, Vector2f rotations) {
      HitResult objectMouseOver = null;
      Entity entity = mc.getCameraEntity();
      if (entity != null && mc.level != null) {
         double distance = mc.player.blockInteractionRange();
         objectMouseOver = pick(distance, partialTicks, true, rotations.getX(), rotations.getY());
      }

      return objectMouseOver;
   }

   public static HitResult rayCast(double range, float partialTicks, boolean hitFluids, Vector2f rotations) {
      HitResult objectMouseOver = null;
      Entity entity = mc.getCameraEntity();
      if (entity != null && mc.level != null) {
         objectMouseOver = pick(range, partialTicks, hitFluids, rotations.getX(), rotations.getY());
      }

      return objectMouseOver;
   }

   public static HitResult rayCast(float partialTicks, Rotation rotations) {
      HitResult objectMouseOver = null;
      Entity entity = mc.getCameraEntity();
      if (entity != null && mc.level != null) {
         double distance = mc.player.blockInteractionRange();
         objectMouseOver = pick(distance, partialTicks, true, rotations.getYaw(), rotations.getPitch());
      }

      return objectMouseOver;
   }

   public static Vec3 calculateViewVector(float pXRot, float pYRot) {
      float f = pXRot * (float) (Math.PI / 180.0);
      float f1 = -pYRot * (float) (Math.PI / 180.0);
      float f2 = Mth.cos(f1);
      float f3 = Mth.sin(f1);
      float f4 = Mth.cos(f);
      float f5 = Mth.sin(f);
      return new Vec3((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));
   }

   public static HitResult pick(double pHitDistance, float pPartialTicks, boolean pHitFluids, float pYRot, float pXRot) {
      Vec3 vec3 = new Vec3(mc.player.getX(), mc.player.getY() + 1.62, mc.player.getZ());
      Vec3 vec31 = calculateViewVector(pXRot, pYRot);
      Vec3 vec32 = vec3.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
      return mc.player.level().clip(new ClipContext(vec3, vec32, Block.OUTLINE, pHitFluids ? Fluid.ANY : Fluid.NONE, mc.player));
   }

   public static HitResult rayTraceBlocks(
      Vec3 vec31, Vec3 vec32, boolean stopOnLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock, Entity var6
   ) {
      Block var7;
      if (ignoreBlockWithoutBoundingBox) {
         var7 = Block.COLLIDER;
      } else {
         var7 = returnLastUncollidableBlock ? Block.VISUAL : Block.OUTLINE;
      }

      Fluid var8 = stopOnLiquid ? Fluid.ANY : Fluid.NONE;
      ClipContext var9 = new ClipContext(vec31, vec32, var7, var8, var6);
      return mc.level.clip(var9);
   }

   public static EntityHitResult calculateIntercept(AABB instance, Vec3 var1, Vec3 var2) {
      Optional<Vec3> e = instance.clip(var1, var2);
      return e.<EntityHitResult>map(vec3 -> new EntityHitResult(null, vec3)).orElse(null);
   }
}
