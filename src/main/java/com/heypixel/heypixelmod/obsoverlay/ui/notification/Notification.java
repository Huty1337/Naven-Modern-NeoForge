package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import java.awt.Color;

public class Notification {
   public static byte[] authTokens;
   private NotificationLevel level;
   private String message;
   private long maxAge;
   private long createTime = System.currentTimeMillis();
   private SmoothAnimationTimer widthTimer = new SmoothAnimationTimer(0.0F);
   private SmoothAnimationTimer heightTimer = new SmoothAnimationTimer(0.0F);

   public Notification(NotificationLevel level, String message, long age) {
      this.level = level;
      this.message = message;
      this.maxAge = age;
   }

   
   
   public float getWidth() {
      float stringWidth = Fonts.harmony.getWidth(this.message, 0.35);
      return stringWidth + 12.0F;
   }

   public float getHeight() {
      return 24.0F;
   }

   public NotificationLevel getLevel() {
      return this.level;
   }

   public String getMessage() {
      return this.message;
   }

   public long getMaxAge() {
      return this.maxAge;
   }

   public long getCreateTime() {
      return this.createTime;
   }

   public SmoothAnimationTimer getWidthTimer() {
      return this.widthTimer;
   }

   public SmoothAnimationTimer getHeightTimer() {
      return this.heightTimer;
   }

   public void setLevel(NotificationLevel level) {
      this.level = level;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public void setMaxAge(long maxAge) {
      this.maxAge = maxAge;
   }

   public void setCreateTime(long createTime) {
      this.createTime = createTime;
   }

   public void setWidthTimer(SmoothAnimationTimer widthTimer) {
      this.widthTimer = widthTimer;
   }

   public void setHeightTimer(SmoothAnimationTimer heightTimer) {
      this.heightTimer = heightTimer;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Notification other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (this.getMaxAge() != other.getMaxAge()) {
         return false;
      } else if (this.getCreateTime() != other.getCreateTime()) {
         return false;
      } else {
         Object this$level = this.getLevel();
         Object other$level = other.getLevel();
         if (this$level == null ? other$level == null : this$level.equals(other$level)) {
            Object this$message = this.getMessage();
            Object other$message = other.getMessage();
            if (this$message == null ? other$message == null : this$message.equals(other$message)) {
               Object this$widthTimer = this.getWidthTimer();
               Object other$widthTimer = other.getWidthTimer();
               if (this$widthTimer == null ? other$widthTimer == null : this$widthTimer.equals(other$widthTimer)) {
                  Object this$heightTimer = this.getHeightTimer();
                  Object other$heightTimer = other.getHeightTimer();
                  return this$heightTimer == null ? other$heightTimer == null : this$heightTimer.equals(other$heightTimer);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Notification;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      long $maxAge = this.getMaxAge();
      result = result * 59 + (int)($maxAge >>> 32 ^ $maxAge);
      long $createTime = this.getCreateTime();
      result = result * 59 + (int)($createTime >>> 32 ^ $createTime);
      Object $level = this.getLevel();
      result = result * 59 + ($level == null ? 43 : $level.hashCode());
      Object $message = this.getMessage();
      result = result * 59 + ($message == null ? 43 : $message.hashCode());
      Object $widthTimer = this.getWidthTimer();
      result = result * 59 + ($widthTimer == null ? 43 : $widthTimer.hashCode());
      Object $heightTimer = this.getHeightTimer();
      return result * 59 + ($heightTimer == null ? 43 : $heightTimer.hashCode());
   }

   @Override
   public String toString() {
      return "Notification(level="
         + this.getLevel()
         + ", message="
         + this.getMessage()
         + ", maxAge="
         + this.getMaxAge()
         + ", createTime="
         + this.getCreateTime()
         + ", widthTimer="
         + this.getWidthTimer()
         + ", heightTimer="
         + this.getHeightTimer()
         + ")";
   }
}
