package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.files.FileManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtils {
   private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   private static final BufferedWriter antiBotsLog;
   private static final BufferedWriter playerInfo;

   public static void antiBot(String message) {
      try {
         antiBotsLog.write("[%s] %s\n".formatted(format.format(new Date()), message));
         antiBotsLog.flush();
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      }
   }

   public static void playerInfo(String message) {
      try {
         playerInfo.write("[%s] %s\n".formatted(format.format(new Date()), message));
         playerInfo.flush();
      } catch (IOException var2) {
         throw new RuntimeException(var2);
      }
   }

   public static void close() {
      try {
         antiBotsLog.close();
         playerInfo.close();
      } catch (IOException var1) {
         throw new RuntimeException(var1);
      }
   }

   static {
      try {
         antiBotsLog = new BufferedWriter(new FileWriter(new File(FileManager.clientFolder, "antibots.log")));
         playerInfo = new BufferedWriter(new FileWriter(new File(FileManager.clientFolder, "playerinfo.log")));
      } catch (IOException var1) {
         throw new RuntimeException(var1);
      }
   }
}
