package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

public class Mesh {
   public boolean depthTest = false;
   public double alpha = 1.0;
   private final DrawMode drawMode;
   private final int primitiveVerticesSize;
   private final int vao;
   private final int vbo;
   private final int ibo;
   private ByteBuffer vertices;
   private long verticesPointer;
   private int vertexComponentCount;
   private ByteBuffer indices;
   private long indicesPointer;
   private int vertexI;
   private int indicesCount;
   private boolean building;
   private double cameraX;
   private double cameraZ;
   private boolean beganRendering;

   public Mesh(DrawMode drawMode, Mesh.Attrib... attributes) {
      int stride = 0;

      for (Mesh.Attrib attribute : attributes) {
         stride += attribute.size * 4;
      }

      this.drawMode = drawMode;
      this.primitiveVerticesSize = stride * drawMode.indicesCount;
      this.vertices = BufferUtils.createByteBuffer(this.primitiveVerticesSize * 256 * 4);
      this.verticesPointer = MemoryUtil.memAddress0(this.vertices);
      this.indices = BufferUtils.createByteBuffer(drawMode.indicesCount * 512 * 4);
      this.indicesPointer = MemoryUtil.memAddress0(this.indices);
      this.vao = GL.genVertexArray();
      GL.bindVertexArray(this.vao);
      this.vbo = GL.genBuffer();
      GL.bindVertexBuffer(this.vbo);
      this.ibo = GL.genBuffer();
      GL.bindIndexBuffer(this.ibo);
      int offset = 0;

      for (int i = 0; i < attributes.length; i++) {
         int attribute = attributes[i].size;
         GL.enableVertexAttribute(i);
         GL.vertexAttribute(i, attribute, 5126, false, stride, (long)offset);
         offset += attribute * 4;
      }

      GL.bindVertexArray(0);
      GL.bindVertexBuffer(0);
      GL.bindIndexBuffer(0);
   }

   public void begin() {
      if (this.building) {
         throw new IllegalStateException("Mesh.end() called while already building.");
      } else {
         this.vertexComponentCount = 0;
         this.vertexI = 0;
         this.indicesCount = 0;
         this.building = true;
         this.cameraX = 0.0;
         this.cameraZ = 0.0;
      }
   }

   public Mesh vec3(double x, double y, double z) {
      long p = this.verticesPointer + (long)this.vertexComponentCount * 4L;
      MemoryUtil.memPutFloat(p, (float)(x - this.cameraX));
      MemoryUtil.memPutFloat(p + 4L, (float)y);
      MemoryUtil.memPutFloat(p + 8L, (float)(z - this.cameraZ));
      this.vertexComponentCount += 3;
      return this;
   }

   public Mesh vec2(double x, double y) {
      long p = this.verticesPointer + (long)this.vertexComponentCount * 4L;
      MemoryUtil.memPutFloat(p, (float)x);
      MemoryUtil.memPutFloat(p + 4L, (float)y);
      this.vertexComponentCount += 2;
      return this;
   }

   public Mesh color(Color c) {
      long p = this.verticesPointer + (long)this.vertexComponentCount * 4L;
      MemoryUtil.memPutFloat(p, (float)c.getRed() / 255.0F);
      MemoryUtil.memPutFloat(p + 4L, (float)c.getGreen() / 255.0F);
      MemoryUtil.memPutFloat(p + 8L, (float)c.getBlue() / 255.0F);
      MemoryUtil.memPutFloat(p + 12L, (float)c.getAlpha() / 255.0F * (float)this.alpha);
      this.vertexComponentCount += 4;
      return this;
   }

   public int next() {
      return this.vertexI++;
   }

   public void line(int i1, int i2) {
      long p = this.indicesPointer + (long)this.indicesCount * 4L;
      MemoryUtil.memPutInt(p, i1);
      MemoryUtil.memPutInt(p + 4L, i2);
      this.indicesCount += 2;
      this.growIfNeeded();
   }

   public void quad(int i1, int i2, int i3, int i4) {
      long p = this.indicesPointer + (long)this.indicesCount * 4L;
      MemoryUtil.memPutInt(p, i1);
      MemoryUtil.memPutInt(p + 4L, i2);
      MemoryUtil.memPutInt(p + 8L, i3);
      MemoryUtil.memPutInt(p + 12L, i3);
      MemoryUtil.memPutInt(p + 16L, i4);
      MemoryUtil.memPutInt(p + 20L, i1);
      this.indicesCount += 6;
      this.growIfNeeded();
   }

   public void growIfNeeded() {
      if ((this.vertexI + 1) * this.primitiveVerticesSize >= this.vertices.capacity()) {
         int newSize = this.vertices.capacity() * 2;
         if (newSize % this.primitiveVerticesSize != 0) {
            newSize += newSize % this.primitiveVerticesSize;
         }

         ByteBuffer newVertices = BufferUtils.createByteBuffer(newSize);
         MemoryUtil.memCopy(MemoryUtil.memAddress0(this.vertices), MemoryUtil.memAddress0(newVertices), (long)this.vertexComponentCount * 4L);
         this.vertices = newVertices;
         this.verticesPointer = MemoryUtil.memAddress0(this.vertices);
      }

      if (this.indicesCount * 4 >= this.indices.capacity()) {
         int newSize = this.indices.capacity() * 2;
         if (newSize % this.drawMode.indicesCount != 0) {
            newSize += newSize % (this.drawMode.indicesCount * 4);
         }

         ByteBuffer newIndices = BufferUtils.createByteBuffer(newSize);
         MemoryUtil.memCopy(MemoryUtil.memAddress0(this.indices), MemoryUtil.memAddress0(newIndices), (long)this.indicesCount * 4L);
         this.indices = newIndices;
         this.indicesPointer = MemoryUtil.memAddress0(this.indices);
      }
   }

   public void end() {
      if (!this.building) {
         throw new IllegalStateException("Mesh.end() called while not building.");
      } else {
         if (this.indicesCount > 0) {
            GL.bindVertexBuffer(this.vbo);
            GL.bufferData(34962, this.vertices.limit(this.vertexComponentCount * 4), 35048);
            GL.bindVertexBuffer(0);
            GL.bindIndexBuffer(this.ibo);
            GL.bufferData(34963, this.indices.limit(this.indicesCount * 4), 35048);
            GL.bindIndexBuffer(0);
         }

         this.building = false;
      }
   }

   public void beginRender(PoseStack matrices) {
      if (this.depthTest) {
         GL.enableDepth();
      } else {
         GL.disableDepth();
      }

      GL.enableBlend();
      GL.disableCull();
      GL.enableLineSmooth();
      this.beganRendering = true;
   }

   public void render(PoseStack matrices) {
      if (this.building) {
         this.end();
      }

      if (this.indicesCount > 0) {
         boolean wasBeganRendering = this.beganRendering;
         if (!wasBeganRendering) {
            this.beginRender(matrices);
         }

         this.beforeRender();
         Shader.BOUND.setDefaults();
         GL.bindVertexArray(this.vao);
         GL.drawElements(this.drawMode.getGL(), this.indicesCount, 5125);
         GL.bindVertexArray(0);
         if (!wasBeganRendering) {
            this.endRender();
         }
      }
   }

   public void endRender() {
      if (this.depthTest) {
         GL.disableBlend();
      } else {
         GL.enableBlend();
      }

      GL.disableBlend();
      GL.enableCull();
      GL.disableLineSmooth();
      this.beganRendering = false;
   }

   protected void beforeRender() {
   }

   public static enum Attrib {
      Float(1),
      Vec2(2),
      Vec3(3),
      Color(4);

      public final int size;

      private Attrib(int size) {
         this.size = size;
      }
   }
}
