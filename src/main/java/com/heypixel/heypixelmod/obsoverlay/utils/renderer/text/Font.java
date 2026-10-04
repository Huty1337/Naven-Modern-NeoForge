package com.heypixel.heypixelmod.obsoverlay.utils.renderer.text;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.stb.STBTTPackedchar.Buffer;
import org.lwjgl.system.MemoryStack;

public class Font {
   private final int height;
   private final float scale;
   private final float ascent;
   private final CharData[] charData;
   private final int from;
   private final int textureSize;
   private final HashMap<String, Double> widthCache = new HashMap<>();
   private ResourceLocation guiTextureLocation;

   public Font(ByteBuffer buffer, int height, int charRangeFrom, int charRangeTo, int textureSize) {
      this(buffer, height, charRangeFrom, charRangeTo, textureSize, null);
   }

   public Font(ByteBuffer buffer, int height, int charRangeFrom, int charRangeTo, int textureSize, String name) {
      this.height = height;
      this.from = charRangeFrom;
      this.textureSize = textureSize;
      STBTTFontinfo fontInfo = STBTTFontinfo.create();
      STBTruetype.stbtt_InitFont(fontInfo, buffer);
      this.charData = new CharData[charRangeTo + 1 - charRangeFrom];
      Buffer cdata = STBTTPackedchar.create(this.charData.length);
      ByteBuffer bitmap = BufferUtils.createByteBuffer(textureSize * textureSize);
      STBTTPackContext packContext = STBTTPackContext.create();
      STBTruetype.stbtt_PackBegin(packContext, bitmap, textureSize, textureSize, 0, 1);
      STBTruetype.stbtt_PackSetOversampling(packContext, 2, 2);
      STBTruetype.stbtt_PackFontRange(packContext, buffer, 0, (float)height, this.from, cdata);
      STBTruetype.stbtt_PackEnd(packContext);
      this.scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, (float)height);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer ascent = stack.mallocInt(1);
         STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
         this.ascent = (float)ascent.get(0);
      } catch (Throwable var15) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }
         }

         throw var15;
      }

      if (stack != null) {
         stack.close();
      }

      for (int i = 0; i < this.charData.length; i++) {
         STBTTPackedchar packedChar = (STBTTPackedchar)cdata.get(i);
         float ipw = 1.0F / (float)textureSize;
         float iph = 1.0F / (float)textureSize;
         this.charData[i] = new CharData(
            packedChar.xoff(),
            packedChar.yoff(),
            packedChar.xoff2(),
            packedChar.yoff2(),
            (float)packedChar.x0() * ipw,
            (float)packedChar.y0() * iph,
            (float)packedChar.x1() * ipw,
            (float)packedChar.y1() * iph,
            packedChar.xadvance()
         );
      }

      if (name != null) {
         this.guiTextureLocation = this.registerGuiTexture(name, bitmap, textureSize);
      }
   }

   private ResourceLocation registerGuiTexture(String name, ByteBuffer bitmap, int size) {
      NativeImage image = null;

      try {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft == null || minecraft.getTextureManager() == null) {
            System.err.println("[Naven] Cannot register font atlas texture - texture manager unavailable: " + name);
            return null;
         }

         image = new NativeImage(NativeImage.Format.RGBA, size, size, false);
         long pixels = ((com.heypixel.heypixelmod.mixin.O.accessors.NativeImageAccessor)(Object)image).getPixels();
         ByteBuffer source = bitmap.duplicate();
         source.clear();

         for (int i = 0; i < size * size; i++) {
            int coverage = source.get(i) & 255;
            org.lwjgl.system.MemoryUtil.memPutInt(pixels + (long)i * 4L, coverage << 24 | 16777215);
         }

         ResourceLocation location = ResourceLocation.fromNamespaceAndPath("naven", "font/" + name);
         DynamicTexture texture = new DynamicTexture(() -> "naven-font-" + name, image);
         image = null;
         texture.setFilter(true, false);
         minecraft.getTextureManager().register(location, texture);
         return location;
      } catch (Throwable var8) {
         if (image != null) {
            image.close();
         }

         System.err.println("[Naven] Failed to register font atlas texture " + name + ": " + var8);
         var8.printStackTrace(System.err);
         return null;
      }
   }

   public ResourceLocation getTextureLocation() {
      return this.guiTextureLocation;
   }

   public int getTextureSize() {
      return this.textureSize;
   }

   private int clampGlyphIndex(int index) {
      return index >= 0 && index < this.charData.length ? index : 0;
   }

   public double getWidth(String string) {
      if (this.widthCache.containsKey(string)) {
         return this.widthCache.get(string);
      } else {
         double width = 0.0;

         for (int i = 0; i < string.length(); i++) {
            int cp = string.charAt(i) - this.from;
            if (cp == 167 && i + 1 < string.length()) {
               char ctrl = string.charAt(i + 1);
               if ((ctrl == 'x' || ctrl == 'X') && isLegacyHexColorSequence(string, i)) {
                  i += 13;
               } else {
                  i++;
               }
            } else {
               cp = this.clampGlyphIndex(cp);
               CharData c = this.charData[cp];
               width += (double)c.xAdvance;
            }
         }

         this.widthCache.put(string, width);
         return width;
      }
   }

   public double getHeight() {
      return (double)this.height;
   }

   public interface VertexSink {
      void vertex(double x, double y, double u, double v, int argb);
   }

   public double emit(VertexSink sink, String string, double x, double y, Color color, double scale, boolean shadow, float alpha) {
      Color currentColor = color;
      y += (double)(this.ascent * this.scale) * scale;

      for (int i = 0; i < string.length(); i++) {
         int cp = string.charAt(i) - this.from;
         if (cp == 167 && i + 1 < string.length()) {
            char ctrl = string.charAt(i + 1);
            if ((ctrl == 'x' || ctrl == 'X') && isLegacyHexColorSequence(string, i)) {
               if (!shadow) {
                  int rgb = parseLegacyHexColor(string, i);
                  if (rgb != -1) {
                     currentColor = new Color(rgb);
                  }
               }

               i += 13;
               continue;
            }

            ChatFormatting byCode = ChatFormatting.getByCode(ctrl);
            if (byCode != null && !shadow) {
               if (byCode == ChatFormatting.RESET) {
                  currentColor = color;
               } else if (byCode.isColor()) {
                  currentColor = new Color(byCode.getColor());
               }
            }

            i++;
         } else {
            cp = this.clampGlyphIndex(cp);
            CharData c = this.charData[cp];
            int argb = toArgb(currentColor, alpha);
            sink.vertex(x + (double)c.x0 * scale, y + (double)c.y0 * scale, (double)c.u0, (double)c.v0, argb);
            sink.vertex(x + (double)c.x0 * scale, y + (double)c.y1 * scale, (double)c.u0, (double)c.v1, argb);
            sink.vertex(x + (double)c.x1 * scale, y + (double)c.y1 * scale, (double)c.u1, (double)c.v1, argb);
            sink.vertex(x + (double)c.x1 * scale, y + (double)c.y0 * scale, (double)c.u1, (double)c.v0, argb);
            x += (double)c.xAdvance * scale;
         }
      }

      return x;
   }

   private static int toArgb(Color color, float alpha) {
      int a = Math.round((float)color.getAlpha() * alpha);
      a = Math.max(0, Math.min(255, a));
      return a << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
   }

   private static boolean isLegacyHexColorSequence(String string, int startIndex) {
      if (startIndex + 13 >= string.length()) {
         return false;
      }

      if (string.charAt(startIndex) != 167) {
         return false;
      }

      char x = string.charAt(startIndex + 1);
      if (x != 'x' && x != 'X') {
         return false;
      }

      for (int pair = 0; pair < 6; pair++) {
         int sectionIndex = startIndex + 2 + pair * 2;
         if (string.charAt(sectionIndex) != 167) {
            return false;
         }

         char hexDigit = string.charAt(sectionIndex + 1);
         if (Character.digit(hexDigit, 16) == -1) {
            return false;
         }
      }

      return true;
   }

   private static int parseLegacyHexColor(String string, int startIndex) {
      if (!isLegacyHexColorSequence(string, startIndex)) {
         return -1;
      }

      int rgb = 0;
      for (int i = 0; i < 6; i++) {
         char digitChar = string.charAt(startIndex + 3 + i * 2);
         int digit = Character.digit(digitChar, 16);
         rgb = (rgb << 4) | digit;
      }

      return rgb;
   }
}
