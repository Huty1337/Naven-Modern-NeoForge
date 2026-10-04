package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import net.minecraft.network.chat.Component;

public class EventRenderScoreboard implements Event {
   private Component component;

   public EventRenderScoreboard(Component component) {
      this.component = component;
   }

   public Component getComponent() {
      return this.component;
   }

   public void setComponent(Component component) {
      this.component = component;
   }
}
