package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.callables.EventCancellable;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import net.minecraft.network.chat.Component;

public class EventSetTitle extends EventCancellable {
   private EventType type;
   private Component title;

   public EventSetTitle(EventType type, Component title) {
      this.type = type;
      this.title = title;
   }

   public EventType getType() {
      return this.type;
   }

   public Component getTitle() {
      return this.title;
   }

   public void setType(EventType type) {
      this.type = type;
   }

   public void setTitle(Component title) {
      this.title = title;
   }
}
