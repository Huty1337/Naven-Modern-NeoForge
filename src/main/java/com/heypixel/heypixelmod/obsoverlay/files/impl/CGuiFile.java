package com.heypixel.heypixelmod.obsoverlay.files.impl;

import com.heypixel.heypixelmod.obsoverlay.files.ClientFile;
import com.heypixel.heypixelmod.obsoverlay.ui.ClickGUI;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class CGuiFile extends ClientFile {
   public CGuiFile() {
      super("clickgui.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      try {
         ClickGUI.windowX = (float)Integer.parseInt(reader.readLine());
         ClickGUI.windowY = (float)Integer.parseInt(reader.readLine());
         ClickGUI.windowWidth = (float)Integer.parseInt(reader.readLine());
         ClickGUI.windowHeight = (float)Integer.parseInt(reader.readLine());
      } catch (Exception var3) {
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      writer.write((int)ClickGUI.windowX + "\n");
      writer.write((int)ClickGUI.windowY + "\n");
      writer.write((int)ClickGUI.windowWidth + "\n");
      writer.write((int)ClickGUI.windowHeight + "\n");
   }
}
