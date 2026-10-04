package com.heypixel.heypixelmod.obsoverlay.utils.renderer.text;

import com.heypixel.heypixelmod.obsoverlay.utils.renderer.GL;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

public class BufferedTexture {
   private int id = -1;

   public BufferedTexture(int width, int height, byte[] data, BufferedTexture.Format format, BufferedTexture.Filter filterMin, BufferedTexture.Filter filterMag) {
      ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
      buffer.put(data);
      buffer.flip();
      this.upload(width, height, buffer, format, filterMin, filterMag);
   }

   public BufferedTexture(
      int width, int height, ByteBuffer buffer, BufferedTexture.Format format, BufferedTexture.Filter filterMin, BufferedTexture.Filter filterMag
   ) {
      this.upload(width, height, buffer, format, filterMin, filterMag);
   }

   private void upload(
      int width, int height, ByteBuffer buffer, BufferedTexture.Format format, BufferedTexture.Filter filterMin, BufferedTexture.Filter filterMag
   ) {
      this.id = GL.genTexture();
      GL.bindTexture(this.id);
      GL.defaultPixelStore();
      GL.textureParam(3553, 10242, 10497);
      GL.textureParam(3553, 10243, 10497);
      GL.textureParam(3553, 10241, filterMin.toOpenGL());
      GL.textureParam(3553, 10240, filterMag.toOpenGL());
      buffer.rewind();
      GL.textureImage2D(3553, 0, format.toOpenGL(), width, height, 0, format.toOpenGL(), 5121, buffer);
      GL.bindTexture(0);
   }

   public int getId() {
      return this.id;
   }

   public void close() {
      if (this.id != -1) {
         GL.deleteTexture(this.id);
         this.id = -1;
      }
   }

   public static enum Filter {
      Nearest,
      Linear;

      public int toOpenGL() {
         return this == Nearest ? 9728 : 9729;
      }
   }

   public static enum Format {
      A,
      RGB,
      RGBA;

      public int toOpenGL() {
         if (this == A) {
            return 6403;
         } else if (this == RGB) {
            return 6407;
         } else {
            return this == RGBA ? 6408 : 0;
         }
      }
   }
}
