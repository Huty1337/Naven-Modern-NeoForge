package com.heypixel.heypixelmod.obsoverlay.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

public class ChatUtils {
   private static final String PREFIX = "§7[§b" + "Naven".charAt(0) + "§7] ";

   public static void component(Component component) {
      ChatComponent chat = Minecraft.getInstance().gui.getChat();
      chat.addMessage(component);
   }

   public static void addChatMessage(String message) {
      addChatMessage(true, message);
   }

   public static void addChatMessage(boolean prefix, String message) {
      component(Component.nullToEmpty((prefix ? PREFIX : "") + message));
   }
}
