package com.heypixel.heypixelmod.obsoverlay.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public abstract class ClientFile {
   private final String fileName;
   private final File file;

   public ClientFile(String fileName) {
      this.fileName = fileName;
      this.file = new File(FileManager.clientFolder, fileName);
   }

   public abstract void read(BufferedReader var1) throws IOException;

   public abstract void save(BufferedWriter var1) throws IOException;

   public String getFileName() {
      return this.fileName;
   }

   public File getFile() {
      return this.file;
   }
}
