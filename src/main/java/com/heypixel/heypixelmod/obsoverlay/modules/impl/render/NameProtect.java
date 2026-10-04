package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderScoreboard;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSetTitle;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.mojang.serialization.JsonOps;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.Entity;

@ModuleInfo(
   name = "NameProtect",
   description = "Protect your name",
   category = Category.RENDER
)
public class NameProtect extends Module {
   public static NameProtect instance;

   private static final String REPLACEMENT_TEXT = "§dHidden§f";
   private static final String REPLACEMENT_COLOR = "light_purple";

   public NameProtect() {
      instance = this;
   }

   public static String getName(String string) {
      if (!instance.isEnabled() || mc.player == null) {
         return string;
      } else {
         String playerName = mc.player.getName().getString();
         if (!string.contains(playerName)) {
            return string;
         }
         return string.replace(playerName, REPLACEMENT_TEXT);
      }
   }

   public static Component getNameJson(Component in) {
      if (in == null || instance == null || !instance.isEnabled() || mc.player == null) {
         return in;
      }

      JsonElement root = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, in).result().orElse(null);
      if (root == null) {
         return in;
      }

      String json = root.toString();
      String replaced = getName(json);
      if (replaced.equals(json)) {
         return in;
      }

      JsonElement patched;
      try {
         patched = JsonParser.parseString(replaced);
      } catch (JsonParseException e) {
         return in;
      }

      Component out = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, patched).result().orElse(null);
      return out == null ? in : out;
   }

   public static Component hookNameTag(Entity entity, Component in) {
      if (in == null) {
         return null;
      }
      NameProtect nameProtect = instance;
      if (nameProtect == null || !nameProtect.isEnabled()) {
         return in;
      }
      if (mc != null && mc.player != null && entity == mc.player) {
         return Component.literal("§dHidden§f");
      }
      return protect(in);
   }

   private static JsonObject textElement(String text) {
      JsonObject obj = new JsonObject();
      obj.addProperty("text", text);
      return obj;
   }

   private static JsonObject coloredTextElement(String text, String colorName) {
      JsonObject obj = new JsonObject();
      obj.addProperty("text", text);
      obj.addProperty("color", colorName);
      return obj;
   }

   private static boolean replaceInJson(JsonElement element, Pattern pattern, String replacementText, String replacementColor) {
      if (element == null || element.isJsonNull()) {
         return false;
      }

      boolean changed = false;

      if (element.isJsonArray()) {
         JsonArray array = element.getAsJsonArray();
         for (JsonElement child : array) {
            changed |= replaceInJson(child, pattern, replacementText, replacementColor);
         }
         return changed;
      }

      if (!element.isJsonObject()) {
         return false;
      }

      JsonObject obj = element.getAsJsonObject();

      if (obj.has("extra") && obj.get("extra").isJsonArray()) {
         JsonArray extra = obj.getAsJsonArray("extra");
         for (JsonElement child : extra) {
            changed |= replaceInJson(child, pattern, replacementText, replacementColor);
         }
      }

      if (obj.has("with") && obj.get("with").isJsonArray()) {
         JsonArray with = obj.getAsJsonArray("with");
         for (JsonElement child : with) {
            changed |= replaceInJson(child, pattern, replacementText, replacementColor);
         }
      }

      if (!obj.has("text") || !obj.get("text").isJsonPrimitive()) {
         return changed;
      }

      String text = obj.get("text").getAsString();
      var matcher = pattern.matcher(text);
      if (!matcher.find()) {
         return changed;
      }

      JsonArray existingExtra = null;
      if (obj.has("extra") && obj.get("extra").isJsonArray()) {
         existingExtra = obj.getAsJsonArray("extra");
      }

      JsonArray newExtra = new JsonArray();
      int last = 0;

      String firstText = text.substring(0, matcher.start());
      obj.addProperty("text", firstText);

      newExtra.add(coloredTextElement(replacementText, replacementColor));
      last = matcher.end();
      while (matcher.find()) {
         String between = text.substring(last, matcher.start());
         if (!between.isEmpty()) {
            newExtra.add(textElement(between));
         }
         newExtra.add(coloredTextElement(replacementText, replacementColor));
         last = matcher.end();
      }

      String tail = text.substring(last);
      if (!tail.isEmpty()) {
         newExtra.add(textElement(tail));
      }

      if (existingExtra != null) {
         for (JsonElement child : existingExtra) {
            newExtra.add(child);
         }
      }

      obj.add("extra", newExtra);
      return true;
   }

   public static Component protect(Component in) {
      if (in == null || instance == null || !instance.isEnabled() || mc.player == null) {
         return in;
      }

      String playerName = mc.player.getName().getString();
      if (playerName.isEmpty() || !in.getString().contains(playerName)) {
         return in;
      }

      Pattern pattern = Pattern.compile(Pattern.quote(playerName));
      JsonElement root = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, in).result().orElse(null);
      if (root == null) {
         return in;
      }

      root = normalizeTextElement(root);
      boolean changed = replaceInJson(root, pattern, REPLACEMENT_TEXT, REPLACEMENT_COLOR);
      if (!changed) {
         return in;
      }

      Component out = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, root).result().orElse(null);
      return out == null ? in : out;
   }

   private static JsonElement normalizeTextElement(JsonElement element) {
      if (element == null || element.isJsonNull()) {
         return element;
      }

      if (element.isJsonPrimitive()) {
         return textElement(element.getAsString());
      }

      if (element.isJsonArray()) {
         JsonArray array = element.getAsJsonArray();
         for (int i = 0; i < array.size(); i++) {
            array.set(i, normalizeTextElement(array.get(i)));
         }
         return array;
      }

      JsonObject obj = element.getAsJsonObject();
      normalizeChildren(obj, "extra");
      normalizeChildren(obj, "with");
      return obj;
   }

   private static void normalizeChildren(JsonObject obj, String name) {
      if (obj.has(name) && obj.get(name).isJsonArray()) {
         JsonArray array = obj.getAsJsonArray(name);
         for (int i = 0; i < array.size(); i++) {
            array.set(i, normalizeTextElement(array.get(i)));
         }
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      Component in = e.getComponent();
      Component out = protect(in);
      if (out != in) {
         e.setComponent(out);
      }
   }

   @EventTarget
   public void onRenderScoreboard(EventRenderScoreboard e) {
      Component in = e.getComponent();
      Component out = protect(in);
      if (out != in) {
         e.setComponent(out);
      }
   }

   @EventTarget
   public void onSetTitle(EventSetTitle e) {
      if (e.getType() != EventType.TITLE && e.getType() != EventType.SUBTITLE) {
         return;
      }

      Component in = e.getTitle();
      Component out = protect(in);
      if (out != in) {
         e.setTitle(out);
      }
   }
}
