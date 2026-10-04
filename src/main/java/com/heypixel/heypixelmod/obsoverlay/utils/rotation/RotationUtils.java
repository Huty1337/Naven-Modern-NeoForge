package com.heypixel.heypixelmod.obsoverlay.utils.rotation;

import com.heypixel.heypixelmod.obsoverlay.utils.MathHelper;
import com.heypixel.heypixelmod.obsoverlay.utils.MathUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class RotationUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   public static float getAngleDifference(float a, float b) {
      return ((a - b) % 360.0F + 540.0F) % 360.0F - 180.0F;
   }

   public static Vec3 getLook() {
      return getLook(mc.player.getYRot(), mc.player.getXRot());
   }

   public static Vector2f getFixedRotation(float yaw, float pitch, float lastYaw, float lastPitch) {
      float f = (float)((Double)mc.options.sensitivity().get() * 0.6F + 0.2F);
      float gcd = f * f * f * 1.2F;
      float deltaYaw = yaw - lastYaw;
      float deltaPitch = pitch - lastPitch;
      float fixedDeltaYaw = deltaYaw - deltaYaw % gcd;
      float fixedDeltaPitch = deltaPitch - deltaPitch % gcd;
      float fixedYaw = lastYaw + fixedDeltaYaw;
      float fixedPitch = lastPitch + fixedDeltaPitch;
      return new Vector2f(fixedYaw, fixedPitch);
   }

   public static Vec3 getLook(float yaw, float pitch) {
      float f = Mth.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f1 = Mth.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f2 = -Mth.cos(-pitch * (float) (Math.PI / 180.0));
      float f3 = Mth.sin(-pitch * (float) (Math.PI / 180.0));
      return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
   }

   public static boolean isVecInside(AABB self, Vec3 vec) {
      return vec.x > self.minX && vec.x < self.maxX && vec.y > self.minY && vec.y < self.maxY && vec.z > self.minZ && vec.z < self.maxZ;
   }

   public static Rotation getRotations(Vec3 eye, Vec3 target) {
      double x = target.x - eye.x;
      double y = target.y - eye.y;
      double z = target.z - eye.z;
      double diffXZ = Math.sqrt(x * x + z * z);
      float yaw = (float)Math.toDegrees(Math.atan2(z, x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(y, diffXZ)));
      return new Rotation(Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch));
   }

   public static Rotation getRotations(BlockPos pos, float partialTicks) {
      Vec3 playerVector = new Vec3(
         mc.player.getX() + mc.player.getDeltaMovement().x * (double)partialTicks,
         mc.player.getY() + (double)mc.player.getEyeHeight() + mc.player.getDeltaMovement().y() * (double)partialTicks,
         mc.player.getZ() + mc.player.getDeltaMovement().z() * (double)partialTicks
      );
      double x = (double)pos.getX() - playerVector.x + 0.5;
      double y = (double)pos.getY() - playerVector.y + 0.5;
      double z = (double)pos.getZ() - playerVector.z + 0.5;
      return diffCalc(randomization(x), randomization(y), randomization(z));
   }

   public static Rotation diffCalc(double diffX, double diffY, double diffZ) {
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new Rotation(Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch));
   }

   private static double randomization(double value) {
      return value + MathUtils.getRandomDoubleInRange(0.05, 0.08) * (MathUtils.getRandomDoubleInRange(0.0, 1.0) * 2.0 - 1.0);
   }

   public static double getMinDistance(Entity target, Vector2f rotations) {
      double minDistance = Double.MAX_VALUE;
      Iterator var4 = getPossibleEyeHeights().iterator();

      while (var4.hasNext()) {
         double eye = (double)((Float)var4.next()).floatValue();
         Vec3 playerPosition = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
         Vec3 eyePos = playerPosition.add(0.0, eye, 0.0);
         minDistance = Math.min(minDistance, getDistance(target, eyePos, rotations));
      }

      return minDistance;
   }

   public static double getDistance(Entity target, Vec3 eyePos, Vector2f rotations) {
      AABB targetBox = getTargetBoundingBox(target);
      HitResult position = getIntercept(targetBox, rotations, eyePos, 6.0);
      if (position != null) {
         Vec3 intercept = position.getLocation();
         return intercept.distanceTo(eyePos);
      } else {
         return 1000.0;
      }
   }

   public static HitResult getIntercept(AABB targetBox, Vector2f rotations, Vec3 eyePos, double reach) {
      Vec3 vec31 = getLook(rotations.x, rotations.y);
      Vec3 vec32 = eyePos.add(vec31.x * reach, vec31.y * reach, vec31.z * reach);
      return ProjectileUtil.getEntityHitResult(
         mc.player, eyePos, vec32, targetBox, p_172770_ -> !p_172770_.isSpectator() && p_172770_.isPickable(), reach * reach
      );
   }

   public static HitResult getIntercept(AABB targetBox, Vector2f rotations, Vec3 eyePos) {
      return getIntercept(targetBox, rotations, eyePos, 6.0);
   }

   public static Vector2f diffCalcVector(double diffX, double diffY, double diffZ) {
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      return new Vector2f(MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch));
   }

   public static Vector2f getRotationsVector(Vec3 vec) {
      Vec3 playerVector = new Vec3(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(), mc.player.getZ());
      double x = vec.x - playerVector.x;
      double y = vec.y - playerVector.y;
      double z = vec.z - playerVector.z;
      return diffCalcVector(x, y, z);
   }

   private static boolean checkHitResult(Vec3 eyePos, HitResult result, Entity target) {
      if (result.getType() == Type.ENTITY && ((EntityHitResult)result).getEntity() == target) {
         Vec3 intercept = result.getLocation();
         return isVecInside(getTargetBoundingBox(target), eyePos) || intercept.distanceTo(eyePos) <= 3.0;
      } else {
         return false;
      }
   }

   private static HitResult rayTrace(Rotation rotations) {
      double d0 = mc.player.blockInteractionRange();
      HitResult hitResult = RayTraceUtil.rayCast(d0, 1.0F, false, rotations);
      Vec3 vec3 = mc.player.getEyePosition(1.0F);
      boolean flag = false;
      double d1 = d0;
      if (d0 > 3.0) {
         flag = true;
      }

      d1 *= d1;
      if (hitResult != null) {
         d1 = hitResult.getLocation().distanceToSqr(vec3);
      }

      Vec3 vec31 = getLook(rotations.getYaw(), rotations.getPitch());
      Vec3 vec32 = vec3.add(vec31.x * d0, vec31.y * d0, vec31.z * d0);
      AABB aabb = mc.player.getBoundingBox().expandTowards(vec31.scale(d0)).inflate(1.0, 1.0, 1.0);
      EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(
         mc.player, vec3, vec32, aabb, p_172770_ -> !p_172770_.isSpectator() && p_172770_.isPickable(), d1
      );
      if (entityhitresult != null) {
         Vec3 vec33 = entityhitresult.getLocation();
         double d2 = vec3.distanceToSqr(vec33);
         if (flag && d2 > 9.0) {
            hitResult = BlockHitResult.miss(vec33, Direction.getApproximateNearest(vec31.x, vec31.y, vec31.z), BlockPos.containing(vec33));
         } else if (d2 < d1 || hitResult == null) {
            hitResult = entityhitresult;
         }
      }

      return hitResult;
   }

   public static Vec3 getVectorForRotation(Rotation rotation) {
      float yawCos = (float)Math.cos((double)(-rotation.getYaw() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float yawSin = (float)Math.sin((double)(-rotation.getYaw() * (float) (Math.PI / 180.0) - (float) Math.PI));
      float pitchCos = (float)(-Math.cos((double)(-rotation.getPitch() * (float) (Math.PI / 180.0))));
      float pitchSin = (float)Math.sin((double)(-rotation.getPitch() * (float) (Math.PI / 180.0)));
      return new Vec3((double)(yawSin * pitchCos), (double)pitchSin, (double)(yawCos * pitchCos));
   }

   public static RotationUtils.Data getRotationDataToEntity(Entity target) {
      Vec3 playerPosition = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      Vec3 eyePos = playerPosition.add(0.0, (double)mc.player.getEyeHeight(), 0.0);
      AABB targetBox = getTargetBoundingBox(target);
      double minX = targetBox.minX;
      double minY = targetBox.minY;
      double minZ = targetBox.minZ;
      double maxX = targetBox.maxX;
      double maxY = targetBox.maxY;
      double maxZ = targetBox.maxZ;
      double spacing = 0.1;
      Set<Vec3> points = new LinkedHashSet<>();
      points.add(new Vec3(minX + maxX / 2.0, minY + maxY / 2.0, minZ + maxZ / 2.0));
      points.add(getClosestPoint(eyePos, targetBox));

      for (double x = minX; x <= maxX; x += spacing) {
         for (double y = minY; y <= maxY; y += spacing) {
            points.add(new Vec3(x, y, minZ));
            points.add(new Vec3(x, y, maxZ));
         }
      }

      for (double x = minX; x <= maxX; x += spacing) {
         for (double z = minZ; z <= maxZ; z += spacing) {
            points.add(new Vec3(x, minY, z));
            points.add(new Vec3(x, maxY, z));
         }
      }

      for (double y = minY; y <= maxY; y += spacing) {
         for (double z = minZ; z <= maxZ; z += spacing) {
            points.add(new Vec3(minX, y, z));
            points.add(new Vec3(maxX, y, z));
         }
      }

      for (Vec3 point : points) {
         Rotation bruteRotations = getRotations(eyePos, point);
         HitResult bruteHitResult = rayTrace(bruteRotations);
         if (checkHitResult(eyePos, bruteHitResult, target)) {
            Vec3 location = bruteHitResult.getLocation();
            return new RotationUtils.Data(
               eyePos,
               location,
               location.distanceTo(eyePos),
               getFixedRotation(bruteRotations.getYaw(), bruteRotations.getPitch(), RotationManager.lastRotations.x, RotationManager.lastRotations.y)
            );
         }
      }

      return new RotationUtils.Data(eyePos, eyePos, 1000.0, null);
   }

   private static AABB getTargetBoundingBox(Entity entity) {
      return entity.getBoundingBox();
   }

   public static List<Float> getPossibleEyeHeights() {
      return List.of(mc.player.getEyeHeight());
   }

   public static Vec3 getClosestPoint(Vec3 vec, AABB aabb) {
      double closestX = Math.max(aabb.minX, Math.min(vec.x, aabb.maxX));
      double closestY = Math.max(aabb.minY, Math.min(vec.y, aabb.maxY));
      double closestZ = Math.max(aabb.minZ, Math.min(vec.z, aabb.maxZ));
      return new Vec3(closestX, closestY, closestZ);
   }

   public static Vector2f getRotations(Entity entity) {
      if (entity == null) {
         return null;
      } else {
         double diffX = entity.getX() - mc.player.getX();
         double diffZ = entity.getZ() - mc.player.getZ();
         double diffY = entity.getY() + (double)entity.getEyeHeight() - (mc.player.getY() + (double)mc.player.getEyeHeight());
         return diffCalcVector(diffX, diffY, diffZ);
      }
   }

   public static float rotateToYaw(float yawSpeed, float currentYaw, float calcYaw) {
      return updateRotation(currentYaw, calcYaw, yawSpeed);
   }

   public static float updateRotation(float current, float calc, float maxDelta) {
      float f = MathHelper.wrapDegrees(calc - current);
      if (f > maxDelta) {
         f = maxDelta;
      }

      if (f < -maxDelta) {
         f = -maxDelta;
      }

      return current + f;
   }

   public static boolean inFoV(Entity entity, float fov) {
      Vector2f rotations = getRotations(entity);
      float diff = Math.abs(mc.player.getYRot() % 360.0F - rotations.x);
      float minDiff = Math.abs(Math.min(diff, 360.0F - diff));
      return minDiff <= fov;
   }

   public static float getDistanceBetweenAngles(float angle1, float angle2) {
      float angle3 = Math.abs(angle1 - angle2) % 360.0F;
      if (angle3 > 180.0F) {
         angle3 = 0.0F;
      }

      return angle3;
   }

   public static Vec3 getEyesPos() {
      return new Vec3(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
   }

   public static class Data {
      private final Vec3 eye;
      private final Vec3 hitVec;
      private final double distance;
      private final Vector2f rotation;

      public Data(Vec3 eye, Vec3 hitVec, double distance, Vector2f rotation) {
         this.eye = eye;
         this.hitVec = hitVec;
         this.distance = distance;
         this.rotation = rotation;
      }

      public Vec3 getEye() {
         return this.eye;
      }

      public Vec3 getHitVec() {
         return this.hitVec;
      }

      public double getDistance() {
         return this.distance;
      }

      public Vector2f getRotation() {
         return this.rotation;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof RotationUtils.Data other)) {
            return false;
         } else if (!other.canEqual(this)) {
            return false;
         } else if (Double.compare(this.getDistance(), other.getDistance()) != 0) {
            return false;
         } else {
            Object this$eye = this.getEye();
            Object other$eye = other.getEye();
            if (this$eye == null ? other$eye == null : this$eye.equals(other$eye)) {
               Object this$hitVec = this.getHitVec();
               Object other$hitVec = other.getHitVec();
               if (this$hitVec == null ? other$hitVec == null : this$hitVec.equals(other$hitVec)) {
                  Object this$rotation = this.getRotation();
                  Object other$rotation = other.getRotation();
                  return this$rotation == null ? other$rotation == null : this$rotation.equals(other$rotation);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof RotationUtils.Data;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         long $distance = Double.doubleToLongBits(this.getDistance());
         result = result * 59 + (int)($distance >>> 32 ^ $distance);
         Object $eye = this.getEye();
         result = result * 59 + ($eye == null ? 43 : $eye.hashCode());
         Object $hitVec = this.getHitVec();
         result = result * 59 + ($hitVec == null ? 43 : $hitVec.hashCode());
         Object $rotation = this.getRotation();
         return result * 59 + ($rotation == null ? 43 : $rotation.hashCode());
      }

      @Override
      public String toString() {
         return "RotationUtils.Data(eye="
            + this.getEye()
            + ", hitVec="
            + this.getHitVec()
            + ", distance="
            + this.getDistance()
            + ", rotation="
            + this.getRotation()
            + ")";
      }
   }
}
