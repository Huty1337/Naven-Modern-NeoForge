package com.heypixel.heypixelmod.obsoverlay.utils.renderer;

public class ShaderMesh extends Mesh {
   private final Shader shader;

   public ShaderMesh(Shader shader, DrawMode drawMode, Mesh.Attrib... attributes) {
      super(drawMode, attributes);
      this.shader = shader;
   }

   @Override
   protected void beforeRender() {
      this.shader.bind();
   }
}
