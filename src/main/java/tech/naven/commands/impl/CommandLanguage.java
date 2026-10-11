package tech.naven.commands.impl;

import tech.naven.Naven;
import tech.naven.commands.Command;
import tech.naven.commands.CommandInfo;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;

@CommandInfo(
   name = "language",
   description = "Open language gui.",
   aliases = {"lang"}
)
public class CommandLanguage extends Command {
   @Override
   public void onCommand(String[] args) {
      Naven.getInstance().getEventManager().register(new Object() {
         @EventTarget
         public void onMotion(EventMotion e) {
            if (e.getType() == EventType.PRE) {
               Minecraft.getInstance().setScreen(new LanguageSelectScreen(null, Minecraft.getInstance().options, Minecraft.getInstance().getLanguageManager()));
               Naven.getInstance().getEventManager().unregister(this);
            }
         }
      });
   }

   @Override
   public String[] onTab(String[] args) {
      return new String[0];
   }
}
