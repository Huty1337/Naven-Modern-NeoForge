package com.heypixel.heypixelmod.obsoverlay.files.impl;

import com.heypixel.heypixelmod.obsoverlay.files.ClientFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProxyFile extends ClientFile {
   private static final Logger log = LogManager.getLogger(ProxyFile.class);
   public static String host = null;
   public static int port = 0;

   public ProxyFile() {
      super("proxy.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      try {
         String proxy = reader.readLine();
         if (proxy != null && proxy.length() > 8) {
            String[] split = proxy.split(":", 2);
            host = split[0];
            port = Integer.parseInt(split[1]);
            log.info("Proxy: {}:{}", host, port);
         }
      } catch (Exception var4) {
         log.error("Failed to read proxy file!", var4);
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      writer.write(host + ":" + port);
   }
}
