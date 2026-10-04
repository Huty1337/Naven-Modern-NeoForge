package com.heypixel.heypixelmod.obsoverlay.utils.rotation;

import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.RandomUtils;

public class Rotation {
   float yaw;
   float pitch;
   public double distanceSq;
   public Runnable task;
   public Runnable postTask;

   public Rotation() {
      this.yaw = 0.0F;
      this.pitch = 0.0F;
   }

   public Rotation(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public Rotation(Vector2f vec) {
      this.yaw = vec.getX();
      this.pitch = vec.getY();
   }

   public Rotation(Vec3 from, Vec3 to) {
      Vec3 diff = to.subtract(from);
      this.yaw = Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
      this.pitch = Mth.wrapDegrees((float)(-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)))));
   }

   public Vector2f toVec2f() {
      return new Vector2f(this.yaw, this.pitch);
   }

   public Rotation subtract(Rotation other) {
      return new Rotation(this.yaw - other.yaw, this.pitch - other.pitch);
   }

   public Rotation invert() {
      return new Rotation(-this.yaw, -this.pitch);
   }

   public Rotation onApply(Runnable task) {
      this.task = task;
      return this;
   }

   public Rotation onPost(Runnable task) {
      this.postTask = task;
      return this;
   }

   public void apply() {
      Minecraft.getInstance().player.setYRot(this.yaw);
      Minecraft.getInstance().player.setXRot(this.pitch);
   }

   public void toPlayer(Player player) {
      if (!Float.isNaN(this.yaw) && !Float.isNaN(this.pitch)) {
         this.fixedSensitivity(((Double)Minecraft.getInstance().options.sensitivity().get()).floatValue());
         player.setYRot(this.yaw);
         player.setXRot(this.pitch);
      }
   }

   public void fixedSensitivity(Float sensitivity) {
      float f = sensitivity * 0.6F + 0.2F;
      float gcd = f * f * f * 1.2F;
      this.yaw = this.yaw - this.yaw % gcd;
      this.pitch = this.pitch - this.pitch % gcd;
   }

   public static float updateRotation(float current, float calc, float maxDelta) {
      float f = Mth.wrapDegrees(calc - current);
      if (f > maxDelta) {
         f = maxDelta;
      }

      if (f < -maxDelta) {
         f = -maxDelta;
      }

      return current + f;
   }

   public double getAngleTo(Rotation other) {
      float yaw1 = Mth.wrapDegrees(this.yaw);
      float yaw2 = Mth.wrapDegrees(other.yaw);
      float diffYaw = Mth.wrapDegrees(yaw1 - yaw2);
      float pitch1 = Mth.wrapDegrees(this.pitch);
      float pitch2 = Mth.wrapDegrees(other.pitch);
      float diffPitch = Mth.wrapDegrees(pitch1 - pitch2);
      return Math.sqrt((double)(diffYaw * diffYaw + diffPitch * diffPitch));
   }

   public float rotateToYaw(float yawSpeed, float currentYaw, float calcYaw) {
      float yaw = updateRotation(currentYaw, calcYaw, yawSpeed + RandomUtils.nextFloat(0.0F, 15.0F));
      double diffYaw = (double)Mth.wrapDegrees(calcYaw - currentYaw);
      if ((double)(-yawSpeed) > diffYaw || diffYaw > (double)yawSpeed) {
         assert Minecraft.getInstance().player != null;

         yaw += (float)((double)RandomUtils.nextFloat(1.0F, 2.0F) * Math.sin((double)Minecraft.getInstance().player.getXRot() * Math.PI));
      }

      if (yaw == currentYaw) {
         return currentYaw;
      } else {
         float mouseSensitivity = ((Double)Minecraft.getInstance().options.sensitivity().get()).floatValue();
         if ((double)mouseSensitivity == 0.5) {
            mouseSensitivity = 0.47887325F;
         }

         float f1 = mouseSensitivity * 0.6F + 0.2F;
         float f2 = f1 * f1 * f1 * 8.0F;
         int deltaX = (int)((6.667 * (double)yaw - 6.666666666666667 * (double)currentYaw) / (double)f2);
         float f3 = (float)deltaX * f2;
         return (float)((double)currentYaw + (double)f3 * 0.15);
      }
   }

   public float rotateToYaw(float yawSpeed, float[] currentRots, float calcYaw) {
      float yaw = updateRotation(currentRots[0], calcYaw, yawSpeed + RandomUtils.nextFloat(0.0F, 15.0F));
      if (yaw != calcYaw) {
         yaw += (float)((double)RandomUtils.nextFloat(1.0F, 2.0F) * Math.sin((double)currentRots[1] * Math.PI));
      }

      if (yaw == currentRots[0]) {
         return currentRots[0];
      } else {
         float mouseSensitivity = ((Double)Minecraft.getInstance().options.sensitivity().get()).floatValue();
         yaw += (float)(ThreadLocalRandom.current().nextGaussian() * 0.2);
         if ((double)mouseSensitivity == 0.5) {
            mouseSensitivity = 0.47887325F;
         }

         float f1 = mouseSensitivity * 0.6F + 0.2F;
         float f2 = f1 * f1 * f1 * 8.0F;
         int deltaX = (int)((6.667 * (double)yaw - 6.6666667 * (double)currentRots[0]) / (double)f2);
         float f3 = (float)deltaX * f2;
         return (float)((double)currentRots[0] + (double)f3 * 0.15);
      }
   }

   public float rotateToPitch(float pitchSpeed, float currentPitch, float calcPitch) {
      float pitch = updateRotation(currentPitch, calcPitch, pitchSpeed + RandomUtils.nextFloat(0.0F, 15.0F));
      if (pitch != calcPitch) {
         pitch += (float)((double)RandomUtils.nextFloat(1.0F, 2.0F) * Math.sin((double)Minecraft.getInstance().player.getYRot() * Math.PI));
      }

      float mouseSensitivity = ((Double)Minecraft.getInstance().options.sensitivity().get()).floatValue();
      if ((double)mouseSensitivity == 0.5) {
         mouseSensitivity = 0.47887325F;
      }

      float f1 = mouseSensitivity * 0.6F + 0.2F;
      float f2 = f1 * f1 * f1 * 8.0F;
      int deltaY = (int)((6.667 * (double)pitch - 6.666667 * (double)currentPitch) / (double)f2) * -1;
      float f3 = (float)deltaY * f2;
      float f4 = (float)((double)currentPitch - (double)f3 * 0.15);
      return Mth.clamp(f4, -90.0F, 90.0F);
   }

   public float rotateToPitch(float pitchSpeed, float[] currentRots, float calcPitch) {
      float pitch = updateRotation(currentRots[1], calcPitch, pitchSpeed + RandomUtils.nextFloat(0.0F, 15.0F));
      if (pitch != calcPitch) {
         pitch += (float)((double)RandomUtils.nextFloat(1.0F, 2.0F) * Math.sin((double)currentRots[0] * Math.PI));
      }

      float mouseSensitivity = ((Double)Minecraft.getInstance().options.sensitivity().get()).floatValue();
      if ((double)mouseSensitivity == 0.5) {
         mouseSensitivity = 0.47887325F;
      }

      float f1 = mouseSensitivity * 0.6F + 0.2F;
      float f2 = f1 * f1 * f1 * 8.0F;
      int deltaY = (int)((6.667 * (double)pitch - 6.666667 * (double)currentRots[1]) / (double)f2) * -1;
      float f3 = (float)deltaY * f2;
      float f4 = (float)((double)currentRots[1] - (double)f3 * 0.15);
      return Mth.clamp(f4, -90.0F, 90.0F);
   }

   public void set(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public double getDistanceSq() {
      return this.distanceSq;
   }

   public Runnable getTask() {
      return this.task;
   }

   public Runnable getPostTask() {
      return this.postTask;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void setDistanceSq(double distanceSq) {
      this.distanceSq = distanceSq;
   }

   public void setTask(Runnable task) {
      this.task = task;
   }

   public void setPostTask(Runnable postTask) {
      this.postTask = postTask;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Rotation other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
         return false;
      } else if (Float.compare(this.getPitch(), other.getPitch()) != 0) {
         return false;
      } else if (Double.compare(this.getDistanceSq(), other.getDistanceSq()) != 0) {
         return false;
      } else {
         Object this$task = this.getTask();
         Object other$task = other.getTask();
         if (this$task == null ? other$task == null : this$task.equals(other$task)) {
            Object this$postTask = this.getPostTask();
            Object other$postTask = other.getPostTask();
            return this$postTask == null ? other$postTask == null : this$postTask.equals(other$postTask);
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Rotation;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + Float.floatToIntBits(this.getYaw());
      result = result * 59 + Float.floatToIntBits(this.getPitch());
      long $distanceSq = Double.doubleToLongBits(this.getDistanceSq());
      result = result * 59 + (int)($distanceSq >>> 32 ^ $distanceSq);
      Object $task = this.getTask();
      result = result * 59 + ($task == null ? 43 : $task.hashCode());
      Object $postTask = this.getPostTask();
      return result * 59 + ($postTask == null ? 43 : $postTask.hashCode());
   }

   @Override
   public String toString() {
      return "Rotation(yaw="
         + this.getYaw()
         + ", pitch="
         + this.getPitch()
         + ", distanceSq="
         + this.getDistanceSq()
         + ", task="
         + this.getTask()
         + ", postTask="
         + this.getPostTask()
         + ")";
   }
}
