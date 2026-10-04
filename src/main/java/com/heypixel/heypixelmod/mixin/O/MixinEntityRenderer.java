package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NameProtect;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NameTags;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"},
      at = {@At("RETURN")}
   )
   private void naven$hideOrProtectNameTag(T pEntity, S pState, float pPartialTick, CallbackInfo ci) {
      if (pEntity instanceof Player && Naven.getInstance().getModuleManager().getModule(NameTags.class).isEnabled()) {
         pState.nameTag = null;
         return;
      }

      pState.nameTag = NameProtect.hookNameTag(pEntity, pState.nameTag);
   }
}
