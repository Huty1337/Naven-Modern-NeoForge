package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Base64ImageLocation {
   private static final Logger log = LogManager.getLogger(Base64ImageLocation.class);
   private static int seed = 0;
   private final String base64;
   private ResourceLocation resourceLocation;
   public boolean failed = false;

   public Base64ImageLocation(String base64) {
      this.base64 = base64;
   }

   public String getBase64() {
      return this.base64;
   }
}
