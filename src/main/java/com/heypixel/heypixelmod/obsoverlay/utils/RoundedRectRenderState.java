package com.heypixel.heypixelmod.obsoverlay.utils;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

public final class RoundedRectRenderState implements GuiElementRenderState {
   private final float[] perimeter;
   private final Matrix3x2f pose;
   private final int color;
   @Nullable
   private final ScreenRectangle scissorArea;
   @Nullable
   private final ScreenRectangle bounds;
   private final RenderPipeline pipeline = RenderPipelines.GUI;
   private final TextureSetup textureSetup = TextureSetup.noTexture();

   public RoundedRectRenderState(
      float[] perimeter, Matrix3x2f pose, int color, @Nullable ScreenRectangle scissorArea
   ) {
      this.perimeter = perimeter;
      this.pose = pose;
      this.color = color;
      this.scissorArea = scissorArea;
      this.bounds = computeBounds(perimeter, pose, scissorArea);
   }

   @Nullable
   private static ScreenRectangle computeBounds(
      float[] perimeter, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea
   ) {
      int points = perimeter.length / 2;
      if (points < 3) {
         return null;
      }

      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;

      for (int i = 0; i < points; i++) {
         float px = perimeter[i * 2];
         float py = perimeter[i * 2 + 1];
         minX = Math.min(minX, px);
         minY = Math.min(minY, py);
         maxX = Math.max(maxX, px);
         maxY = Math.max(maxY, py);
      }

      ScreenRectangle rectangle = new ScreenRectangle(
            Mth.floor(minX), Mth.floor(minY), Mth.ceil(maxX - minX), Mth.ceil(maxY - minY)
         )
         .transformMaxBounds(pose);
      return scissorArea != null ? scissorArea.intersection(rectangle) : rectangle;
   }

   @Override
   public void buildVertices(VertexConsumer consumer) {
      int points = this.perimeter.length / 2;
      if (points < 3) {
         return;
      }

      float hubX = this.perimeter[0];
      float hubY = this.perimeter[1];
      float previousX = this.perimeter[(points - 1) * 2];
      float previousY = this.perimeter[(points - 1) * 2 + 1];

      for (int i = 0; i < points; i++) {
         float currentX = this.perimeter[i * 2];
         float currentY = this.perimeter[i * 2 + 1];

         consumer.addVertexWith2DPose(this.pose, hubX, hubY).setColor(this.color);
         consumer.addVertexWith2DPose(this.pose, previousX, previousY).setColor(this.color);
         consumer.addVertexWith2DPose(this.pose, currentX, currentY).setColor(this.color);
         consumer.addVertexWith2DPose(this.pose, hubX, hubY).setColor(this.color);

         previousX = currentX;
         previousY = currentY;
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
}
