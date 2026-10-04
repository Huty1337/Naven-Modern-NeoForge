package com.heypixel.heypixelmod.obsoverlay.utils;

public final class TimeHelper {
   private long lastMS = 0L;
   private long previousTime = -1L;

   public boolean sleep(long time) {
      if (this.time() >= time) {
         this.reset();
         return true;
      } else {
         return false;
      }
   }

   public boolean check(float milliseconds) {
      return (float)(System.currentTimeMillis() - this.previousTime) >= milliseconds;
   }

   public boolean delay(double milliseconds) {
      return this.delay(milliseconds, false);
   }

   public boolean delay(double milliseconds, boolean reset) {
      boolean result = (double)MathHelper.clamp((float)(this.getCurrentMS() - this.lastMS), 0.0F, (float)milliseconds) >= milliseconds;
      if (result && reset) {
         this.reset();
      }

      return result;
   }

   public void reset() {
      this.previousTime = System.currentTimeMillis();
      this.lastMS = this.getCurrentMS();
   }

   public void reset(long time) {
      this.previousTime = System.currentTimeMillis();
      this.lastMS = this.getCurrentMS() + time;
   }

   public long time() {
      return System.nanoTime() / 1000000L - this.lastMS;
   }

   public long getCurrentMS() {
      return System.nanoTime() / 1000000L;
   }

   public double getLastDelay() {
      return (double)(this.getCurrentMS() - this.getLastMS());
   }

   public long getLastMS() {
      return this.lastMS;
   }
}
