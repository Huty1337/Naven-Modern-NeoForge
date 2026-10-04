package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.files.impl.ProxyFile;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;

@CommandInfo(
   name = "proxy",
   description = "Set client proxy",
   aliases = {"prox"}
)
public class CommandProxy extends Command {
   @Override
   public void onCommand(String[] args) {
      if (args.length == 0) {
         if (ProxyFile.host == null) {
            ChatUtils.addChatMessage("No proxy set.");
         } else {
            ChatUtils.addChatMessage("Current Proxy: " + ProxyFile.host + ":" + ProxyFile.port);
         }
      } else if (args.length == 1) {
         if (args[0].equals("cancel")) {
            ProxyFile.host = null;
            ProxyFile.port = 0;
            ChatUtils.addChatMessage("Proxy cancelled.");
         } else {
            try {
               String[] proxy = args[0].split(":");
               ProxyFile.host = proxy[0];
               ProxyFile.port = Integer.parseInt(proxy[1]);
            } catch (Exception var3) {
               ChatUtils.addChatMessage("Invalid proxy.");
            }
         }
      }
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}
