package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;

public class Shader {
   public static Shader BOUND;
   private final int id;
   private final Object2IntMap<String> uniformLocations = new Object2IntOpenHashMap();

   public Shader(String vertPath, String fragPath) {
      int vert = GL.createShader(35633);
      GL.shaderSource(vert, this.read(vertPath));
      String vertError = GL.compileShader(vert);
      if (vertError != null) {
         throw new RuntimeException("Failed to compile vertex shader (" + vertPath + "): " + vertError);
      } else {
         int frag = GL.createShader(35632);
         GL.shaderSource(frag, this.read(fragPath));
         String fragError = GL.compileShader(frag);
         if (fragError != null) {
            throw new RuntimeException("Failed to compile fragment shader (" + fragPath + "): " + fragError);
         } else {
            this.id = GL.createProgram();
            String programError = GL.linkProgram(this.id, vert, frag);
            if (programError != null) {
               throw new RuntimeException("Failed to link program: " + programError);
            } else {
               GL.deleteShader(vert);
               GL.deleteShader(frag);
            }
         }
      }
   }

   private String read(String path) {
      try {
         return IOUtils.toString(this.getClass().getResourceAsStream("/assets/heypixel/VcX6svVqmeT8/shader/" + path), StandardCharsets.UTF_8);
      } catch (IOException var3) {
         var3.printStackTrace();
         return "";
      }
   }

   public void bind() {
      GL.useProgram(this.id);
      BOUND = this;
   }

   private int getLocation(String name) {
      if (this.uniformLocations.containsKey(name)) {
         return this.uniformLocations.getInt(name);
      } else {
         int location = GL.getUniformLocation(this.id, name);
         this.uniformLocations.put(name, location);
         return location;
      }
   }

   public void set(String name, boolean v) {
      GL.uniformInt(this.getLocation(name), v ? 1 : 0);
   }

   public void set(String name, int v) {
      GL.uniformInt(this.getLocation(name), v);
   }

   public void set(String name, double v) {
      GL.uniformFloat(this.getLocation(name), (float)v);
   }

   public void set(String name, double v1, double v2) {
      GL.uniformFloat2(this.getLocation(name), (float)v1, (float)v2);
   }

   public void set(String name, Matrix4f mat) {
      GL.uniformMatrix(this.getLocation(name), mat);
   }

   public void set(String name, float... args) {
      int location = this.getLocation(name);
      if (args.length == 1) {
         GL.uniformFloat(location, args[0]);
      } else if (args.length == 2) {
         GL.uniformFloat2(location, args[0], args[1]);
      } else if (args.length == 3) {
         GL.uniformFloat3(location, args[0], args[1], args[2]);
      } else {
         if (args.length != 4) {
            throw new IllegalArgumentException("Invalid number of arguments for uniform '" + name + "'");
         }

         GL.uniformFloat4(location, args[0], args[1], args[2], args[3]);
      }
   }

   public void setDefaults() {
      this.set("u_Proj", GL.getProjectionMatrix());
      this.set("u_ModelView", GL.getModelViewMatrix());
   }
}
