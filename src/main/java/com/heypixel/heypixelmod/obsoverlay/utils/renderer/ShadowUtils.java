package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public class ShadowUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final TimeHelper shadowTimer = new TimeHelper();
   private static Framebuffer fbo1;
   private static Framebuffer fbo2;
   private static final MaskTarget maskTarget = new MaskTarget();
   private static Shader shader;
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static boolean unavailable = false;

   public static void onRenderAfterWorld(PoseStack stack, float fps) {
      if (unavailable || stack == null) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return;
      }

      RenderTarget main = mc.getMainRenderTarget();
      int width = Math.max(1, main.width);
      int height = Math.max(1, main.height);
      int mainFramebuffer = GL.getMainTargetFramebuffer();
      if (mainFramebuffer <= 0 || !ensureResources() || !maskTarget.ensure(width, height)) {
         return;
      }

      GL.State previous = GL.captureState();
      boolean began = false;

      try {
         GL.disableScissorTest();
         GL.stencilTest(false);
         GL.colorMask(true, true, true, true);

         if (lastWidth != width || lastHeight != height) {
            fbo1.resizeTo(width, height);
            fbo2.resizeTo(width, height);
            lastWidth = width;
            lastHeight = height;
         }

         boolean shouldRefreshMask = false;
         if (shadowTimer.delay((double)(1000.0F / Math.max(1.0F, fps)))) {
            shouldRefreshMask = true;
            shadowTimer.reset();
         }

         GL.enableBlend();
         shader.bind();
         shader.set("u_Size", (double)width, (double)height);
         PostProcessRenderer.beginRender(stack);
         began = true;

         if (shouldRefreshMask) {
            maskTarget.paintMask(stack, EventType.SHADOW);
            int maskTexture = maskTarget.glTextureId();
            if (maskTexture <= 0) {
               maskTexture = 0;
            }

            fbo1.bind();
            fbo1.setViewport();
            GL.bindTexture(maskTexture);
            shader.set("u_Direction", 1.0, 0.0);
            PostProcessRenderer.render(stack);

            fbo2.bind();
            fbo2.setViewport();
            GL.bindTexture(fbo1.texture);
            shader.set("u_Direction", 0.0, 1.0);
            PostProcessRenderer.render(stack);

            fbo1.bind();
            fbo1.setViewport();
            GL.bindTexture(fbo2.texture);
            shader.set("u_Direction", 1.0, 0.0);
            PostProcessRenderer.render(stack);
         }

         GL.bindFramebuffer(mainFramebuffer);
         GL.viewport(0, 0, width, height);
         GL.disableDepth();
         GL.depthMask(false);
         shader.bind();
         shader.set("u_Size", (double)width, (double)height);
         GL.bindTexture(fbo1.texture);
         shader.set("u_Direction", 0.0, 1.0);
         PostProcessRenderer.render(stack);
      } catch (Throwable throwable) {
         unavailable = true;
         LOGGER.error("HUD shadow/glow pass failed and has been disabled for this session", throwable);
      } finally {
         try {
            GL.disableBlend();
            if (began) {
               PostProcessRenderer.endRender();
            }
         } catch (Throwable ignored) {
         }

         try {
            GL.restoreState(previous);
            GL.resetTextureSlot();
         } catch (Throwable ignored) {
         }
      }
   }

   private static boolean ensureResources() {
      if (shader != null) {
         return true;
      }

      try {
         shader = new Shader("shadow.vert", "shadow.frag");
         fbo1 = new Framebuffer();
         fbo2 = new Framebuffer();
         PostProcessRenderer.init();
         return true;
      } catch (Throwable throwable) {
         shader = null;
         unavailable = true;
         LOGGER.error("Could not initialise the shadow shaders; effect disabled", throwable);
         return false;
      }
   }
}
