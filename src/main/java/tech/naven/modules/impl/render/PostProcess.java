package tech.naven.modules.impl.render;

import tech.naven.Naven;
import tech.naven.events.api.types.EventType;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.ModuleManager;
import tech.naven.modules.impl.combat.Aura;
import tech.naven.utils.renderer.BlurUtils;
import tech.naven.utils.renderer.GlowUtils;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;
import com.mojang.blaze3d.vertex.PoseStack;

@ModuleInfo(
   name = "PostProcess",
   description = "Post process effects",
   category = Category.RENDER
)
public class PostProcess extends Module {
   private static final boolean GLOW_ENABLED = false;
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
