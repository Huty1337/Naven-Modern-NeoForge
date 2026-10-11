package tech.naven.utils.renderer;

import tech.naven.events.api.types.EventType;
import tech.naven.utils.TimeHelper;
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
}
