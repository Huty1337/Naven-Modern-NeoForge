package com.heypixel.heypixelmod.obsoverlay.ui;

import com.mojang.blaze3d.vertex.PoseStack;

public final class GuiRenderCompat {
   private GuiRenderCompat() {
   }

   public static PoseStack poseStack() {
      return new PoseStack();
   }
}
