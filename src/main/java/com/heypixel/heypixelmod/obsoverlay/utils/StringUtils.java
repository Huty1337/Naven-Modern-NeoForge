package com.heypixel.heypixelmod.obsoverlay.utils;

public class StringUtils {
   public static boolean containChinese(String str) {
      if (str != null && !str.isEmpty()) {
         for (char c : str.toCharArray()) {
            if (c > 19968) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
