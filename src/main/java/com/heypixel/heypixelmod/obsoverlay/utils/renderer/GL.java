package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.heypixel.heypixelmod.obsoverlay.utils.ICapabilityTracker;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuDevice;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32C;

public class GL {
   private static final FloatBuffer MAT = BufferUtils.createFloatBuffer(16);
   private static final ByteBuffer BOOL4 = BufferUtils.createByteBuffer(4);
   private static final IntBuffer INT1 = BufferUtils.createIntBuffer(1);
   private static final Matrix4f PROJECTION = new Matrix4f();
   private static final float GUI_Z_TRANSLATION = -11000.0F;
   private static final Matrix4f MODEL_VIEW = new Matrix4f().translate(0.0F, 0.0F, GUI_Z_TRANSLATION);
   private static final ICapabilityTracker DEPTH = tracker(GL11.GL_DEPTH_TEST);
   private static final ICapabilityTracker BLEND = tracker(GL11.GL_BLEND);
   private static final ICapabilityTracker CULL = tracker(GL11.GL_CULL_FACE);
   private static final ICapabilityTracker SCISSOR = tracker(GL11.GL_SCISSOR_TEST);
   private static boolean depthSaved;
   private static boolean blendSaved;
   private static boolean cullSaved;
   private static boolean scissorSaved;
   public static int CURRENT_IBO;

   public static int genVertexArray() {
      return GlStateManager._glGenVertexArrays();
   }

   public static int genBuffer() {
      return GlStateManager._glGenBuffers();
   }

   public static int genTexture() {
      return GlStateManager._genTexture();
   }

   public static int genFramebuffer() {
      return GlStateManager.glGenFramebuffers();
   }

   public static void saveState() {
      depthSaved = DEPTH.get();
      blendSaved = BLEND.get();
      cullSaved = CULL.get();
      scissorSaved = SCISSOR.get();
   }

   public static void restoreState() {
      DEPTH.set(depthSaved);
      BLEND.set(blendSaved);
      CULL.set(cullSaved);
      SCISSOR.set(scissorSaved);
      disableLineSmooth();
   }


   public static void clearColor(float red, float green, float blue, float alpha) {
      GL11.glClearColor(red, green, blue, alpha);
   }

   public static void clearBuffer(int mask) {
      GlStateManager._clear(mask);
   }

   public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
      GlStateManager._colorMask(red, green, blue, alpha);
   }

   public static void depthMask(boolean mask) {
      GlStateManager._depthMask(mask);
   }

   public static void defaultBlendFunc() {
      GlStateManager._blendFuncSeparate(770, 771, 770, 771);
   }

   public static void stencilTest(boolean enabled) {
      if (enabled) {
         GlStateManager._enableStencilTest();
      } else {
         GlStateManager._disableStencilTest();
      }
   }

   private static int getInteger(int pname) {
      INT1.clear();
      GL11.glGetIntegerv(pname, INT1);
      return INT1.get(0);
   }

   public static GL.State captureState() {
      GL.State state = new GL.State();
      state.drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
      state.readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
      GL11.glGetIntegerv(GL11.GL_VIEWPORT, state.viewport);
      state.scissorTest = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
      GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, state.scissorBox);
      GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, BOOL4);
      for (int i = 0; i < 4; i++) {
         state.colorMask[i] = BOOL4.get(i) != 0;
      }

      state.depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      state.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
      state.blend = GL11.glIsEnabled(GL11.GL_BLEND);
      state.blendFunc[0] = getInteger(GL14.GL_BLEND_SRC_RGB);
      state.blendFunc[1] = getInteger(GL14.GL_BLEND_DST_RGB);
      state.blendFunc[2] = getInteger(GL14.GL_BLEND_SRC_ALPHA);
      state.blendFunc[3] = getInteger(GL14.GL_BLEND_DST_ALPHA);
      state.cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
      state.cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
      state.stencilTest = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
      state.stencilFunc[0] = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
      state.stencilFunc[1] = GL11.glGetInteger(GL11.GL_STENCIL_REF);
      state.stencilFunc[2] = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
      state.stencilOp[0] = GL11.glGetInteger(GL11.GL_STENCIL_FAIL);
      state.stencilOp[1] = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL);
      state.stencilOp[2] = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS);
      state.stencilWriteMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
      state.program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
      state.activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
      state.vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
      state.arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
      state.elementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);

      for (int slot = 0; slot < state.texture2d.length; slot++) {
         GlStateManager._activeTexture(33984 + slot);
         state.texture2d[slot] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      }

      GlStateManager._activeTexture(state.activeTexture);
      return state;
   }

   public static void restoreState(GL.State state) {
      if (state == null) {
         return;
      }

      GlStateManager._glBindVertexArray(state.vao);
      GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, state.arrayBuffer);
      if (state.vao != 0) {
         GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, state.elementBuffer);
      }

      GlStateManager._glUseProgram(state.program);

      for (int slot = 0; slot < state.texture2d.length; slot++) {
         GlStateManager._activeTexture(33984 + slot);
         GlStateManager._bindTexture(state.texture2d[slot]);
      }

      GlStateManager._activeTexture(state.activeTexture);
      GlStateManager._blendFuncSeparate(state.blendFunc[0], state.blendFunc[1], state.blendFunc[2], state.blendFunc[3]);
      if (state.blend) {
         GlStateManager._enableBlend();
      } else {
         GlStateManager._disableBlend();
      }

      GlStateManager._depthMask(state.depthMask);
      if (state.depthTest) {
         GlStateManager._enableDepthTest();
      } else {
         GlStateManager._disableDepthTest();
      }

      GlStateManager._colorMask(state.colorMask[0], state.colorMask[1], state.colorMask[2], state.colorMask[3]);
      if (state.cull) {
         GlStateManager._enableCull();
      } else {
         GlStateManager._disableCull();
      }

      GL11.glCullFace(state.cullFace);
      GlStateManager._stencilFunc(state.stencilFunc[0], state.stencilFunc[1], state.stencilFunc[2]);
      GlStateManager._stencilOp(state.stencilOp[0], state.stencilOp[1], state.stencilOp[2]);
      GlStateManager._stencilMask(state.stencilWriteMask);
      stencilTest(state.stencilTest);
      if (state.scissorTest) {
         GlStateManager._enableScissorTest();
      } else {
         GlStateManager._disableScissorTest();
      }

      GlStateManager._scissorBox(state.scissorBox[0], state.scissorBox[1], state.scissorBox[2], state.scissorBox[3]);
      viewport(state.viewport[0], state.viewport[1], state.viewport[2], state.viewport[3]);
      GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, state.drawFramebuffer);
      GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, state.readFramebuffer);
   }

   public static final class State {
      public final int[] viewport = new int[4];
      public final int[] scissorBox = new int[4];
      public final boolean[] colorMask = new boolean[4];
      public final int[] blendFunc = new int[4];
      public final int[] stencilFunc = new int[3];
      public final int[] stencilOp = new int[3];
      public final int[] texture2d = new int[2];
      public int drawFramebuffer;
      public int readFramebuffer;
      public boolean scissorTest;
      public boolean depthTest;
      public boolean depthMask;
      public boolean blend;
      public boolean cull;
      public boolean stencilTest;
      public int cullFace;
      public int stencilWriteMask;
      public int program;
      public int activeTexture;
      public int vao;
      public int arrayBuffer;
      public int elementBuffer;
   }

   public static void deleteBuffer(int buffer) {
      GlStateManager._glDeleteBuffers(buffer);
   }

   public static void deleteVertexArray(int vao) {
      GL30.glDeleteVertexArrays(vao);
   }

   public static void deleteShader(int shader) {
      GlStateManager.glDeleteShader(shader);
   }

   public static void deleteTexture(int id) {
      GlStateManager._deleteTexture(id);
   }

   public static void deleteFramebuffer(int fbo) {
      GlStateManager._glDeleteFramebuffers(fbo);
   }

   public static void deleteProgram(int program) {
      GlStateManager.glDeleteProgram(program);
   }

   public static void bindVertexArray(int vao) {
      if (vao >= 0) {
         try {
            GlStateManager._glBindVertexArray(vao);
         } catch (Exception var2) {
            System.err.println("Error binding VAO " + vao + ": " + var2.getMessage());
         }
      } else {
         System.err.println("WARNING: Attempted to bind invalid VAO: " + vao);
      }
   }

   public static void bindVertexBuffer(int vbo) {
      GlStateManager._glBindBuffer(34962, vbo);
   }

   public static void bindIndexBuffer(int ibo) {
      try {
         GlStateManager._glBindBuffer(34963, ibo);
      } catch (Exception var2) {
         System.err.println("Error binding IBO: " + var2.getMessage());
      }
   }

   public static void bindFramebuffer(int fbo) {
      GlStateManager._glBindFramebuffer(36160, fbo);
   }

   public static int getMainTargetFramebuffer() {
      RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
      GlTexture color = asGlTexture(target.getColorTexture());
      GpuDevice device = realDevice(RenderSystem.getDevice());
      if (color != null && device instanceof GlDevice glDevice) {
         return color.getFbo(glDevice.directStateAccess(), asGlTexture(target.getDepthTexture()));
      }

      return 0;
   }

   public static int getMainTargetColorTexture() {
      RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
      GlTexture color = asGlTexture(target.getColorTexture());
      return color != null ? color.glId() : -1;
   }

   public static GpuDevice realDevice(GpuDevice device) {
      return device instanceof ValidationGpuDevice validation ? realDevice(validation.getRealDevice()) : device;
   }

   public static GlTexture asGlTexture(GpuTexture texture) {
      if (texture instanceof GlTexture glTexture) {
         return glTexture;
      }

      return texture instanceof ValidationGpuTexture validation
         ? asGlTexture(validation.getRealTexture())
         : null;
   }

   public static void bufferData(int target, ByteBuffer data, int usage) {
      GlStateManager._glBufferData(target, data, usage);
   }

   public static void drawElements(int mode, int first, int type) {
      GlStateManager._drawElements(mode, first, type, 0L);
   }

   public static void enableVertexAttribute(int i) {
      RenderSystem.assertOnRenderThread();
      GL20.glEnableVertexAttribArray(i);
   }

   public static void vertexAttribute(int index, int size, int type, boolean normalized, int stride, long pointer) {
      GlStateManager._vertexAttribPointer(index, size, type, normalized, stride, pointer);
   }

   public static int createShader(int type) {
      return GlStateManager.glCreateShader(type);
   }

   public static void shaderSource(int shader, String source) {
      GlStateManager.glShaderSource(shader, source);
   }

   public static String compileShader(int shader) {
      GlStateManager.glCompileShader(shader);
      return GlStateManager.glGetShaderi(shader, 35713) == 0 ? GlStateManager.glGetShaderInfoLog(shader, 512) : null;
   }

   public static int createProgram() {
      return GlStateManager.glCreateProgram();
   }

   public static String linkProgram(int program, int vertShader, int fragShader) {
      GlStateManager.glAttachShader(program, vertShader);
      GlStateManager.glAttachShader(program, fragShader);
      GlStateManager.glLinkProgram(program);
      return GlStateManager.glGetProgrami(program, 35714) == 0 ? GlStateManager.glGetProgramInfoLog(program, 512) : null;
   }

   public static void useProgram(int program) {
      GlStateManager._glUseProgram(program);
   }

   public static void viewport(int x, int y, int width, int height) {
      GlStateManager._viewport(x, y, width, height);
   }

   public static int getUniformLocation(int program, String name) {
      return GlStateManager._glGetUniformLocation(program, name);
   }

   public static void uniformInt(int location, int v) {
      GlStateManager._glUniform1i(location, v);
   }

   public static void uniformFloat(int location, float v) {
      GL32C.glUniform1f(location, v);
   }

   public static void uniformFloat2(int location, float v1, float v2) {
      GL32C.glUniform2f(location, v1, v2);
   }

   public static void uniformFloat3(int location, float v1, float v2, float v3) {
      GL32C.glUniform3f(location, v1, v2, v3);
   }

   public static void uniformFloat4(int location, float v1, float v2, float v3, float v4) {
      GL32C.glUniform4f(location, v1, v2, v3, v4);
   }

   public static void uniformFloat3Array(int location, float[] v) {
      GL32C.glUniform3fv(location, v);
   }

   public static void uniformMatrix(int location, Matrix4f v) {
      MAT.clear();
      v.get(MAT);
      MAT.flip();
      GL20.glUniformMatrix4fv(location, false, MAT);
   }

   public static void pixelStore(int name, int param) {
      GlStateManager._pixelStore(name, param);
   }

   public static void textureParam(int target, int name, int param) {
      GlStateManager._texParameter(target, name, param);
   }

   public static void textureImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
      GL32C.glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
   }

   public static void defaultPixelStore() {
      pixelStore(3312, 0);
      pixelStore(3313, 0);
      pixelStore(3314, 0);
      pixelStore(32878, 0);
      pixelStore(3315, 0);
      pixelStore(3316, 0);
      pixelStore(32877, 0);
      pixelStore(3317, 4);
   }

   public static void generateMipmap(int target) {
      GL32C.glGenerateMipmap(target);
   }

   public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
      GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
   }

   public static void clear(int mask) {
      GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager._clear(mask);
   }

   public static void enableDepth() {
      GlStateManager._enableDepthTest();
   }

   public static void disableDepth() {
      GlStateManager._disableDepthTest();
   }

   public static void enableBlend() {
      GlStateManager._enableBlend();
      GlStateManager._blendFuncSeparate(770, 771, 770, 771);
   }

   public static void disableBlend() {
      GlStateManager._disableBlend();
   }

   public static void enableCull() {
      GlStateManager._enableCull();
   }

   public static void disableCull() {
      GlStateManager._disableCull();
   }

   public static void enableScissorTest() {
      GlStateManager._enableScissorTest();
   }

   public static void disableScissorTest() {
      GlStateManager._disableScissorTest();
   }

   public static void enableLineSmooth() {
      GL32C.glEnable(2848);
      GL32C.glLineWidth(1.0F);
   }

   public static void disableLineSmooth() {
      GL32C.glDisable(2848);
   }

   public static void bindTexture(ResourceLocation id) {
      GlStateManager._activeTexture(33984);
      try {
         AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
         if (texture.getTexture() instanceof GlTexture glTexture) {
            GlStateManager._bindTexture(glTexture.glId());
         }
      } catch (Exception var2) {
         System.err.println("Error binding texture " + id + ": " + var2.getMessage());
      }
   }

   public static void bindTexture(int i, int slot) {
      GlStateManager._activeTexture(33984 + slot);
      GlStateManager._bindTexture(i);
   }

   public static void bindTexture(int i) {
      bindTexture(i, 0);
   }

   public static void resetTextureSlot() {
      GlStateManager._activeTexture(33984);
   }

   public static Matrix4f getProjectionMatrix() {
      int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
      return PROJECTION.identity().setOrtho(0.0F, (float)width, (float)height, 0.0F, 1000.0F, 21000.0F);
   }

   public static Matrix4f getModelViewMatrix() {
      return MODEL_VIEW;
   }

   private static ICapabilityTracker tracker(int capability) {
      return new GL.CapabilityTracker(capability);
   }

   private static final class CapabilityTracker implements ICapabilityTracker {
      private final int capability;

      private CapabilityTracker(int capability) {
         this.capability = capability;
      }

      @Override
      public boolean get() {
         return GL11.glIsEnabled(this.capability);
      }

      @Override
      public void set(boolean state) {
         switch (this.capability) {
            case GL11.GL_DEPTH_TEST:
               if (state) {
                  GlStateManager._enableDepthTest();
               } else {
                  GlStateManager._disableDepthTest();
               }
               break;
            case GL11.GL_BLEND:
               if (state) {
                  GlStateManager._enableBlend();
               } else {
                  GlStateManager._disableBlend();
               }
               break;
            case GL11.GL_CULL_FACE:
               if (state) {
                  GlStateManager._enableCull();
               } else {
                  GlStateManager._disableCull();
               }
               break;
            case GL11.GL_SCISSOR_TEST:
               if (state) {
                  GlStateManager._enableScissorTest();
               } else {
                  GlStateManager._disableScissorTest();
               }
               break;
            case GL11.GL_STENCIL_TEST:
               if (state) {
                  GlStateManager._enableStencilTest();
               } else {
                  GlStateManager._disableStencilTest();
               }
               break;
            default:
               if (state) {
                  GL11.glEnable(this.capability);
               } else {
                  GL11.glDisable(this.capability);
               }
         }
      }
   }
}
