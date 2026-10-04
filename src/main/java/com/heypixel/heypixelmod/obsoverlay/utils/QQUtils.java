package com.heypixel.heypixelmod.obsoverlay.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class QQUtils {
   private static final byte[] header = new byte[]{-1, -40, -1, -32, 0, 16, 74, 70, 73, 70};

   public static Set<String> getAllQQ() throws IOException {
      Set<String> qqs = new HashSet<>();
      if (System.getProperty("os.name").toLowerCase().contains("windows")) {
         File ntQQPath = new File(System.getenv("APPDATA") + "\\Tencent\\QQ\\Misc");
         if (ntQQPath.exists() && ntQQPath.isDirectory()) {
            File[] files = ntQQPath.listFiles();
            if (files != null) {
               for (File file : files) {
                  if (!file.isDirectory()) {
                     String fileName = file.getName();
                     if (fileName.matches("[0-9]+") && fileName.length() >= 5 && fileName.length() <= 10 && checkNTQQFile(file)) {
                        qqs.add(fileName);
                     }
                  }
               }
            }
         }

         File legacyQQPath = new File(System.getenv("PUBLIC") + "\\Documents\\Tencent\\QQ\\UserDataInfo.ini");
         if (legacyQQPath.exists() && legacyQQPath.isFile()) {
            BufferedReader stream = new BufferedReader(new InputStreamReader(Files.newInputStream(legacyQQPath.toPath())));

            String qq;
            while ((qq = stream.readLine()) != null && !qq.isEmpty()) {
               if (qq.startsWith("UserDataSavePath=")) {
                  File tencentFiles = new File(qq.split("=")[1]);
                  if (tencentFiles.exists() && tencentFiles.isDirectory()) {
                     for (File qqData : Objects.requireNonNull(tencentFiles.listFiles())) {
                        if (qqData.isDirectory() && qqData.getName().length() >= 6 && qqData.getName().length() <= 10 && qqData.getName().matches("^[0-9]*$")) {
                           qqs.add(qqData.getName());
                        }
                     }
                  }
               }
            }
         }
      }

      return qqs;
   }

   private static boolean checkNTQQFile(File file) {
      try {
         boolean var3;
         try (FileInputStream stream = new FileInputStream(file)) {
            byte[] header = new byte[10];
            if (stream.read(header) != 0) {
               return Arrays.equals(header, QQUtils.header);
            }

            var3 = false;
         }

         return var3;
      } catch (Exception var6) {
         return false;
      }
   }
}
