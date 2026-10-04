package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderAfterWorld;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.FullBright;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.MotionBlur;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NoHurtCam;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.PostProcess;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.GL;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public class MixinGameRenderer {
   private CachedOrthoProjectionMatrixBuffer navenGuiProjection;

   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private GuiRenderState guiRenderState;
   @Shadow
   @Final
   private Camera mainCamera;

   @Inject(
      method = {"renderLevel"},
      at = {@At("TAIL")}
   )
   private void onRenderWorldTail(DeltaTracker deltaTracker, CallbackInfo ci) {
      PoseStack stack = new PoseStack();
      Quaternionf rotation = this.mainCamera.rotation().conjugate(new Quaternionf());
      stack.mulPose(new Matrix4f().rotation(rotation));
      Naven.getInstance().getEventManager().call(new EventRender(deltaTracker.getGameTimeDeltaPartialTick(true), stack));
      Naven.getInstance().getEventManager().call(new EventRenderAfterWorld());
   }

   @Inject(
      method = {"getNightVisionScale"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void getNightVisionScale(LivingEntity pLivingEntity, float pNanoTime, CallbackInfoReturnable<Float> cir) {
      FullBright module = (FullBright)Naven.getInstance().getModuleManager().getModule(FullBright.class);
      if (module.isEnabled()) {
         cir.setReturnValue(module.brightness.getCurrentValue());
         cir.cancel();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"
      )}
   )
   public void injectRender2DEvent(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {

      GuiGraphics guiGraphics = new GuiGraphics(this.minecraft, this.guiRenderState);
      PoseStack stack = new PoseStack();
      Naven.getInstance().getEventManager().call(new EventRender2D(stack, guiGraphics));

      if (renderLevel) {
         PostProcess postProcess = (PostProcess)Naven.getInstance().getModuleManager().getModule(PostProcess.class);
         if (postProcess != null && postProcess.isEnabled()) {
            postProcess.applyHudPostProcess(stack);
         }
      }
   }
   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void navenMotionBlurTail(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
      MotionBlur module = (MotionBlur)Naven.getInstance().getModuleManager().getModule(MotionBlur.class);
      if (module != null) {
         module.applyMotionBlur(renderLevel);
      }
   }   @Inject(
      method = {"bobHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bobHurt(PoseStack pMatrixStack, float pPartialTicks, CallbackInfo ci) {
      NoHurtCam module = (NoHurtCam)Naven.getInstance().getModuleManager().getModule(NoHurtCam.class);
      if (module.isEnabled()) {
         ci.cancel();
      }
   }
}
