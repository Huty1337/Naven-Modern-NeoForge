package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;

@ModuleInfo(
   name = "MotionBlur",
   description = "Make your game smoother.",
   category = Category.RENDER
)
public class MotionBlur extends Module {
   private static final Logger LOGGER = LogUtils.getLogger();

   private static final ResourceLocation POST_EFFECT_ID = ResourceLocation.withDefaultNamespace("motion_blur");
   private static final ResourceLocation CONFIG_FILE = ResourceLocation.withDefaultNamespace("post_effect/motion_blur.json");
   private static final ResourceLocation VERTEX_SHADER_ID = ResourceLocation.withDefaultNamespace("core/screenquad");
   private static final ResourceLocation FRAGMENT_SHADER_ID = ResourceLocation.withDefaultNamespace("post/motion_blur");
   private static final ResourceLocation PROBE_PIPELINE_ID = ResourceLocation.withDefaultNamespace("post/motion_blur_probe");
   private static final ResourceLocation HISTORY_TARGET = ResourceLocation.withDefaultNamespace("history");
   private static final String SAMPLER_INFO_BLOCK = "SamplerInfo";
   private static final String CONFIG_BLOCK = "MotionBlurConfig";
   private static final String CURR_SAMPLER = "CurrSampler";
   private static final String PREV_SAMPLER = "PrevSampler";
   private static final int BLEND_UNIFORM_SIZE = 4;
   private static final float MAX_STRENGTH = 10.0F;

   private static final Field CHAIN_PASSES_FIELD = findField(PostChain.class, "passes");
   private static final Field CHAIN_PERSISTENT_TARGETS_FIELD = findField(PostChain.class, "persistentTargets");
   private static final Field PASS_UNIFORMS_FIELD = findField(PostPass.class, "customUniforms");
   private static final Field SHADER_CACHE_FIELD = findField(ShaderManager.class, "compilationCache");

   public static MotionBlur instance;

   private final FloatValue strength = ValueBuilder.create(this, "Strength")
      .setFloatStep(0.1F)
      .setDefaultFloatValue(7.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();

   @Deprecated
   public PostChain shader;

   private PostChain lastChain;
   private PostChain strengthChain;
   private GpuBuffer strengthBuffer;
   private float uploadedBlend = Float.NaN;
   private final ByteBuffer blendScratch = ByteBuffer.allocateDirect(BLEND_UNIFORM_SIZE).order(ByteOrder.nativeOrder());

   private boolean validated;
   private boolean failed;
   private boolean strengthWritable = true;
   private boolean warned;
   private Object validatedGeneration;

   public MotionBlur() {
      instance = this;
   }

   @Override
   public void onEnable() {
      this.resetHistory();
      this.validated = false;
      this.failed = false;
   }

   @EventTarget
   public void onTick(EventRunTicks event) {
   }

   public void applyMotionBlur(boolean renderLevel) {
      Minecraft mc = Minecraft.getInstance();
      if (!this.isEnabled() || !renderLevel || mc.level == null || mc.noRender) {
         return;
      }

      Object generation = this.shaderGeneration(mc);
      if (generation != this.validatedGeneration) {
         this.validatedGeneration = generation;
         this.validated = false;
         this.failed = false;
      }

      if (this.failed) {
         return;
      }

      if (!this.validated) {
         this.validated = true;
         if (!this.validate(mc)) {
            this.failed = true;
            this.warnOnce("the motion blur post effect is unavailable, see the errors above; toggle the module to retry");
            return;
         }
      }

      PostChain chain = mc.getShaderManager().getPostChain(POST_EFFECT_ID, LevelTargetBundle.MAIN_TARGETS);
      if (chain == null) {
         this.failed = true;
         this.warnOnce("the motion blur post effect could not be loaded; toggle the module to retry");
         return;
      }

      try {
         this.uploadStrength(chain);
         chain.process(mc.getMainRenderTarget(), GraphicsResourceAllocator.UNPOOLED);
         this.lastChain = chain;
      } catch (Exception exception) {
         this.failed = true;
         LOGGER.error("[MotionBlur] failed to apply the post effect", exception);
      }
   }

   private boolean validate(Minecraft mc) {
      return this.validateConfig(mc) && this.validatePipeline(mc);
   }

   private boolean validateConfig(Minecraft mc) {
      Optional<Resource> resource = mc.getResourceManager().getResource(CONFIG_FILE);
      if (resource.isEmpty()) {
         LOGGER.error("[MotionBlur] post effect {} is missing from the mod resources", CONFIG_FILE);
         return false;
      }

      try (Reader reader = resource.get().openAsReader()) {
         JsonElement json = StrictJsonParser.parse(reader);
         PostChainConfig.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(JsonSyntaxException::new);
         return true;
      } catch (Exception exception) {
         LOGGER.error("[MotionBlur] post effect {} is invalid", CONFIG_FILE, exception);
         return false;
      }
   }

   private boolean validatePipeline(Minecraft mc) {
      try {
         RenderPipeline probe = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(PROBE_PIPELINE_ID)
            .withVertexShader(VERTEX_SHADER_ID)
            .withFragmentShader(FRAGMENT_SHADER_ID)
            .withSampler(CURR_SAMPLER)
            .withSampler(PREV_SAMPLER)
            .withUniform(SAMPLER_INFO_BLOCK, UniformType.UNIFORM_BUFFER)
            .withUniform(CONFIG_BLOCK, UniformType.UNIFORM_BUFFER)
            .build();
         boolean valid = RenderSystem.getDevice().precompilePipeline(probe, mc.getShaderManager()::getShader).isValid();
         if (!valid) {
            LOGGER.error("[MotionBlur] the motion blur shader program failed to compile");
         }

         return valid;
      } catch (Exception exception) {
         LOGGER.error("[MotionBlur] could not validate the motion blur shader program", exception);
         return false;
      }
   }

   private float blendFactor() {
      return Mth.clamp(1.0F - this.strength.getCurrentValue() / MAX_STRENGTH, 0.0F, 1.0F);
   }

   private void uploadStrength(PostChain chain) {
      GpuBuffer buffer = this.resolveStrengthBuffer(chain);
      if (buffer == null) {
         return;
      }

      float blend = this.blendFactor();
      if (Float.compare(blend, this.uploadedBlend) == 0) {
         return;
      }

      try {
         this.blendScratch.clear();
         this.blendScratch.putFloat(blend);
         this.blendScratch.flip();
         RenderSystem.getDevice()
            .createCommandEncoder()
            .writeToBuffer(buffer.slice(0, BLEND_UNIFORM_SIZE), this.blendScratch);
         this.uploadedBlend = blend;
      } catch (Exception exception) {
         this.strengthWritable = false;
         this.strengthBuffer = null;
         this.strengthChain = null;
         LOGGER.warn("[MotionBlur] the Strength slider cannot be driven at runtime; using the fixed value from the post effect json", exception);
      }
   }

   @SuppressWarnings("unchecked")
   private GpuBuffer resolveStrengthBuffer(PostChain chain) {
      if (!this.strengthWritable) {
         return null;
      }

      if (this.strengthChain == chain && this.strengthBuffer != null && !this.strengthBuffer.isClosed()) {
         return this.strengthBuffer;
      }

      if (CHAIN_PASSES_FIELD == null || PASS_UNIFORMS_FIELD == null) {
         this.strengthWritable = false;
         LOGGER.warn("[MotionBlur] PostPass internals are not reachable; the Strength slider is fixed to the post effect json value");
         return null;
      }

      try {
         List<PostPass> passes = (List<PostPass>)CHAIN_PASSES_FIELD.get(chain);
         if (passes == null) {
            this.strengthWritable = false;
            return null;
         }

         Map<String, GpuBuffer> uniforms = null;
         for (PostPass pass : passes) {
            Map<String, GpuBuffer> candidate = (Map<String, GpuBuffer>)PASS_UNIFORMS_FIELD.get(pass);
            if (candidate != null && candidate.containsKey(CONFIG_BLOCK)) {
               uniforms = candidate;
               break;
            }
         }

         if (uniforms == null) {
            this.strengthWritable = false;
            LOGGER.warn(
               "[MotionBlur] no post pass declares the {} uniform block; the Strength slider is fixed to the post effect json value", CONFIG_BLOCK
            );
            return null;
         }

         this.blendScratch.clear();
         this.blendScratch.putFloat(this.blendFactor());
         this.blendScratch.flip();
         GpuBuffer writable = RenderSystem.getDevice()
            .createBuffer(() -> "Naven MotionBlur strength", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, this.blendScratch);
         GpuBuffer replaced = uniforms.put(CONFIG_BLOCK, writable);
         if (replaced != null && replaced != writable) {
            replaced.close();
         }

         this.strengthChain = chain;
         this.strengthBuffer = writable;
         this.uploadedBlend = this.blendFactor();
         return writable;
      } catch (Exception exception) {
         this.strengthWritable = false;
         LOGGER.warn("[MotionBlur] could not install the writable strength uniform buffer; using the fixed value from the post effect json", exception);
         return null;
      }
   }

   @SuppressWarnings("unchecked")
   private void resetHistory() {
      PostChain chain = this.lastChain;
      this.lastChain = null;
      if (chain == null || CHAIN_PERSISTENT_TARGETS_FIELD == null) {
         return;
      }

      try {
         Map<ResourceLocation, RenderTarget> targets = (Map<ResourceLocation, RenderTarget>)CHAIN_PERSISTENT_TARGETS_FIELD.get(chain);
         RenderTarget history = targets == null ? null : targets.get(HISTORY_TARGET);
         GpuTexture color = history == null ? null : history.getColorTexture();
         if (color != null && !color.isClosed()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(color, 0);
         }
      } catch (Exception exception) {
         LOGGER.warn("[MotionBlur] could not reset the motion blur history target", exception);
      }
   }

   private void warnOnce(String message) {
      if (!this.warned) {
         this.warned = true;
         LOGGER.warn("[MotionBlur] {}", message);
      }
   }

   private static Field findField(Class<?> owner, String name) {
      try {
         Field field = owner.getDeclaredField(name);
         field.setAccessible(true);
         return field;
      } catch (Exception exception) {
         return null;
      }
   }

   private Object shaderGeneration(Minecraft mc) {
      if (SHADER_CACHE_FIELD == null) {
         return null;
      }

      try {
         return SHADER_CACHE_FIELD.get(mc.getShaderManager());
      } catch (Exception exception) {
         return null;
      }
   }
}
