package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class Render3DUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   private static GpuBufferSlice levelProjection;
   private static ProjectionType levelProjectionType = ProjectionType.PERSPECTIVE;

   private Render3DUtils() {
   }

   public static MultiBufferSource.BufferSource bufferSource() {
      return mc.renderBuffers().bufferSource();
   }

   public static void filledBox(PoseStack stack, AABB box, float red, float green, float blue, float alpha) {
      MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
      VertexConsumer consumer = source.getBuffer(EspRenderTypes.filledBox());
      withLevelProjection(() -> {
         ShapeRenderer.addChainedFilledBoxVertices(
            stack, consumer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha
         );
         source.endLastBatch();
      });
   }

   public static void outlinedBox(PoseStack stack, AABB box, float red, float green, float blue, float alpha) {
      MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
      VertexConsumer consumer = source.getBuffer(EspRenderTypes.outlinedBox());
      withLevelProjection(() -> {
         ShapeRenderer.renderLineBox(stack.last(), consumer, box, red, green, blue, alpha);
         source.endLastBatch();
      });
   }

   public static void lineStrip(PoseStack stack, List<Vec3> points, Vec3 camera, float red, float green, float blue, float alpha, double width) {
      if (points.size() < 2) {
         return;
      }

      MultiBufferSource.BufferSource source = mc.renderBuffers().bufferSource();
      VertexConsumer consumer = source.getBuffer(EspRenderTypes.lineStrip(width));
      Matrix4f matrix = stack.last().pose();
      withLevelProjection(() -> {
         for (Vec3 point : points) {
            consumer.addVertex(matrix, (float)(point.x - camera.x), (float)(point.y - camera.y), (float)(point.z - camera.z))
               .setColor(red, green, blue, alpha);
         }

         source.endLastBatch();
      });
   }

   private static void withLevelProjection(Runnable draw) {
      GpuBufferSlice saved = RenderSystem.getProjectionMatrixBuffer();
      ProjectionType savedType = RenderSystem.getProjectionType();
      GpuBufferSlice level = levelProjection;
      boolean override = level != null && level != saved;

      if (override) {
         RenderSystem.setProjectionMatrix(level, levelProjectionType);
      }

      try {
         draw.run();
      } finally {
         if (override) {
            RenderSystem.setProjectionMatrix(saved, savedType);
         }
      }
   }

   private static void captureLevelProjection() {
      GpuBufferSlice slice = RenderSystem.getProjectionMatrixBuffer();
      if (slice != null) {
         levelProjection = slice;
         levelProjectionType = RenderSystem.getProjectionType();
      }
   }

   public static final class LevelProjectionListener {
      @SubscribeEvent
      public void onLevelStage(RenderLevelStageEvent.AfterLevel event) {
         captureLevelProjection();
      }
   }
}
