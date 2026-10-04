package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

public class GlowUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final EnumMap<EventType, GlowCache> caches = new EnumMap<>(EventType.class);
   private static Shader blurShader;
   private static Shader applyShader;
   private static boolean unavailable = false;

   private static final class GlowCache {
      private final TimeHelper timer = new TimeHelper();
      private final Framebuffer fbo1 = new Framebuffer();
      private final Framebuffer fbo2 = new Framebuffer();
      private final MaskTarget mask = new MaskTarget();
      private int width;
      private int height;

      private boolean ensureSize(int requestedWidth, int requestedHeight) {
         int w = Math.max(1, requestedWidth);
         int h = Math.max(1, requestedHeight);
         if (this.width == w && this.height == h) {
            return false;
         }

         this.width = w;
         this.height = h;
         this.mask.ensure(w, h);
         this.fbo1.resizeTo(w, h);
         this.fbo2.resizeTo(w, h);
         return true;
      }
   }

   public static void onRenderAfterWorld(PoseStack stack, float fps, float width, float intensity, float depth, EventType shaderType) {
      if (unavailable || stack == null) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return;
      }

      RenderTarget main = mc.getMainRenderTarget();
      int mainFramebuffer = GL.getMainTargetFramebuffer();
      if (mainFramebuffer <= 0 || !ensureResources()) {
         return;
      }

      int viewportWidth = Math.max(1, main.width);
      int viewportHeight = Math.max(1, main.height);

      GL.State previous = GL.captureState();
      boolean began = false;

      try {
         GL.disableScissorTest();
         GL.stencilTest(false);
         GL.colorMask(true, true, true, true);

         GlowCache cache = caches.computeIfAbsent(shaderType, type -> new GlowCache());
         double scale = computeScale(width);
         boolean resized = cache.ensureSize(
            (int)Math.round((double)viewportWidth * scale), (int)Math.round((double)viewportHeight * scale)
         );

         boolean shouldRefreshMask = resized;
         if (!shouldRefreshMask && cache.timer.delay((double)(1000.0F / Math.max(1.0F, fps)))) {
            shouldRefreshMask = true;
         }

         if (shouldRefreshMask) {
            cache.timer.reset();
         }

         PostProcessRenderer.beginRender(stack);
         began = true;

         if (shouldRefreshMask) {
            cache.mask.paintMask(stack, shaderType);
            int maskTexture = cache.mask.glTextureId();
            if (maskTexture <= 0) {
               return;
            }

            diagMask(shaderType, cache, maskTexture);

            GL.enableBlend();
            blurShader.bind();
            blurShader.set("uTexture", 0);
            blurShader.set("uDirection", 1.0, 0.0);
            blurShader.set("uRadius", Math.max(1.0, (double)width * scale));
            blurShader.set("uSigma", 0.0);

            cache.fbo1.bind();
            cache.fbo1.setViewport();
            GL.bindTexture(maskTexture);
            PostProcessRenderer.render(stack);

            blurShader.set("uDirection", 0.0, 1.0);
            cache.fbo2.bind();
            cache.fbo2.setViewport();
            GL.bindTexture(cache.fbo1.texture);
            PostProcessRenderer.render(stack);
         }

         GL.bindFramebuffer(mainFramebuffer);
         GL.viewport(0, 0, viewportWidth, viewportHeight);
         GL.disableDepth();
         GL.depthMask(false);
         GL.enableBlend();
         GlStateManager._blendFuncSeparate(770, 1, 770, 1);

         float hueOffset = (float)((double)(System.currentTimeMillis() % 6000L) / 6000.0);
         applyShader.bind();
         applyShader.set("uTexture", 0);
         applyShader.set("uMaskTexture", 1);
         applyShader.set("uColor", 1.0F, 1.0F, 1.0F);
         applyShader.set("uRainbow", 1);
         applyShader.set("uHueOffset", (double)hueOffset);
         applyShader.set("uRainbowSaturation", 1.0);
         applyShader.set("uRainbowBands", 12.0);
         applyShader.set("uRainbowSteps", 4.0);
         applyShader.set("uIntensity", (double)intensity);
         applyShader.set("uDepth", (double)depth);
         GL.bindTexture(cache.fbo2.texture, 0);
         GL.bindTexture(cache.mask.glTextureId(), 1);
         PostProcessRenderer.render(stack);
         GL.resetTextureSlot();
      } catch (Throwable throwable) {
         unavailable = true;
         LOGGER.error("HUD glow/bloom pass failed and has been disabled for this session", throwable);
      } finally {
         try {
            GL.disableBlend();
            GL.defaultBlendFunc();
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
      if (blurShader != null) {
         return true;
      }

      try {
         blurShader = new Shader("blur.vert", "glow_blur.frag");
         applyShader = new Shader("blur.vert", "glow_apply.frag");
         PostProcessRenderer.init();
         return true;
      } catch (Throwable throwable) {
         blurShader = null;
         unavailable = true;
         LOGGER.error("Could not initialise the glow shaders; effect disabled", throwable);
         return false;
      }
   }

   private static double computeScale(float width) {
      float clampedWidth = Math.max(1.0F, width);
      double targetRadius = 24.0;
      double scale = targetRadius / (double)clampedWidth;
      if (scale > 1.0) {
         return 1.0;
      }

      return Math.max(scale, 0.25);
   }


   static final boolean DIAG = true;
   private static boolean diagDumped;
   private static long diagLast;

   private static void diagMask(EventType type, GlowCache cache, int maskTexture) {
      if (!DIAG) {
         return;
      }

      int width = cache.mask.width();
      int height = cache.mask.height();
      if (!diagDumped) {
         diagDumped = true;
         ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
         GL.bindTexture(maskTexture, 0);
         GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
         int count = 0;
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int maxX = -1;
         int maxY = -1;

         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               int alpha = buffer.get((y * width + x) * 4 + 3) & 0xFF;
               if (alpha > 0) {
                  count++;
                  minX = Math.min(minX, x);
                  minY = Math.min(minY, y);
                  maxX = Math.max(maxX, x);
                  maxY = Math.max(maxY, y);
               }
            }
         }

         LOGGER.info(
            "[ppdiag] glowMask({}) {}x{} nonZeroAlphaPixels={} bbox={},{}..{},{} draws={}",
            new Object[]{type, width, height, count, minX, minY, maxX, maxY, com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.diagDraws}
         );
      }

      long now = System.currentTimeMillis();
      if (now - diagLast > 2000L) {
         diagLast = now;
         LOGGER.info("[ppdiag] glowPass type={} mask={}x{} glError={}", new Object[]{type, width, height, GL11.glGetError()});
      }
   }
}
