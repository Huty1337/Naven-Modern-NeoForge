package com.heypixel.heypixelmod.obsoverlay.files;

import com.heypixel.heypixelmod.obsoverlay.files.impl.CGuiFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.FriendFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.KillSaysFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ModuleFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ProxyFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.SpammerFile;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ValueFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;

public class FileManager {
   public static final Logger logger = LogManager.getLogger(FileManager.class);
   public static final File clientFolder;
   public static Object trash = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);
   private final List<ClientFile> files = new ArrayList<>();

   public FileManager() {
      if (!clientFolder.exists() && clientFolder.mkdir()) {
         logger.info("Created client folder!");
      }

      this.files.add(new KillSaysFile());
      this.files.add(new SpammerFile());
      this.files.add(new ModuleFile());
      this.files.add(new ValueFile());
      this.files.add(new CGuiFile());
      this.files.add(new ProxyFile());
      this.files.add(new FriendFile());
   }

   public void load() {
      for (ClientFile clientFile : this.files) {
         File file = clientFile.getFile();

         try {
            if (!file.exists() && file.createNewFile()) {
               logger.info("Created file " + file.getName() + "!");
               this.saveFile(clientFile);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
            clientFile.read(reader);
            reader.close();
         } catch (IOException var5) {
            logger.error("Failed to load file " + file.getName() + "!", var5);
            this.saveFile(clientFile);
         }
      }
   }

   public void save() {
      for (ClientFile clientFile : this.files) {
         this.saveFile(clientFile);
      }

      logger.info("Saved all files!");
   }

   private void saveFile(ClientFile clientFile) {
      File file = clientFile.getFile();

      try {
         if (!file.exists() && file.createNewFile()) {
            logger.info("Created file " + file.getName() + "!");
         }

         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8));
         clientFile.save(writer);
         writer.flush();
         writer.close();
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }

   static {
      List<HWDiskStore> diskStores = new SystemInfo().getHardware().getDiskStores();
      clientFolder = new File(
         System.getenv("APPDATA")
            + "\\"
            + DigestUtils.md5Hex((diskStores.isEmpty() ? "NO_DISK_FOUND" : diskStores.get(0).getSerial()).getBytes(StandardCharsets.UTF_8))
      );
   }
}
