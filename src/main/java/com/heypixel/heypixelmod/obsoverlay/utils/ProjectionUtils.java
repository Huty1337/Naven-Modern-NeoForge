package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.mixin.O.accessors.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ProjectionUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   public static Vector2f project(double x, double y, double z, float renderPartialTicks) {
      Vec3 camera_pos = mc.getEntityRenderDispatcher().camera.getPosition();
      Quaternionf cameraRotation = new Quaternionf(mc.getEntityRenderDispatcher().camera.rotation());
      cameraRotation.conjugate();
      Vector3f result3f = new Vector3f((float)(x - camera_pos.x), (float)(y - camera_pos.y), (float)(z - camera_pos.z));
      result3f.rotate(cameraRotation);
      if ((Boolean)mc.options.bobView().get() && mc.getCameraEntity() instanceof AbstractClientPlayer playerentity) {
         calculateViewBobbing(playerentity, result3f, renderPartialTicks);
      }

      double fov = ((GameRendererAccessor)mc.gameRenderer).invokeGetFov(mc.getEntityRenderDispatcher().camera, renderPartialTicks, true);
      return calculateScreenPosition(result3f, fov);
   }

   private static void calculateViewBobbing(AbstractClientPlayer playerentity, Vector3f result3f, float renderPartialTicks) {
      ClientAvatarState avatarState = playerentity.avatarState();
      float f1 = avatarState.getBackwardsInterpolatedWalkDistance(renderPartialTicks);
      float f2 = avatarState.getInterpolatedBob(renderPartialTicks);
      result3f.rotate(new Quaternionf().rotationX(Math.abs(Mth.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F * (float) (Math.PI / 180.0)));
      result3f.rotate(new Quaternionf().rotationZ(Mth.sin(f1 * (float) Math.PI) * f2 * 3.0F * (float) (Math.PI / 180.0)));
      result3f.add(new Vector3f(Mth.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(Mth.cos(f1 * (float) Math.PI) * f2), 0.0F));
   }

   private static Vector2f calculateScreenPosition(Vector3f result3f, double fov) {
      if (result3f.z() >= 0.0F) {
         return new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
      }

      float halfWidth = (float)mc.getWindow().getGuiScaledWidth() / 2.0F;
      float halfHeight = (float)mc.getWindow().getGuiScaledHeight() / 2.0F;
      float scaleFactor = halfHeight / (-result3f.z() * (float)Math.tan(Math.toRadians(fov / 2.0)));
      return new Vector2f(halfWidth + result3f.x() * scaleFactor, halfHeight - result3f.y() * scaleFactor);
   }
}
