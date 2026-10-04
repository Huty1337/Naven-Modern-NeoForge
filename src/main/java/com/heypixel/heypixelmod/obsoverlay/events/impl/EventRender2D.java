package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public class EventRender2D implements Event {
   private final PoseStack stack;
   private final GuiGraphics guiGraphics;

   public EventRender2D(PoseStack stack, GuiGraphics guiGraphics) {
      this.stack = stack;
      this.guiGraphics = guiGraphics;
   }

   public PoseStack getStack() {
      return this.stack;
   }

   public GuiGraphics getGuiGraphics() {
      return this.guiGraphics;
   }
}
