package com.heypixel.heypixelmod.obsoverlay.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.ColoredRectangleRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class RenderUtils {
   private static final Minecraft mc = Minecraft.getInstance();
   private static final AABB DEFAULT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);


   private static int alphaOf(int color) {
      return MathUtils.clamp(color >> 24 & 0xFF, 0, 255);
   }

   private static int redOf(int color) {
      return MathUtils.clamp(color >> 16 & 0xFF, 0, 255);
   }

   private static int greenOf(int color) {
      return MathUtils.clamp(color >> 8 & 0xFF, 0, 255);
   }

   private static int blueOf(int color) {
      return MathUtils.clamp(color & 0xFF, 0, 255);
   }

   public static int reAlpha(int color, float alpha) {
      int alphaChannel = MathUtils.clamp((int)(alpha * 255.0F), 0, 255);
      return ARGB.color(alphaChannel, redOf(color), greenOf(color), blueOf(color));
   }

   public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
      float hue = (float)((System.currentTimeMillis() + (long)index) % (long)((int)speed)) / speed;
      return Color.HSBtoRGB(hue, saturation, brightness);
   }


   private interface VertexWriter {
      void write(VertexConsumer consumer);
   }

   private static void submitMesh(Mode mode, RenderType renderType, VertexWriter writer) {
      BufferBuilder buffer = Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
      writer.write(buffer);
      MeshData mesh = buffer.build();
      if (mesh != null) {
         renderType.draw(mesh);
      }
   }

   private static void innerFill(Matrix4f matrix, float minX, float minY, float maxX, float maxY, int color) {
      float x0 = Math.min(minX, maxX);
      float x1 = Math.max(minX, maxX);
      float y0 = Math.min(minY, maxY);
      float y1 = Math.max(minY, maxY);
      diagDraws++;
      diagLast = "rect " + x0 + "," + y0 + ".." + x1 + "," + y1 + " color=" + Integer.toHexString(color);
      submitMesh(Mode.QUADS, RenderType.debugQuads(), consumer -> rectVertices(consumer, matrix, x0, y0, x1, y1, color));
   }

   public static volatile int diagDraws;
   public static volatile String diagLast = "-";

   private static void rectVertices(VertexConsumer consumer, Matrix4f matrix, float minX, float minY, float maxX, float maxY, int color) {
      consumer.addVertex(matrix, minX, minY, 0.0F).setColor(color);
      consumer.addVertex(matrix, minX, maxY, 0.0F).setColor(color);
      consumer.addVertex(matrix, maxX, maxY, 0.0F).setColor(color);
      consumer.addVertex(matrix, maxX, minY, 0.0F).setColor(color);
   }


   private static void submitGuiRect(GuiGraphics guiGraphics, Matrix3x2f pose, float minX, float minY, float maxX, float maxY, int color) {
      int x0 = Math.round(Math.min(minX, maxX));
      int y0 = Math.round(Math.min(minY, maxY));
      int x1 = Math.round(Math.max(minX, maxX));
      int y1 = Math.round(Math.max(minY, maxY));
      guiGraphics.submitGuiElementRenderState(
         new ColoredRectangleRenderState(
            RenderPipelines.GUI,
            TextureSetup.noTexture(),
            pose,
            x0,
            y0,
            x1,
            y1,
            color,
            color,
            guiGraphics.peekScissorStack()
         )
      );
   }

   private static void submitGuiPolygon(GuiGraphics guiGraphics, float[] perimeter, int color) {
      if (perimeter == null || perimeter.length < 6) {
         return;
      }

      guiGraphics.submitGuiElementRenderState(
         new RoundedRectRenderState(perimeter, new Matrix3x2f(guiGraphics.pose()), color, guiGraphics.peekScissorStack())
      );
   }

   private static float[] rectPerimeter(float minX, float minY, float maxX, float maxY) {
      return new float[]{minX, maxY, maxX, maxY, maxX, minY, minX, minY};
   }

   private static void fillRect(GuiGraphics guiGraphics, float minX, float minY, float maxX, float maxY, int color) {
      if (guiGraphics == null) {
         return;
      }

      float x0 = Math.min(minX, maxX);
      float x1 = Math.max(minX, maxX);
      float y0 = Math.min(minY, maxY);
      float y1 = Math.max(minY, maxY);
      float[] perimeter = rectPerimeter(x0, y0, x1, y1);

      if (StencilUtils.isWritingMask()) {
         StencilUtils.registerMask(perimeter);
         if (!StencilUtils.isMaskVisible()) {
            return;
         }
      } else if (StencilUtils.isClipping()) {
         float[] clipped = StencilUtils.clipToActiveMask(perimeter);
         if (clipped == null || clipped.length < 6) {
            return;
         }
         if (clipped != perimeter) {
            submitGuiPolygon(guiGraphics, clipped, color);
            return;
         }
      }

      submitGuiRect(guiGraphics, new Matrix3x2f(guiGraphics.pose()), x0, y0, x1, y1, color);
   }


   public static BlockPos getCameraBlockPos() {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      return camera.getBlockPosition();
   }

   public static Vec3 getCameraPos() {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      return camera.getPosition();
   }

   public static RegionPos getCameraRegion() {
      return RegionPos.of(getCameraBlockPos());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack) {
      applyRegionalRenderOffset(matrixStack, getCameraRegion());
   }

   public static void applyRegionalRenderOffset(PoseStack matrixStack, RegionPos region) {
      Vec3 offset = region.toVec3().subtract(getCameraPos());
      matrixStack.translate(offset.x, offset.y, offset.z);
   }


   public static void drawTracer(PoseStack poseStack, float x, float y, float size, float widthDiv, float heightDiv, int color) {
      Matrix4f matrix = poseStack.last().pose();
      float halfWidth = size / widthDiv;
      float height = size / heightDiv;
      submitMesh(Mode.TRIANGLE_FAN, RenderType.debugTriangleFan(), consumer -> {
         consumer.addVertex(matrix, x, y, 0.0F).setColor(color);
         consumer.addVertex(matrix, x - halfWidth, y + size, 0.0F).setColor(color);
         consumer.addVertex(matrix, x, y + height, 0.0F).setColor(color);
         consumer.addVertex(matrix, x + halfWidth, y + size, 0.0F).setColor(color);
      });
   }

   public static void drawTracer(GuiGraphics guiGraphics, float x, float y, float size, float widthDiv, float heightDiv, int color) {
      if (guiGraphics == null) {
         return;
      }

      float halfWidth = size / widthDiv;
      float height = size / heightDiv;
      submitGuiRect(guiGraphics, new Matrix3x2f(guiGraphics.pose()), x - halfWidth, y, x + halfWidth, y + Math.max(size, height), color);
   }

   public static void fill(PoseStack pPoseStack, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      innerFill(pPoseStack.last().pose(), pMinX, pMinY, pMaxX, pMaxY, pColor);
   }

   public static void fill(GuiGraphics guiGraphics, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
      fillRect(guiGraphics, pMinX, pMinY, pMaxX, pMaxY, pColor);
   }

   public static void drawRectBound(PoseStack poseStack, float x, float y, float width, float height, int color) {
      innerFill(poseStack.last().pose(), x, y, x + width, y + height, color);
   }

   public static void drawRectBound(GuiGraphics guiGraphics, float x, float y, float width, float height, int color) {
      fillRect(guiGraphics, x, y, x + width, y + height, color);
   }

   private static float clampRadius(float width, float height, float edgeRadius) {
      float radius = Math.max(edgeRadius, 0.0F);
      if (radius > width / 2.0F) {
         radius = width / 2.0F;
      }

      if (radius > height / 2.0F) {
         radius = height / 2.0F;
      }

      return radius;
   }

   private static final int CORNER_SAMPLES = 45;

   private static float[] buildRoundedRectPerimeter(
      float x, float y, float width, float height, float radius
   ) {
      float toX = x + width;
      float toY = y + height;
      float[][] map = new float[][]{
         {toX - radius, toY - radius},
         {toX - radius, y + radius},
         {x + radius, y + radius},
         {x + radius, toY - radius}
      };

      FloatList perimeter = new FloatList();
      float step = 90.0F / (float)Math.max(1, CORNER_SAMPLES);

      for (int i = 0; i < 4; i++) {
         float centerX = map[i][0];
         float centerY = map[i][1];
         float startDeg = i * 90.0F;
         float endDeg = startDeg + 90.0F;

         for (float deg = startDeg; deg < endDeg; deg += step) {
            arcVertex(perimeter, centerX, centerY, radius, deg);
         }

         arcVertex(perimeter, centerX, centerY, radius, endDeg);
      }

      return perimeter.toArray();
   }

   private static void arcVertex(FloatList out, float centerX, float centerY, float radius, float degrees) {
      float radians = (float)Math.toRadians(degrees);
      float sin = (float)(Math.sin(radians) * (double)radius);
      float cos = (float)(Math.cos(radians) * (double)radius);
      out.add(centerX + sin, centerY + cos);
   }

   public static void drawRoundedRect(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int color) {
      if (width == 0.0F || height == 0.0F) {
         return;
      }

      if (width < 0.0F) {
         x += width;
         width = -width;
      }
      if (height < 0.0F) {
         y += height;
         height = -height;
      }

      if (color == 0xFFFFFF) {
         color = 0xFFFFFFFF;
      }

      float radius = clampRadius(width, height, edgeRadius);
      if (radius <= 0.0F) {
         fill(poseStack, x, y, x + width, y + height, color);
         return;
      }

      final float drawX = x;
      final float drawY = y;
      final float drawWidth = width;
      final float drawHeight = height;
      final int drawColor = color;
      final float[] perimeter = buildRoundedRectPerimeter(drawX, drawY, drawWidth, drawHeight, radius);
      Matrix4f matrix = poseStack.last().pose();
      diagDraws++;
      diagLast = "roundRect " + drawX + "," + drawY + " " + drawWidth + "x" + drawHeight + " color=" + Integer.toHexString(drawColor);
      submitMesh(Mode.QUADS, RenderType.debugQuads(), consumer -> writeFanQuads(consumer, matrix, perimeter, drawColor));
   }

   public static void drawRoundedRect(GuiGraphics guiGraphics, float x, float y, float width, float height, float edgeRadius, int color) {
      if (guiGraphics == null || width == 0.0F || height == 0.0F) {
         return;
      }

      if (width < 0.0F) {
         x += width;
         width = -width;
      }
      if (height < 0.0F) {
         y += height;
         height = -height;
      }

      if (color == 0xFFFFFF) {
         color = 0xFFFFFFFF;
      }

      float radius = clampRadius(width, height, edgeRadius);
      if (radius <= 0.0F) {
         fillRect(guiGraphics, x, y, x + width, y + height, color);
         return;
      }

      float[] perimeter = buildRoundedRectPerimeter(x, y, width, height, radius);
      if (StencilUtils.isWritingMask()) {
         StencilUtils.registerMask(perimeter);
         if (!StencilUtils.isMaskVisible()) {
            return;
         }
      } else if (StencilUtils.isClipping()) {
         float[] clipped = StencilUtils.clipToActiveMask(perimeter);
         if (clipped == null || clipped.length < 6) {
            return;
         }
         perimeter = clipped;
      }

      submitGuiPolygon(guiGraphics, perimeter, color);
   }

   private static void writeFanQuads(VertexConsumer consumer, Matrix4f matrix, float[] perimeter, int color) {
      int points = perimeter.length / 2;
      if (points < 3) {
         return;
      }

      float hubX = perimeter[0];
      float hubY = perimeter[1];
      float previousX = perimeter[(points - 1) * 2];
      float previousY = perimeter[(points - 1) * 2 + 1];

      for (int i = 0; i < points; i++) {
         float currentX = perimeter[i * 2];
         float currentY = perimeter[i * 2 + 1];
         consumer.addVertex(matrix, hubX, hubY, 0.0F).setColor(color);
         consumer.addVertex(matrix, previousX, previousY, 0.0F).setColor(color);
         consumer.addVertex(matrix, currentX, currentY, 0.0F).setColor(color);
         consumer.addVertex(matrix, hubX, hubY, 0.0F).setColor(color);
         previousX = currentX;
         previousY = currentY;
      }
   }


   public static void drawSolidBox(PoseStack matrixStack) {
      drawSolidBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawSolidBox(AABB bb, PoseStack matrixStack) {
      Matrix4f matrix = matrixStack.last().pose();
      submitMesh(Mode.QUADS, RenderType.debugQuads(), consumer -> solidBoxVertices(consumer, matrix, bb, -1));
   }

   public static void drawOutlinedBox(PoseStack matrixStack) {
      drawOutlinedBox(DEFAULT_BOX, matrixStack);
   }

   public static void drawOutlinedBox(AABB bb, PoseStack matrixStack) {
      Matrix4f matrix = matrixStack.last().pose();
      submitMesh(Mode.DEBUG_LINES, RenderType.lines(), consumer -> outlinedBoxVertices(consumer, matrix, bb, -1));
   }

   public static void drawSolidBox(AABB bb, BufferBuilder bufferBuilder) {
      solidBoxVertices(bufferBuilder, null, bb, -1);
   }

   public static void drawOutlinedBox(AABB bb, BufferBuilder bufferBuilder) {
      outlinedBoxVertices(bufferBuilder, null, bb, -1);
   }

   private static void solidBoxVertices(VertexConsumer consumer, Matrix4f matrix, AABB bb, int color) {
      float minX = (float)bb.minX;
      float minY = (float)bb.minY;
      float minZ = (float)bb.minZ;
      float maxX = (float)bb.maxX;
      float maxY = (float)bb.maxY;
      float maxZ = (float)bb.maxZ;
      quad(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color);
      quad(consumer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
      quad(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
      quad(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
      quad(consumer, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
      quad(consumer, matrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, color);
   }

   private static void outlinedBoxVertices(VertexConsumer consumer, Matrix4f matrix, AABB bb, int color) {
      float minX = (float)bb.minX;
      float minY = (float)bb.minY;
      float minZ = (float)bb.minZ;
      float maxX = (float)bb.maxX;
      float maxY = (float)bb.maxY;
      float maxZ = (float)bb.maxZ;
      line(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, color);
      line(consumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, color);
      line(consumer, matrix, maxX, minY, maxZ, minX, minY, maxZ, color);
      line(consumer, matrix, minX, minY, maxZ, minX, minY, minZ, color);
      line(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, color);
      line(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, color);
      line(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, color);
      line(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, color);
      line(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, color);
      line(consumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, color);
      line(consumer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, color);
      line(consumer, matrix, minX, maxY, maxZ, minX, maxY, minZ, color);
   }

   private static void quad(
      VertexConsumer consumer,
      Matrix4f matrix,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      int color
   ) {
      if (matrix != null) {
         consumer.addVertex(matrix, x0, y0, z0).setColor(color);
         consumer.addVertex(matrix, x1, y1, z1).setColor(color);
         consumer.addVertex(matrix, x2, y2, z2).setColor(color);
         consumer.addVertex(matrix, x3, y3, z3).setColor(color);
      } else {
         consumer.addVertex(x0, y0, z0).setColor(color);
         consumer.addVertex(x1, y1, z1).setColor(color);
         consumer.addVertex(x2, y2, z2).setColor(color);
         consumer.addVertex(x3, y3, z3).setColor(color);
      }
   }

   private static void line(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float dz = z1 - z0;
      float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      float nx = length > 0.0F ? dx / length : 1.0F;
      float ny = length > 0.0F ? dy / length : 0.0F;
      float nz = length > 0.0F ? dz / length : 0.0F;
      if (matrix != null) {
         consumer.addVertex(matrix, x0, y0, z0).setColor(color).setNormal(nx, ny, nz);
         consumer.addVertex(matrix, x1, y1, z1).setColor(color).setNormal(nx, ny, nz);
      } else {
         consumer.addVertex(x0, y0, z0).setColor(color).setNormal(nx, ny, nz);
         consumer.addVertex(x1, y1, z1).setColor(color).setNormal(nx, ny, nz);
      }
   }


   public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
      return (float)mouseX > xLeft && (float)mouseX < xRight && (float)mouseY > yUp && (float)mouseY < yBottom;
   }

   public static boolean isHoveringBound(int mouseX, int mouseY, float xLeft, float yUp, float width, float height) {
      return (float)mouseX > xLeft && (float)mouseX < xLeft + width && (float)mouseY > yUp && (float)mouseY < yUp + height;
   }

   public static void fillBound(PoseStack stack, float left, float top, float width, float height, int color) {
      fill(stack, left, top, left + width, top + height, color);
   }

   public static void fillBound(GuiGraphics guiGraphics, float left, float top, float width, float height, int color) {
      fill(guiGraphics, left, top, left + width, top + height, color);
   }

   public static void 装女人(BufferBuilder bufferBuilder, Matrix4f matrix, AABB box) {
      Vec3 camera = mc.getEntityRenderDispatcher().camera.getPosition();
      float minX = (float)(box.minX - camera.x());
      float minY = (float)(box.minY - camera.y());
      float minZ = (float)(box.minZ - camera.z());
      float maxX = (float)(box.maxX - camera.x());
      float maxY = (float)(box.maxY - camera.y());
      float maxZ = (float)(box.maxZ - camera.z());
      quad(bufferBuilder, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, -1);
      quad(bufferBuilder, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, -1);
      quad(bufferBuilder, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, -1);
      quad(bufferBuilder, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, -1);
      quad(bufferBuilder, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, -1);
      quad(bufferBuilder, matrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1);
   }
}
