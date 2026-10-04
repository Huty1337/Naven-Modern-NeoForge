package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntDoubleImmutablePair;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

public class BlurUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static Shader shaderDown;
   private static Shader shaderUp;
   private static Shader shaderApply;
   private static final TimeHelper blurTimer = new TimeHelper();
   private static final Framebuffer[] fbos = new Framebuffer[6];
   private static final MaskTarget maskTarget = new MaskTarget();
   private static final IntDoubleImmutablePair[] strengths = new IntDoubleImmutablePair[]{
      IntDoubleImmutablePair.of(1, 1.25),
      IntDoubleImmutablePair.of(1, 2.25),
      IntDoubleImmutablePair.of(2, 2.0),
      IntDoubleImmutablePair.of(2, 3.0),
      IntDoubleImmutablePair.of(2, 4.25),
      IntDoubleImmutablePair.of(3, 2.5),
      IntDoubleImmutablePair.of(3, 3.25),
      IntDoubleImmutablePair.of(3, 4.25),
      IntDoubleImmutablePair.of(3, 5.5),
      IntDoubleImmutablePair.of(4, 3.25),
      IntDoubleImmutablePair.of(4, 4.0),
      IntDoubleImmutablePair.of(4, 5.0),
      IntDoubleImmutablePair.of(4, 6.0),
      IntDoubleImmutablePair.of(4, 7.25),
      IntDoubleImmutablePair.of(4, 8.25),
      IntDoubleImmutablePair.of(5, 4.5),
      IntDoubleImmutablePair.of(5, 5.25),
      IntDoubleImmutablePair.of(5, 6.25),
      IntDoubleImmutablePair.of(5, 7.25),
      IntDoubleImmutablePair.of(5, 8.5)
   };
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static boolean unavailable = false;

   public static void onRenderAfterWorld(PoseStack stack, float fps, int strengthIndex) {
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
      int mainTexture = GL.getMainTargetColorTexture();
      if (mainFramebuffer <= 0 || mainTexture <= 0 || !ensureResources() || !maskTarget.ensure(width, height)) {
         return;
      }

      IntDoubleImmutablePair config = strengths[Math.max(0, Math.min(strengths.length - 1, strengthIndex))];
      int iterations = config.leftInt();
      double offset = config.rightDouble();

      GL.State previous = GL.captureState();
      boolean began = false;

      try {
         GL.disableScissorTest();
         GL.stencilTest(false);
         GL.colorMask(true, true, true, true);

         if (lastWidth != width || lastHeight != height) {
            for (int i = 0; i < fbos.length; i++) {
               fbos[i].resizeTo(Math.max(1, width >> i), Math.max(1, height >> i));
            }

            lastWidth = width;
            lastHeight = height;
         }

         PostProcessRenderer.beginRender(stack);
         began = true;

         if (blurTimer.delay((double)(1000.0F / Math.max(1.0F, fps)))) {
            renderToFbo(stack, fbos[0], mainTexture, shaderDown, offset);

            for (int i = 0; i < iterations; i++) {
               renderToFbo(stack, fbos[i + 1], fbos[i].texture, shaderDown, offset);
            }

            for (int i = iterations; i >= 1; i--) {
               renderToFbo(stack, fbos[i - 1], fbos[i].texture, shaderUp, offset);
            }

            blurTimer.reset();
         }

         bindMainTarget(mainFramebuffer, width, height);
         GL.disableDepth();
         GL.depthMask(false);
         GL.enableBlend();
         GL.defaultBlendFunc();

         applyMaskedBlur(stack, mainFramebuffer, width, height, EventType.BLUR, fbos[0], 1.0F);

         applyMaskedBlur(stack, mainFramebuffer, width, height, EventType.BLUR_TARGETHUD, fbos[0], 1.0F);
      } catch (Throwable throwable) {
         unavailable = true;
         LOGGER.error("HUD backdrop blur failed and has been disabled for this session", throwable);
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
      if (shaderDown != null) {
         return true;
      }

      try {
         shaderDown = new Shader("blur.vert", "blur_down.frag");
         shaderUp = new Shader("blur.vert", "blur_up.frag");
         shaderApply = new Shader("blur.vert", "blur_apply.frag");

         for (int i = 0; i < fbos.length; i++) {
            fbos[i] = new Framebuffer();
         }

         PostProcessRenderer.init();
         return true;
      } catch (Throwable throwable) {
         shaderDown = null;
         unavailable = true;
         LOGGER.error("Could not initialise the HUD backdrop blur shaders; effect disabled", throwable);
         return false;
      }
   }

   private static void bindMainTarget(int framebuffer, int width, int height) {
      GL.bindFramebuffer(framebuffer);
      GL.viewport(0, 0, width, height);
   }

   private static void applyMaskedBlur(
      PoseStack stack, int mainFramebuffer, int width, int height, EventType maskType, Framebuffer blurTexture, float alpha
   ) {
      maskTarget.paintMask(stack, maskType);
      diagMask(maskType);
      int maskTexture = maskTarget.glTextureId();
      if (maskTexture <= 0) {
         return;
      }

      bindMainTarget(mainFramebuffer, width, height);
      GL.enableBlend();
      GL.defaultBlendFunc();
      GL.disableDepth();
      GL.depthMask(false);

      shaderApply.bind();
      shaderApply.set("uTexture", 0);
      shaderApply.set("uMaskTexture", 1);
      shaderApply.set("uAlpha", (double)alpha);
      GL.bindTexture(blurTexture.texture, 0);
      GL.bindTexture(maskTexture, 1);
      PostProcessRenderer.render(stack);
      GL.resetTextureSlot();
   }

   private static void renderToFbo(PoseStack stack, Framebuffer targetFbo, int sourceTexture, Shader shader, double offset) {
      targetFbo.bind();
      targetFbo.setViewport();
      shader.bind();
      GL.bindTexture(sourceTexture);
      shader.set("uTexture", 0);
      shader.set("uHalfTexelSize", 0.5 / (double)targetFbo.width, 0.5 / (double)targetFbo.height);
      shader.set("uOffset", offset);
      if (shader == shaderUp) {
         shader.set("uAlpha", 1.0);
      }

      PostProcessRenderer.render(stack);
   }


   static final boolean DIAG = true;
   private static long diagLastLog;
   private static int diagLastDraws;
   private static boolean diagMaskDumped;
   private static boolean diagEnvLogged;

   private static void diagMask(EventType type) {
      if (!DIAG) {
         return;
      }

      int width = maskTarget.width();
      int height = maskTarget.height();
      if (!diagEnvLogged) {
         diagEnvLogged = true;
         LOGGER.info(
            "[ppdiag] device={} realDevice={} mainFbo={} mainTex={} mask={}x{} draws={} last={}",
            new Object[]{
               RenderSystem.getDevice().getClass().getSimpleName(),
               GL.realDevice(RenderSystem.getDevice()).getClass().getSimpleName(),
               GL.getMainTargetFramebuffer(),
               GL.getMainTargetColorTexture(),
               width,
               height,
               com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.diagDraws,
               com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.diagLast
            }
         );
      }

      if (!diagMaskDumped && type == EventType.BLUR) {
         diagMaskDumped = true;
         int maskTexture = maskTarget.glTextureId();
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
            "[ppdiag] mask({}) {}x{} nonZeroAlphaPixels={} bbox={},{}..{},{} draws={} last={}",
            new Object[]{
               type,
               width,
               height,
               count,
               minX,
               minY,
               maxX,
               maxY,
               com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.diagDraws,
               com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.diagLast
            }
         );
      }

      long now = System.currentTimeMillis();
      if (now - diagLastLog > 2000L) {
         diagLastLog = now;
         LOGGER.info(
            "[ppdiag] chain type={} drawsDelta={} drawsTotal={} last={} mask={}x{} glError={}",
            new Object[]{
               type,
               com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.diagDraws - diagLastDraws,
               com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.diagDraws,
               com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.diagLast,
               width,
               height,
               GL11.glGetError()
            }
         );
         diagLastDraws = com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils.diagDraws;
      }
   }
}
