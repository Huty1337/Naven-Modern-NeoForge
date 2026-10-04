package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.BlurUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.GlowUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;

@ModuleInfo(
   name = "PostProcess",
   description = "Post process effects",
   category = Category.RENDER
)
public class PostProcess extends Module {
   private static final boolean GLOW_ENABLED = false;
   private static final boolean DIAG_SUMMON = true;
   private static boolean diagSummoned;
   private final BooleanValue blur = ValueBuilder.create(this, "Blur").setDefaultBooleanValue(true).build().getBooleanValue();
   private final FloatValue blurFPS = ValueBuilder.create(this, "Blur FPS")
      .setVisibility(this.blur::getCurrentValue)
      .setFloatStep(1.0F)
      .setDefaultFloatValue(90.0F)
      .setMinFloatValue(15.0F)
      .setMaxFloatValue(120.0F)
      .build()
      .getFloatValue();
   private final FloatValue strength = ValueBuilder.create(this, "Blur Strength")
      .setVisibility(this.blur::getCurrentValue)
      .setDefaultFloatValue(2.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(19.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   private final BooleanValue bloom = ValueBuilder.create(this, "Bloom").setDefaultBooleanValue(true).build().getBooleanValue();
   private final FloatValue bloomFPS = ValueBuilder.create(this, "Bloom FPS")
      .setVisibility(this.bloom::getCurrentValue)
      .setFloatStep(1.0F)
      .setDefaultFloatValue(90.0F)
      .setMinFloatValue(15.0F)
      .setMaxFloatValue(120.0F)
      .build()
      .getFloatValue();

   public void applyHudPostProcess(PoseStack stack) {
      if (DIAG_SUMMON && !diagSummoned && Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
         diagSummoned = true;
         Minecraft.getInstance().player.connection.sendCommand("summon minecraft:zombie ~ ~ ~5");
      }

      if (!hasMaskConsumers()) {
         return;
      }

      if (this.blur.getCurrentValue()) {
         BlurUtils.onRenderAfterWorld(stack, this.blurFPS.getCurrentValue(), (int)this.strength.getCurrentValue());
      }

      if (this.bloom.getCurrentValue() && GLOW_ENABLED) {
         GlowUtils.onRenderAfterWorld(stack, this.bloomFPS.getCurrentValue(), 48.0F, 1.0F, 1.0F, EventType.GLOW);
      }
   }

   private static boolean hasMaskConsumers() {
      ModuleManager modules = Naven.getInstance().getModuleManager();
      return isEnabled(modules, HUD.class)
         || isEnabled(modules, ItemTags.class)
         || isEnabled(modules, NameTags.class)
         || isEnabled(modules, EffectDisplay.class)
         || isEnabled(modules, Aura.class);
   }

   private static boolean isEnabled(ModuleManager modules, Class<? extends Module> type) {
      Module module = modules.getModule(type);
      return module != null && module.isEnabled();
   }
}
