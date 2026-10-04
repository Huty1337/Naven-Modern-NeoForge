package com.heypixel.heypixelmod.obsoverlay.utils.renderer.text;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.Color;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

public final class TextQuadRenderState implements GuiElementRenderState {
   public static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);

   private static final int VERTEX_STRIDE = 4;

   private final RenderPipeline pipeline;
   private final TextureSetup textureSetup;
   private final Matrix3x2f pose;
   @Nullable
   private final ScreenRectangle scissorArea;
   @Nullable
   private final ScreenRectangle bounds;
   private final float[] vertices;
   private final int[] colors;
   private final int vertexCount;

   private TextQuadRenderState(
      RenderPipeline pipeline,
      TextureSetup textureSetup,
      Matrix3x2f pose,
      @Nullable ScreenRectangle scissorArea,
      float[] vertices,
      int[] colors,
      int vertexCount
   ) {
      this.pipeline = pipeline;
      this.textureSetup = textureSetup;
      this.pose = pose;
      this.scissorArea = scissorArea;
      this.vertices = vertices;
      this.colors = colors;
      this.vertexCount = vertexCount;
      this.bounds = computeBounds(vertices, vertexCount, pose, scissorArea);
   }

   @Nullable
   public static TextQuadRenderState build(
      Font font,
      GpuTextureView texture,
      GuiGraphics guiGraphics,
      String text,
      double x,
      double y,
      Color color,
      boolean shadow,
      double scale,
      float alpha
   ) {
      Builder builder = new Builder();
      if (shadow) {
         font.emit(builder, text, x + 0.5, y + 0.5, SHADOW_COLOR, scale, true, alpha);
      }

      font.emit(builder, text, x, y, color, scale, false, alpha);
      if (builder.count == 0) {
         return null;
      }

      return new TextQuadRenderState(
         RenderPipelines.GUI_TEXTURED,
         TextureSetup.singleTexture(texture),
         new Matrix3x2f(guiGraphics.pose()),
         guiGraphics.peekScissorStack(),
         builder.vertices(),
         builder.colors(),
         builder.count
      );
   }

   @Override
   public void buildVertices(VertexConsumer consumer) {
      for (int i = 0; i < this.vertexCount; i++) {
         int offset = i * VERTEX_STRIDE;
         consumer.addVertexWith2DPose(this.pose, this.vertices[offset], this.vertices[offset + 1])
            .setUv(this.vertices[offset + 2], this.vertices[offset + 3])
            .setColor(this.colors[i]);
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return this.pipeline;
   }

   @Override
   public TextureSetup textureSetup() {
      return this.textureSetup;
   }

   @Nullable
   @Override
   public ScreenRectangle scissorArea() {
      return this.scissorArea;
   }

   @Nullable
   @Override
   public ScreenRectangle bounds() {
      return this.bounds;
   }

   @Nullable
   private static ScreenRectangle computeBounds(float[] vertices, int vertexCount, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;

      for (int i = 0; i < vertexCount; i++) {
         int offset = i * VERTEX_STRIDE;
         float vx = vertices[offset];
         float vy = vertices[offset + 1];
         minX = Math.min(minX, vx);
         minY = Math.min(minY, vy);
         maxX = Math.max(maxX, vx);
         maxY = Math.max(maxY, vy);
      }

      ScreenRectangle rectangle = new ScreenRectangle(
            Mth.floor(minX), Mth.floor(minY), Mth.ceil(maxX) - Mth.floor(minX), Mth.ceil(maxY) - Mth.floor(minY)
         )
         .transformMaxBounds(pose);
      return scissorArea != null ? scissorArea.intersection(rectangle) : rectangle;
   }

   private static final class Builder implements Font.VertexSink {
      private float[] vertices = new float[64 * VERTEX_STRIDE];
      private int[] colors = new int[64];
      private int count;

      @Override
      public void vertex(double x, double y, double u, double v, int argb) {
         if (this.count == this.colors.length) {
            int grown = this.colors.length * 2;
            float[] biggerVertices = new float[grown * VERTEX_STRIDE];
            System.arraycopy(this.vertices, 0, biggerVertices, 0, this.count * VERTEX_STRIDE);
            this.vertices = biggerVertices;
            int[] biggerColors = new int[grown];
            System.arraycopy(this.colors, 0, biggerColors, 0, this.count);
            this.colors = biggerColors;
         }

         int offset = this.count * VERTEX_STRIDE;
         this.vertices[offset] = (float)x;
         this.vertices[offset + 1] = (float)y;
         this.vertices[offset + 2] = (float)u;
         this.vertices[offset + 3] = (float)v;
         this.colors[this.count] = argb;
         this.count++;
      }

      float[] vertices() {
         if (this.vertices.length == this.count * VERTEX_STRIDE) {
            return this.vertices;
         }

         float[] trimmed = new float[this.count * VERTEX_STRIDE];
         System.arraycopy(this.vertices, 0, trimmed, 0, trimmed.length);
         return trimmed;
      }

      int[] colors() {
         if (this.colors.length == this.count) {
            return this.colors;
         }

         int[] trimmed = new int[this.count];
         System.arraycopy(this.colors, 0, trimmed, 0, this.count);
         return trimmed;
      }
   }
}
