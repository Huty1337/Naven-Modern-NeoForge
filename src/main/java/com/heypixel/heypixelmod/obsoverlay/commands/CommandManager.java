package com.heypixel.heypixelmod.obsoverlay.commands;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.commands.impl.CommandBind;
import com.heypixel.heypixelmod.obsoverlay.commands.impl.CommandConfig;
import com.heypixel.heypixelmod.obsoverlay.commands.impl.CommandLanguage;
import com.heypixel.heypixelmod.obsoverlay.commands.impl.CommandProxy;
import com.heypixel.heypixelmod.obsoverlay.commands.impl.CommandToggle;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClientChat;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
   public static final String PREFIX = ".";
   public final Map<String, Command> aliasMap = new HashMap<>();

   public CommandManager() {
      try {
         this.initCommands();
      } catch (Exception var2) {
         throw new RuntimeException(var2);
      }

      Naven.getInstance().getEventManager().register(this);
   }

   private void initCommands() {
      this.registerCommand(new CommandBind());
      this.registerCommand(new CommandToggle());
      this.registerCommand(new CommandConfig());
      this.registerCommand(new CommandLanguage());
      this.registerCommand(new CommandProxy());
   }

   private void registerCommand(Command command) {
      command.initCommand();
      this.aliasMap.put(command.getName().toLowerCase(), command);

      for (String alias : command.getAliases()) {
         this.aliasMap.put(alias.toLowerCase(), command);
      }
   }

   @EventTarget
   public void onChat(EventClientChat e) {
      if (e.getMessage().startsWith(".")) {
         e.setCancelled(true);
         String chatMessage = e.getMessage().substring(".".length());
         String[] arguments = chatMessage.split(" ");
         if (arguments.length < 1) {
            ChatUtils.addChatMessage("Invalid command.");
            return;
         }

         String alias = arguments[0].toLowerCase();
         Command command = this.aliasMap.get(alias);
         if (command == null) {
            ChatUtils.addChatMessage("Invalid command.");
            return;
         }

         String[] args = new String[arguments.length - 1];
         System.arraycopy(arguments, 1, args, 0, args.length);
         command.onCommand(args);
      }
   }
}
