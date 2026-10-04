package com.heypixel.heypixelmod.obsoverlay.utils.rotation;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class RayTraceUtil {
   public static HitResult rayCast(float partialTicks, Rotation rotations) {
      HitResult objectMouseOver = null;
      Entity entity = Minecraft.getInstance().getCameraEntity();
      if (entity != null && Minecraft.getInstance().level != null) {
         double distance = Minecraft.getInstance().player.blockInteractionRange();
         objectMouseOver = pick(distance, partialTicks, true, rotations.getYaw(), rotations.getPitch());
      }

      return objectMouseOver;
   }

   public static HitResult rayCast(double range, float partialTicks, boolean hitFluids, Rotation rotations) {
      HitResult objectMouseOver = null;
      Entity entity = Minecraft.getInstance().getCameraEntity();
      if (entity != null && Minecraft.getInstance().level != null) {
         objectMouseOver = pick(range, partialTicks, hitFluids, rotations.getYaw(), rotations.getPitch());
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
      Vec3 vec3 = new Vec3(Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getY() + 1.62, Minecraft.getInstance().player.getZ());
      Vec3 vec31 = calculateViewVector(pXRot, pYRot);
      Vec3 vec32 = vec3.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
      return Minecraft.getInstance()
         .player
         .level()
         .clip(new ClipContext(vec3, vec32, Block.OUTLINE, pHitFluids ? Fluid.ANY : Fluid.NONE, Minecraft.getInstance().player));
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
      return Minecraft.getInstance().level.clip(var9);
   }

   public static EntityHitResult calculateIntercept(AABB instance, Vec3 var1, Vec3 var2) {
      Optional<Vec3> e = instance.clip(var1, var2);
      return e.<EntityHitResult>map(vec3 -> new EntityHitResult(null, vec3)).orElse(null);
   }

   public static HitResult rayCast(Rotation rotation, double range, float expand, Entity filterEntity, Entity targetEntity, boolean throughWalls) {
      if (filterEntity != null && Minecraft.getInstance().level != null) {
         float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
         Vec3 eyePosition = filterEntity.getEyePosition(partialTicks);
         Vec3 lookVector = RotationUtils.getVectorForRotation(rotation);
         Vec3 targetVec = eyePosition.add(lookVector.x * range, lookVector.y * range, lookVector.z * range);
         BlockHitResult blockHit = null;
         double blockDistance = range;
         if (!throughWalls) {
            blockHit = Minecraft.getInstance().level.clip(new ClipContext(eyePosition, targetVec, Block.OUTLINE, Fluid.NONE, filterEntity));
            blockDistance = blockHit.getType() == Type.BLOCK ? eyePosition.distanceTo(blockHit.getLocation()) : range;
         }

         double expandedRange = Math.min(range, blockDistance) + (double)expand;
         AABB searchBox = new AABB(
            eyePosition.x - expandedRange,
            eyePosition.y - expandedRange,
            eyePosition.z - expandedRange,
            eyePosition.x + expandedRange,
            eyePosition.y + expandedRange,
            eyePosition.z + expandedRange
         );
         List<Entity> entities = Minecraft.getInstance()
            .level
            .getEntitiesOfClass(
               Entity.class,
               searchBox,
               ex -> ex != filterEntity && (targetEntity == null || ex == targetEntity) && EntitySelector.NO_SPECTATORS.test(ex) && ex.isPickable()
            );
         Entity pointedEntity = null;
         Vec3 hitVec = null;
         double closestDistance = Math.min(range, blockDistance);
         closestDistance *= closestDistance;

         for (Entity e : entities) {
            AABB entityBox = e.getBoundingBox().inflate((double)expand);
            Optional<Vec3> intercept = entityBox.clip(eyePosition, targetVec);
            if (intercept.isPresent()) {
               Vec3 interceptPoint = intercept.get();
               double distSq = eyePosition.distanceToSqr(interceptPoint);
               if (distSq < closestDistance) {
                  boolean canHit = true;
                  if (!throughWalls) {
                     Vec3 entityCenter = entityBox.getCenter();
                     BlockHitResult wallCheck = Minecraft.getInstance()
                        .level
                        .clip(new ClipContext(eyePosition, entityCenter, Block.OUTLINE, Fluid.NONE, filterEntity));
                     if (wallCheck.getType() == Type.BLOCK && eyePosition.distanceToSqr(wallCheck.getLocation()) <= distSq) {
                        canHit = false;
                     }
                  }

                  if (canHit) {
                     closestDistance = distSq;
                     pointedEntity = e;
                     hitVec = interceptPoint;
                  }
               }
            }
         }

         if (pointedEntity != null) {
            return new EntityHitResult(pointedEntity, hitVec);
         } else {
            return !throughWalls && blockHit != null
               ? blockHit
               : Minecraft.getInstance().level.clip(new ClipContext(eyePosition, targetVec, Block.OUTLINE, Fluid.NONE, filterEntity));
         }
      } else {
         return null;
      }
   }

   private RayTraceUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
