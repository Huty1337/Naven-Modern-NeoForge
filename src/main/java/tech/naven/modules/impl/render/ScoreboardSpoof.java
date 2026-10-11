package tech.naven.modules.impl.render;

import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventRenderScoreboard;
import tech.naven.events.impl.EventRenderTabOverlay;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.utils.TextReplacer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@ModuleInfo(
   name = "ServerNameSpoof",
   description = "Spoof the server name",
   category = Category.RENDER
)
public class ScoreboardSpoof extends Module {
   public static final String SERVER_NAME = "布吉岛";
   private static final String SERVER_SPOOF_TEXT = "§d§l吉吉岛";
   private static final String SERVER_SPOOF_PLAIN = "吉吉岛";

   @Override
   public void onEnable() {
      TextReplacer.setServer(SERVER_NAME, SERVER_SPOOF_PLAIN, SERVER_SPOOF_TEXT);
   }

   @Override
   public void onDisable() {
      TextReplacer.setServer(null, null, null);
   }

   @EventTarget
   public void onRenderScoreboard(EventRenderScoreboard e) {
      String string = e.getComponent().getString();
      if (string.contains(SERVER_NAME)) {
         MutableComponent textComponent = Component.literal(SERVER_SPOOF_TEXT);
         textComponent.setStyle(e.getComponent().getStyle());
         e.setComponent(textComponent);
      }
   }

   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      String string = e.getComponent().getString();
      if (string.contains(SERVER_NAME)) {
         if (e.getType() == EventType.HEADER) {
            e.setComponent(Component.literal(SERVER_SPOOF_TEXT));
         } else if (e.getType() == EventType.FOOTER) {
            e.setComponent(Component.literal(""));
         }
      }
   }
}
