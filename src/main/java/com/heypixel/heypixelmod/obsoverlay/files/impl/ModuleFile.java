package com.heypixel.heypixelmod.obsoverlay.files.impl;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.exceptions.NoSuchModuleException;
import com.heypixel.heypixelmod.obsoverlay.files.ClientFile;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModuleFile extends ClientFile {
   private static final Logger logger = LogManager.getLogger(ModuleFile.class);

   public ModuleFile() {
      super("modules.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      ModuleManager moduleManager = Naven.getInstance().getModuleManager();

      String line;
      while ((line = reader.readLine()) != null) {
         String[] split = line.split(":", 3);
         if (split.length != 3) {
            logger.error("Failed to read line {}!", line);
         } else {
            String name = split[0];
            int key = Integer.parseInt(split[1]);
            boolean enabled = Boolean.parseBoolean(split[2]);

            try {
               Module module = moduleManager.getModule(name);
               module.setKey(key);
               module.setEnabled(enabled);
            } catch (NoSuchModuleException var9) {
               logger.error("Failed to find module {}!", name);
            }
         }
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      ModuleManager moduleManager = Naven.getInstance().getModuleManager();

      for (Module module : new ArrayList<>(moduleManager.getModules())) {
         writer.write(String.format("%s:%d:%s\n", module.getName(), module.getKey(), module.isEnabled()));
      }
   }
}
