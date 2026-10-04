package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.mojang.blaze3d.platform.Window;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public class NotificationManager {
   private final List<Notification> notifications = new CopyOnWriteArrayList<>();

   public void addNotification(Notification notification) {
      if (!this.notifications.contains(notification)) {
         this.notifications.add(notification);
      }
   }

   public void onRenderShadow(EventShader e) {
      Window window = Minecraft.getInstance().getWindow();
      float screenW = (float)window.getGuiScaledWidth();
      float screenH = (float)window.getGuiScaledHeight();

      for (Notification notification : this.notifications) {
         float x = screenW - notification.getWidthTimer().value + 2.0F;
         float y = screenH - notification.getHeightTimer().value;
         RenderUtils.drawRoundedRect(
            e.getStack(), x + 2.0F, y + 4.0F, notification.getWidth(), 20.0F, 5.0F, notification.getLevel().getColor()
         );
      }
   }

   public void onRender(EventRender2D e) {
      GuiGraphics guiGraphics = e.getGuiGraphics();
      float height = 5.0F;

      for (Notification notification : this.notifications) {
         float width = notification.getWidth();
         height += notification.getHeight();
         SmoothAnimationTimer widthTimer = notification.getWidthTimer();
         SmoothAnimationTimer heightTimer = notification.getHeightTimer();
         float lifeTime = (float)(System.currentTimeMillis() - notification.getCreateTime());
         if (lifeTime > (float)notification.getMaxAge()) {
            widthTimer.target = 0.0F;
            heightTimer.target = 0.0F;
            if (widthTimer.isAnimationDone(true)) {
               this.notifications.remove(notification);
            }
         } else {
            widthTimer.target = width;
            heightTimer.target = height;
         }

         widthTimer.update(true);
         heightTimer.update(true);
         Window window = Minecraft.getInstance().getWindow();
         float x = (float)window.getGuiScaledWidth() - widthTimer.value + 2.0F;
         float y = (float)window.getGuiScaledHeight() - heightTimer.value;
         RenderUtils.drawRoundedRect(
            guiGraphics, x + 2.0F, y + 4.0F, notification.getWidth(), 20.0F, 5.0F, notification.getLevel().getColor()
         );
         renderScaledString(guiGraphics, notification.getMessage(), x + 6.0F, y + 9.0F, 0.35F, Color.WHITE.getRGB());
      }
   }

   private static void renderScaledString(GuiGraphics guiGraphics, String text, float x, float y, float scale, int color) {
      if (guiGraphics == null || text == null || text.isEmpty() || scale <= 0.0F) {
         return;
      }

      Fonts.harmony.render(guiGraphics, text, (double)x, (double)y, new Color(color, true), true, (double)scale);
   }
}
