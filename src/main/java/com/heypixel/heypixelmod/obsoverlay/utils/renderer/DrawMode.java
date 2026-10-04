package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

public enum DrawMode {
   Lines(2),
   Triangles(3);

   public final int indicesCount;

   private DrawMode(int indicesCount) {
      this.indicesCount = indicesCount;
   }

   public int getGL() {
      if (this == Lines) {
         return 1;
      } else {
         return this == Triangles ? 4 : 0;
      }
   }
}
