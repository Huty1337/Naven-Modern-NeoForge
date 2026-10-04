package com.heypixel.heypixelmod.obsoverlay.utils;

import java.util.Arrays;

public class SharedESPData {
   public String displayName;
   public double posX;
   public double posY;
   public double posZ;
   public double health;
   public double maxHealth;
   public double absorption;
   public double[] renderPosition;
   public String[] tags;
   public long updateTime;

   public String getDisplayName() {
      return this.displayName;
   }

   public double getPosX() {
      return this.posX;
   }

   public double getPosY() {
      return this.posY;
   }

   public double getPosZ() {
      return this.posZ;
   }

   public double getHealth() {
      return this.health;
   }

   public double getMaxHealth() {
      return this.maxHealth;
   }

   public double getAbsorption() {
      return this.absorption;
   }

   public double[] getRenderPosition() {
      return this.renderPosition;
   }

   public String[] getTags() {
      return this.tags;
   }

   public long getUpdateTime() {
      return this.updateTime;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   public void setPosX(double posX) {
      this.posX = posX;
   }

   public void setPosY(double posY) {
      this.posY = posY;
   }

   public void setPosZ(double posZ) {
      this.posZ = posZ;
   }

   public void setHealth(double health) {
      this.health = health;
   }

   public void setMaxHealth(double maxHealth) {
      this.maxHealth = maxHealth;
   }

   public void setAbsorption(double absorption) {
      this.absorption = absorption;
   }

   public void setRenderPosition(double[] renderPosition) {
      this.renderPosition = renderPosition;
   }

   public void setTags(String[] tags) {
      this.tags = tags;
   }

   public void setUpdateTime(long updateTime) {
      this.updateTime = updateTime;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof SharedESPData other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (Double.compare(this.getPosX(), other.getPosX()) != 0) {
         return false;
      } else if (Double.compare(this.getPosY(), other.getPosY()) != 0) {
         return false;
      } else if (Double.compare(this.getPosZ(), other.getPosZ()) != 0) {
         return false;
      } else if (Double.compare(this.getHealth(), other.getHealth()) != 0) {
         return false;
      } else if (Double.compare(this.getMaxHealth(), other.getMaxHealth()) != 0) {
         return false;
      } else if (Double.compare(this.getAbsorption(), other.getAbsorption()) != 0) {
         return false;
      } else if (this.getUpdateTime() != other.getUpdateTime()) {
         return false;
      } else {
         Object this$displayName = this.getDisplayName();
         Object other$displayName = other.getDisplayName();
         if (this$displayName == null ? other$displayName == null : this$displayName.equals(other$displayName)) {
            return !Arrays.equals(this.getRenderPosition(), other.getRenderPosition()) ? false : Arrays.deepEquals(this.getTags(), other.getTags());
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof SharedESPData;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      long $posX = Double.doubleToLongBits(this.getPosX());
      result = result * 59 + (int)($posX >>> 32 ^ $posX);
      long $posY = Double.doubleToLongBits(this.getPosY());
      result = result * 59 + (int)($posY >>> 32 ^ $posY);
      long $posZ = Double.doubleToLongBits(this.getPosZ());
      result = result * 59 + (int)($posZ >>> 32 ^ $posZ);
      long $health = Double.doubleToLongBits(this.getHealth());
      result = result * 59 + (int)($health >>> 32 ^ $health);
      long $maxHealth = Double.doubleToLongBits(this.getMaxHealth());
      result = result * 59 + (int)($maxHealth >>> 32 ^ $maxHealth);
      long $absorption = Double.doubleToLongBits(this.getAbsorption());
      result = result * 59 + (int)($absorption >>> 32 ^ $absorption);
      long $updateTime = this.getUpdateTime();
      result = result * 59 + (int)($updateTime >>> 32 ^ $updateTime);
      Object $displayName = this.getDisplayName();
      result = result * 59 + ($displayName == null ? 43 : $displayName.hashCode());
      result = result * 59 + Arrays.hashCode(this.getRenderPosition());
      return result * 59 + Arrays.deepHashCode(this.getTags());
   }

   @Override
   public String toString() {
      return "SharedESPData(displayName="
         + this.getDisplayName()
         + ", posX="
         + this.getPosX()
         + ", posY="
         + this.getPosY()
         + ", posZ="
         + this.getPosZ()
         + ", health="
         + this.getHealth()
         + ", maxHealth="
         + this.getMaxHealth()
         + ", absorption="
         + this.getAbsorption()
         + ", renderPosition="
         + Arrays.toString(this.getRenderPosition())
         + ", tags="
         + Arrays.deepToString(this.getTags())
         + ", updateTime="
         + this.getUpdateTime()
         + ")";
   }

   public SharedESPData(
      String displayName,
      double posX,
      double posY,
      double posZ,
      double health,
      double maxHealth,
      double absorption,
      double[] renderPosition,
      String[] tags,
      long updateTime
   ) {
      this.displayName = displayName;
      this.posX = posX;
      this.posY = posY;
      this.posZ = posZ;
      this.health = health;
      this.maxHealth = maxHealth;
      this.absorption = absorption;
      this.renderPosition = renderPosition;
      this.tags = tags;
      this.updateTime = updateTime;
   }
}
