package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

public class PostProcessRenderer {
   private static Mesh mesh;

   public static void init() {
      if (mesh != null) {
         return;
      }

      Mesh created = new Mesh(DrawMode.Triangles, Mesh.Attrib.Vec2);
      created.begin();
      created.quad(
         created.vec2(-1.0, -1.0).next(), created.vec2(-1.0, 1.0).next(), created.vec2(1.0, 1.0).next(), created.vec2(1.0, -1.0).next()
      );
      created.end();
      mesh = created;
   }

   public static boolean isReady() {
      return mesh != null;
   }

   public static void beginRender(PoseStack stack) {
      init();
      mesh.beginRender(stack);
   }

   public static void render(PoseStack stack) {
      init();
      mesh.render(stack);
   }

   public static void endRender() {
      if (mesh != null) {
         mesh.endRender();
      }
   }
}
