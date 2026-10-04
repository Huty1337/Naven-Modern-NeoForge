package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class EspRenderTypes {
   public static final RenderPipeline FILLED_BOX_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
      .withLocation(ResourceLocation.fromNamespaceAndPath("naven", "pipeline/esp_filled_box"))
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   public static final RenderPipeline OUTLINED_BOX_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
      .withLocation(ResourceLocation.fromNamespaceAndPath("naven", "pipeline/esp_outlined_box"))
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .build();
   public static final RenderPipeline LINE_STRIP_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
      .withLocation(ResourceLocation.fromNamespaceAndPath("naven", "pipeline/esp_line_strip"))
      .withVertexShader("core/position_color")
      .withFragmentShader("core/position_color")
      .withBlend(BlendFunction.TRANSLUCENT)
      .withCull(false)
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINE_STRIP)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .build();
   private static final Map<Double, RenderType> LINE_STRIP_TYPES = new HashMap<>();
   private static RenderType filledBox;
   private static RenderType outlinedBox;

   private EspRenderTypes() {
   }

   public static RenderType filledBox() {
      RenderType type = filledBox;
      if (type == null) {
         filledBox = type = RenderType.create(
            "naven_esp_filled_box",
            1536,
            false,
            true,
            FILLED_BOX_PIPELINE,
            RenderType.CompositeState.builder().createCompositeState(false)
         );
      }

      return type;
   }

   public static RenderType outlinedBox() {
      RenderType type = outlinedBox;
      if (type == null) {
         outlinedBox = type = RenderType.create(
            "naven_esp_outlined_box",
            1536,
            OUTLINED_BOX_PIPELINE,
            RenderType.CompositeState.builder()
               .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0)))
               .createCompositeState(false)
         );
      }

      return type;
   }

   public static RenderType lineStrip(double width) {
      return LINE_STRIP_TYPES.computeIfAbsent(width, w -> RenderType.create(
         "naven_esp_line_strip", 1536, LINE_STRIP_PIPELINE, RenderType.CompositeState.builder().setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(w))).createCompositeState(false)
      ));
   }
}
