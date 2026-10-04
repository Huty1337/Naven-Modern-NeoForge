package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import org.joml.Matrix4fStack;

public final class MaskTarget {
   private static final CachedOrthoProjectionMatrixBuffer GUI_PROJECTION = new CachedOrthoProjectionMatrixBuffer(
      "naven post-process mask", 1000.0F, 11000.0F, true
   );
   private static final float GUI_Z_TRANSLATION = -11000.0F;

   private GpuTexture color;
   private GpuTextureView colorView;
   private int width = -1;
   private int height = -1;

   public boolean ensure(int requestedWidth, int requestedHeight) {
      int requestedW = Math.max(1, requestedWidth);
      int requestedH = Math.max(1, requestedHeight);
      if (this.color != null && this.width == requestedW && this.height == requestedH) {
         return true;
      }

      this.close();
      int usage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST;
      this.color = RenderSystem.getDevice()
         .createTexture("Naven post-process mask", usage, TextureFormat.RGBA8, requestedW, requestedH, 1, 1);
      this.colorView = RenderSystem.getDevice().createTextureView(this.color);
      this.width = requestedW;
      this.height = requestedH;
      return true;
   }

   public void paintMask(PoseStack stack, EventType type) {
      RenderSystem.assertOnRenderThread();
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.clearColorTexture(this.color, 0);

      GpuTextureView previousColor = RenderSystem.outputColorTextureOverride;
      ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
      boolean scissorWasEnabled = scissor.enabled();
      int scissorX = scissor.x();
      int scissorY = scissor.y();
      int scissorWidth = scissor.width();
      int scissorHeight = scissor.height();
      RenderSystem.backupProjectionMatrix();
      Matrix4fStack modelView = RenderSystem.getModelViewStack();
      modelView.pushMatrix();

      try {
         RenderSystem.outputColorTextureOverride = this.colorView;
         Minecraft mc = Minecraft.getInstance();
         RenderSystem.setProjectionMatrix(
            GUI_PROJECTION.getBuffer(
               (float)mc.getWindow().getWidth() / (float)mc.getWindow().getGuiScale(),
               (float)mc.getWindow().getHeight() / (float)mc.getWindow().getGuiScale()
            ),
            ProjectionType.ORTHOGRAPHIC
         );
         modelView.identity();
         modelView.translate(0.0F, 0.0F, GUI_Z_TRANSLATION);
         if (scissorWasEnabled) {
            RenderSystem.disableScissorForRenderTypeDraws();
         }

         Naven.getInstance().getEventManager().call(new EventShader(stack, type));
      } finally {
         if (scissorWasEnabled) {
            RenderSystem.enableScissorForRenderTypeDraws(scissorX, scissorY, scissorWidth, scissorHeight);
         }

         modelView.popMatrix();
         RenderSystem.outputColorTextureOverride = previousColor;
         RenderSystem.restoreProjectionMatrix();
      }
   }

   public int glTextureId() {
      GlTexture glTexture = GL.asGlTexture(this.color);
      return glTexture != null ? glTexture.glId() : -1;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public void close() {
      if (this.colorView != null) {
         this.colorView.close();
         this.colorView = null;
      }

      if (this.color != null) {
         this.color.close();
         this.color = null;
      }

      this.width = -1;
      this.height = -1;
   }
}
