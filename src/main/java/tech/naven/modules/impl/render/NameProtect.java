package tech.naven.modules.impl.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventRenderScoreboard;
import tech.naven.events.impl.EventRenderTabOverlay;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.events.impl.EventSetTitle;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.utils.TextReplacer;
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
   private static final String REPLACEMENT_PLAIN = "Hidden";

   private static String lastPlayerName;

   public NameProtect() {
      instance = this;
   }

   @EventTarget(4)
   public void onTick(EventRunTicks event) {
      if (event.getType() == EventType.PRE) {
         this.refresh();
      }
   }

   @Override
   public void onEnable() {
      this.refresh();
   }

   @Override
   public void onDisable() {
      lastPlayerName = null;
      TextReplacer.setName(null, null, null);
   }

   private void refresh() {
      if (!this.isEnabled() || mc.player == null) {
         return;
      }

      String playerName = mc.player.getName().getString();
      if (playerName.isEmpty() || playerName.equals(lastPlayerName)) {
         return;
      }

      lastPlayerName = playerName;
      if (REPLACEMENT_PLAIN.contains(playerName)) {
         TextReplacer.setName(null, null, null);
         return;
      }

      TextReplacer.setName(playerName, REPLACEMENT_PLAIN, REPLACEMENT_TEXT);
   }

   public static String getName(String string) {
      return TextReplacer.replace(string);
   }

   public static boolean active() {
      return instance != null && instance.isEnabled() && !TextReplacer.isEmpty();
   }

   public static Component hookNameTag(Entity entity, Component in) {
      if (in == null || instance == null || !instance.isEnabled()) {
         return in;
      }

      if (mc != null && mc.player != null && entity == mc.player) {
         return Component.literal(REPLACEMENT_TEXT);
      }

      String flat = in.getString();
      if (!TextReplacer.contains(flat)) {
         return in;
      }

      return Component.literal(TextReplacer.replace(flat)).setStyle(in.getStyle());
   }

   public static Component rewriteComponent(Component in) {
      if (in == null || TextReplacer.isEmpty()) {
         return in;
      }

      if (!TextReplacer.contains(in.getString())) {
         return in;
      }

      JsonElement root = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, in).result().orElse(null);
      if (root == null) {
         return in;
      }

      root = normalizeTextElement(root);
      boolean changed = false;
      String nameFrom = TextReplacer.getNameFrom();
      if (nameFrom != null) {
         changed = replaceInJson(root, nameFrom, REPLACEMENT_PLAIN, "light_purple");
      }

      String serverFrom = TextReplacer.getServerFrom();
      if (serverFrom != null) {
         changed |= replaceInJson(root, serverFrom, TextReplacer.getServerToPlain(), "light_purple");
      }

      if (!changed) {
         return in;
      }

      Component out = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, root).result().orElse(null);
      return out == null ? in : out;
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

   private static boolean replaceInJson(JsonElement element, String from, String replacementText, String replacementColor) {
      if (element == null || element.isJsonNull()) {
         return false;
      }

      boolean changed = false;
      if (element.isJsonArray()) {
         for (JsonElement child : element.getAsJsonArray()) {
            changed |= replaceInJson(child, from, replacementText, replacementColor);
         }
         return changed;
      }

      if (!element.isJsonObject()) {
         return false;
      }

      JsonObject obj = element.getAsJsonObject();
      if (obj.has("extra") && obj.get("extra").isJsonArray()) {
         for (JsonElement child : obj.getAsJsonArray("extra")) {
            changed |= replaceInJson(child, from, replacementText, replacementColor);
         }
      }

      if (obj.has("with") && obj.get("with").isJsonArray()) {
         for (JsonElement child : obj.getAsJsonArray("with")) {
            changed |= replaceInJson(child, from, replacementText, replacementColor);
         }
      }

      if (!obj.has("text") || !obj.get("text").isJsonPrimitive()) {
         return changed;
      }

      String text = obj.get("text").getAsString();
      int hit = text.indexOf(from);
      if (hit < 0) {
         return changed;
      }

      JsonArray existingExtra = obj.has("extra") && obj.get("extra").isJsonArray() ? obj.getAsJsonArray("extra") : null;
      JsonArray newExtra = new JsonArray();
      obj.addProperty("text", text.substring(0, hit));
      newExtra.add(coloredTextElement(replacementText, replacementColor));
      int last = hit + from.length();
      int next;
      while ((next = text.indexOf(from, last)) >= 0) {
         String between = text.substring(last, next);
         if (!between.isEmpty()) {
            newExtra.add(textElement(between));
         }

         newExtra.add(coloredTextElement(replacementText, replacementColor));
         last = next + from.length();
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

   private static Component flatReplace(Component in) {
      if (in == null || instance == null || !instance.isEnabled()) {
         return in;
      }

      String flat = in.getString();
      if (!TextReplacer.contains(flat)) {
         return in;
      }

      return Component.literal(TextReplacer.replace(flat)).setStyle(in.getStyle());
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      Component out = flatReplace(e.getComponent());
      if (out != e.getComponent()) {
         e.setComponent(out);
      }
   }

   @EventTarget
   public void onRenderScoreboard(EventRenderScoreboard e) {
      Component out = flatReplace(e.getComponent());
      if (out != e.getComponent()) {
         e.setComponent(out);
      }
   }

   @EventTarget
   public void onSetTitle(EventSetTitle e) {
      if (e.getType() != EventType.TITLE && e.getType() != EventType.SUBTITLE) {
         return;
      }

      Component out = flatReplace(e.getTitle());
      if (out != e.getTitle()) {
         e.setTitle(out);
      }
   }
}
