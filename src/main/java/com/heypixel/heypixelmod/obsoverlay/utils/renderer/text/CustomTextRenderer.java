package com.heypixel.heypixelmod.obsoverlay.utils.renderer.text;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.lwjgl.BufferUtils;

public class CustomTextRenderer {
   private final Font font;
   private float alpha = 1.0F;

   public CustomTextRenderer(String name, int size, int from, int to, int textureSize) {
      InputStream in = this.getClass().getResourceAsStream("/assets/heypixel/VcX6svVqmeT8/fonts/" + name + ".ttf");
      if (in == null) {
         throw new RuntimeException("Font not found: " + name);
      }

      byte[] bytes;
      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         byte[] buffer = new byte[1024];
         int len;
         while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
         }
         bytes = out.toByteArray();
      } catch (IOException e) {
         throw new RuntimeException("Failed to read font: " + name, e);
      }

      ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes);
      ((java.nio.Buffer)buffer).flip();
      this.font = new Font(buffer, size, from, to, textureSize, name);
   }

   public void setAlpha(float alpha) {
      this.alpha = alpha;
   }

   public float getWidth(String text, double scale) {
      return (float)this.getWidth(text, false, scale);
   }

   public double getWidth(String text, boolean shadow, double scale) {
      if (text == null || text.isEmpty()) {
         return 0.0;
      }

      return (this.font.getWidth(text) + (double)(shadow ? 0.5F : 0.0F)) * scale;
   }

   public double getHeight(boolean shadow, double scale) {
      return (this.font.getHeight() + (double)(shadow ? 0.5F : 0.0F)) * scale;
   }

   public double render(GuiGraphics guiGraphics, String text, double x, double y, Color color, boolean shadow, double scale) {
      double width = this.getWidth(text, shadow, scale);
      ResourceLocation atlas = this.font.getTextureLocation();
      if (guiGraphics == null || text == null || text.isEmpty() || atlas == null || scale == 0.0) {
         return width;
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft == null || minecraft.getTextureManager() == null) {
         return width;
      }

      AbstractTexture texture = minecraft.getTextureManager().getTexture(atlas);
      TextQuadRenderState state = TextQuadRenderState.build(
         this.font, texture.getTextureView(), guiGraphics, text, x, y, color, shadow, scale, this.alpha
      );
      if (state == null) {
         return width;
      }

      guiGraphics.submitGuiElementRenderState(state);
      return x + this.font.getWidth(text) * scale;
   }
}
