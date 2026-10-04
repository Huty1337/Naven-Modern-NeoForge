package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class Framebuffer {
   private int id;
   public int texture;
   public double sizeMulti = 1.0;
   public int width;
   public int height;
   private boolean fixedSize;
   private int fixedWidth;
   private int fixedHeight;

   public Framebuffer(double sizeMulti) {
      this.sizeMulti = sizeMulti;
      this.init();
   }

   public Framebuffer() {
      this(1.0);
   }

   public Framebuffer(int width, int height) {
      this.fixedSize = true;
      this.fixedWidth = width;
      this.fixedHeight = height;
      this.init();
   }

   private void init() {
      GL.State previous = GL.captureState();

      try {
         this.id = GL.genFramebuffer();
         int baseWidth = Minecraft.getInstance().getWindow().getWidth();
         int baseHeight = Minecraft.getInstance().getWindow().getHeight();
         if (previous.viewport[2] > 1 && previous.viewport[3] > 1) {
            baseWidth = previous.viewport[2];
            baseHeight = previous.viewport[3];
         }

         if (this.fixedSize) {
            this.width = Math.max(1, this.fixedWidth);
            this.height = Math.max(1, this.fixedHeight);
         } else {
            this.width = Math.max(1, (int)((double)baseWidth * this.sizeMulti));
            this.height = Math.max(1, (int)((double)baseHeight * this.sizeMulti));
         }

         GL.bindFramebuffer(this.id);
         this.texture = GL.genTexture();
         GL.bindTexture(this.texture);
         GL.defaultPixelStore();
         GL.textureParam(3553, 10242, 33071);
         GL.textureParam(3553, 10243, 33071);
         GL.textureParam(3553, 10241, 9729);
         GL.textureParam(3553, 10240, 9729);
         GL.textureImage2D(3553, 0, 32856, this.width, this.height, 0, 6408, 5121, null);
         GL.framebufferTexture2D(36160, 36064, 3553, this.texture, 0);
         if (GL30.glCheckFramebufferStatus(36160) != 36053) {
            throw new IllegalStateException("Framebuffer " + this.id + " is not complete");
         }
      } finally {
         GL.restoreState(previous);
      }
   }

   public int getId() {
      return this.id;
   }

   public void bind() {
      GL.bindFramebuffer(this.id);
   }

   public void setViewport() {
      GL.viewport(0, 0, this.width, this.height);
   }

   public void unbind() {
      GL.bindFramebuffer(GL.getMainTargetFramebuffer());
      RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
      GL.viewport(0, 0, target.width, target.height);
   }

   public void resize() {
      GL.deleteFramebuffer(this.id);
      GL.deleteTexture(this.texture);
      this.init();
   }

   public void resizeTo(int width, int height) {
      this.fixedSize = true;
      if (this.fixedWidth == width && this.fixedHeight == height) {
         return;
      }

      this.fixedWidth = width;
      this.fixedHeight = height;
      this.resize();
   }

   public void clear() {
      this.bind();
      this.setViewport();
      GL.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL.clearBuffer(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
   }
}
