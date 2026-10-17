package tech.naven.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tech.naven.utils.rotation.RotationManager;

public class FallingPlayer {
   public double x;
   public double y;
   public double z;
   private double motionX;
   private double motionY;
   private double motionZ;
   private final float yaw;
   private final float strafe;
   private final float forward;
   private float jumpMovementFactor;
   private boolean onGround;
   private double eyeHeight;

   private static Minecraft mc() {
      return Minecraft.getInstance();
   }

   public FallingPlayer(double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float strafe, float forward) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.motionX = motionX;
      this.motionY = motionY;
      this.motionZ = motionZ;
      this.yaw = yaw;
      this.strafe = strafe;
      this.forward = forward;
      this.jumpMovementFactor = 0.02F;
      this.eyeHeight = 1.62;
   }

   public FallingPlayer(Player player) {
      this(
         player.getX(),
         player.getY(),
         player.getZ(),
         player.getDeltaMovement().x,
         player.getDeltaMovement().y,
         player.getDeltaMovement().z,
         currentYaw(player),
         moveStrafe(player),
         moveForward(player)
      );
      this.jumpMovementFactor = player.isSprinting() ? 0.026F : 0.02F;
      this.eyeHeight = player.getEyeHeight();
      this.onGround = player.onGround();
   }

   private static float currentYaw(Player player) {
      if (RotationManager.active && RotationManager.rotations != null) {
         return RotationManager.rotations.getX();
      }

      return player.getYRot();
   }

   private static float moveStrafe(Player player) {
      return player instanceof LocalPlayer local ? local.input.getMoveVector().x : 0.0F;
   }

   private static float moveForward(Player player) {
      return player instanceof LocalPlayer local ? local.input.getMoveVector().y : 0.0F;
   }

   private void calculateForTick() {
      this.updateVelocity(this.jumpMovementFactor, new Vec3(this.strafe, 0.0, this.forward));
      this.x = this.x + this.motionX;
      this.y = this.y + this.motionY;
      this.z = this.z + this.motionZ;
      this.updateGroundState();
      this.motionY -= 0.08;
      this.motionX *= 0.91;
      this.motionY *= 0.98;
      this.motionZ *= 0.91;
   }

   private void calculateForTick2() {
      this.updateVelocity(this.jumpMovementFactor, new Vec3(this.strafe, 0.0, this.forward));
      this.x = this.x + this.motionX;
      this.y = this.y + this.motionY;
      this.z = this.z + this.motionZ;
      this.motionY -= 0.08;
      this.motionY *= 0.98;
   }

   private void updateVelocity(float speed, Vec3 input) {
      double lengthSquared = input.lengthSqr();
      if (lengthSquared < 1.0E-7) {
         return;
      }

      Vec3 normalizedInput = (lengthSquared > 1.0 ? input.normalize() : input).scale(speed);
      float sin = (float)Math.sin(this.yaw * (float) Math.PI / 180.0F);
      float cos = (float)Math.cos(this.yaw * (float) Math.PI / 180.0F);
      double inputX = normalizedInput.x * cos - normalizedInput.z * sin;
      double inputZ = normalizedInput.z * cos + normalizedInput.x * sin;
      this.motionX += inputX;
      this.motionZ += inputZ;
   }

   private void updateGroundState() {
      Vec3 center = new Vec3(this.x, this.y, this.z);
      Vec3 down = center.add(0.0, -0.2, 0.0);
      BlockHitResult result = this.rayTraceHit(center, down);
      this.onGround = result != null && result.getType() == HitResult.Type.BLOCK && result.getDirection() == Direction.UP;
   }

   public void calculate(int ticks) {
      for (int i = 0; i < ticks; i++) {
         this.calculateForTick();
      }
   }

   public void calculateMLG(int ticks) {
      for (int i = 0; i < ticks; i++) {
         this.calculateForTick2();
      }
   }

   public BlockPos findCollision(int ticks) {
      float width = mc().player != null ? mc().player.getBbWidth() / 2.0F : 0.3F;
      for (int i = 0; i < ticks; i++) {
         Vec3 start = new Vec3(this.x, this.y, this.z);
         this.calculateForTick();
         Vec3 end = new Vec3(this.x, this.y, this.z);

         BlockPos hit;
         if ((hit = this.rayTrace(start, end)) != null) {
            return hit;
         }

         if ((hit = this.rayTrace(start.add(width, 0.0, width), end.add(width, 0.0, width))) != null) {
            return hit;
         }

         if ((hit = this.rayTrace(start.add(-width, 0.0, width), end.add(-width, 0.0, width))) != null) {
            return hit;
         }

         if ((hit = this.rayTrace(start.add(width, 0.0, -width), end.add(width, 0.0, -width))) != null) {
            return hit;
         }

         if ((hit = this.rayTrace(start.add(-width, 0.0, -width), end.add(-width, 0.0, -width))) != null) {
            return hit;
         }

         if ((hit = this.rayTrace(start.add(width, 0.0, 0.0), end.add(width, 0.0, 0.0))) != null) {
            return hit;
         }

         if ((hit = this.rayTrace(start.add(-width, 0.0, 0.0), end.add(-width, 0.0, 0.0))) != null) {
            return hit;
         }

         if ((hit = this.rayTrace(start.add(0.0, 0.0, width), end.add(0.0, 0.0, width))) != null) {
            return hit;
         }

         if ((hit = this.rayTrace(start.add(0.0, 0.0, -width), end.add(0.0, 0.0, -width))) != null) {
            return hit;
         }
      }

      return null;
   }

   private BlockHitResult rayTraceHit(Vec3 start, Vec3 end) {
      if (start.distanceTo(end) < 1.0E-7) {
         return null;
      }

      HitResult result = RayTraceUtils.rayTraceBlocks(start, end, false, false, false, mc().player);
      return result instanceof BlockHitResult hit ? hit : null;
   }

   private BlockPos rayTrace(Vec3 start, Vec3 end) {
      BlockHitResult result = this.rayTraceHit(start, end);
      if (result != null && result.getType() == HitResult.Type.BLOCK && result.getDirection() == Direction.UP) {
         return result.getBlockPos();
      }

      return null;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public Vec3 getEyePos() {
      return new Vec3(this.x, this.y + this.eyeHeight, this.z);
   }

   public Vec3 getPos() {
      return new Vec3(this.x, this.y, this.z);
   }
}
