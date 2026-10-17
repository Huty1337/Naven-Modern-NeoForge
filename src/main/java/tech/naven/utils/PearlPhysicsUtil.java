package tech.naven.utils;

import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PearlPhysicsUtil {
   public static final double PEARL_INITIAL_VELOCITY = 1.5;
   public static final double PEARL_GRAVITY = 0.03;
   public static final double PEARL_DRAG = 0.99;

   private PearlPhysicsUtil() {
   }

   public static Object[] predictPearlLandingWithTicks(ThrownEnderpearl pearl, ClientLevel level) {
      double pX = pearl.getX();
      double pY = pearl.getY();
      double pZ = pearl.getZ();
      double mX = pearl.getDeltaMovement().x;
      double mY = pearl.getDeltaMovement().y;
      double mZ = pearl.getDeltaMovement().z;

      List<Entity> nearbyEntities = level.getEntities(
         pearl,
         pearl.getBoundingBox().inflate(128.0),
         e -> e instanceof LivingEntity && !(e instanceof EnderMan) && e != pearl.getOwner()
      );

      int ticks = 0;
      for (int i = 0; i < 1000; i++) {
         Vec3 curr = new Vec3(pX, pY, pZ);
         Vec3 next = new Vec3(pX + mX, pY + mY, pZ + mZ);
         ticks++;

         HitResult hit = RayTraceUtils.rayTraceBlocks(curr, next, false, false, false, pearl);
         if (hit.getType() != HitResult.Type.MISS) {
            return new Object[]{hit.getLocation(), ticks};
         }

         for (Entity entity : nearbyEntities) {
            HitResult entityHit = RayTraceUtils.calculateIntercept(entity.getBoundingBox(), curr, next);
            if (entityHit != null) {
               return new Object[]{entityHit.getLocation(), ticks};
            }
         }

         pX += mX;
         pY += mY;
         pZ += mZ;
         mX *= PEARL_DRAG;
         mY *= PEARL_DRAG;
         mZ *= PEARL_DRAG;
         mY -= PEARL_GRAVITY;
      }

      return new Object[]{null, ticks};
   }

   public static Vector2f calculateOptimalRotations(Vec3 eyePos, Vec3 targetPos) {
      double dX = targetPos.x - eyePos.x;
      double dY = targetPos.y - eyePos.y;
      double dZ = targetPos.z - eyePos.z;
      float yaw = (float)(Math.toDegrees(Math.atan2(dZ, dX)) - 90.0);
      double hD = Math.sqrt(dX * dX + dZ * dZ);
      if (hD == 0.0) {
         return new Vector2f(yaw, dY > 0.0 ? -90.0F : 90.0F);
      }

      double v = PEARL_INITIAL_VELOCITY;
      double g = PEARL_GRAVITY;
      double disc = v * v * v * v - g * (g * hD * hD + 2.0 * dY * v * v);
      if (disc < 0.0) {
         return null;
      }

      float pitch = (float)(-Math.toDegrees(Math.atan((v * v - Math.sqrt(disc)) / (g * hD))));
      return new Vector2f(yaw, pitch);
   }

   public static boolean isTrajectoryClear(ClientLevel level, Player player, Vec3 target) {
      Vector2f rots = calculateOptimalRotations(player.getEyePosition(), target);
      if (rots == null) {
         return false;
      }

      Vec3 eyePos = player.getEyePosition();
      double pX = eyePos.x;
      double pY = eyePos.y;
      double pZ = eyePos.z;
      float yawRad = (float)Math.toRadians((double)(rots.x + 90.0F));
      float pitchRad = (float)Math.toRadians((double)(-rots.y));

      double mX = Math.cos((double)yawRad) * Math.cos((double)pitchRad) * PEARL_INITIAL_VELOCITY;
      double mY = Math.sin((double)pitchRad) * PEARL_INITIAL_VELOCITY;
      double mZ = Math.sin((double)yawRad) * Math.cos((double)pitchRad) * PEARL_INITIAL_VELOCITY;

      for (int i = 0; i < 300; i++) {
         Vec3 currentPos = new Vec3(pX, pY, pZ);
         Vec3 nextPos = new Vec3(pX + mX, pY + mY, pZ + mZ);

         if (currentPos.distanceToSqr(eyePos) > target.distanceToSqr(eyePos)) {
            break;
         }

         if (RayTraceUtils.rayTraceBlocks(currentPos, nextPos, false, false, false, player).getType() != HitResult.Type.MISS) {
            return false;
         }

         for (Entity entity : level.entitiesForRendering()) {
            if (entity.equals(player) || !entity.isPickable() || !(entity instanceof LivingEntity)) {
               continue;
            }

            if (entity.getBoundingBox().inflate(0.3).clip(currentPos, nextPos).isPresent()) {
               return false;
            }
         }

         pX += mX;
         pY += mY;
         pZ += mZ;
         mX *= PEARL_DRAG;
         mY *= PEARL_DRAG;
         mZ *= PEARL_DRAG;
         mY -= PEARL_GRAVITY;
      }

      return true;
   }
}
