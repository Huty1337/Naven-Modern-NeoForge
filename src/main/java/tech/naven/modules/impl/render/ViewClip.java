package tech.naven.modules.impl.render;

import tech.naven.events.api.EventTarget;
import tech.naven.events.impl.EventRender2D;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.utils.SmoothAnimationTimer;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;
import net.minecraft.client.CameraType;

@ModuleInfo(
   name = "ViewClip",
   description = "Allows you to see through blocks",
   category = Category.RENDER
)
public class ViewClip extends Module {
   public FloatValue scale = ValueBuilder.create(this, "Scale")
      .setMinFloatValue(0.5F)
      .setMaxFloatValue(2.0F)
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.01F)
      .build()
      .getFloatValue();
   public BooleanValue animation = ValueBuilder.create(this, "Animation").setDefaultBooleanValue(true).build().getBooleanValue();
   public FloatValue animationSpeed = ValueBuilder.create(this, "Animation Speed")
      .setMinFloatValue(0.01F)
      .setMaxFloatValue(0.5F)
      .setDefaultFloatValue(0.3F)
      .setFloatStep(0.01F)
      .setVisibility(() -> this.animation.getCurrentValue())
      .build()
      .getFloatValue();
   public SmoothAnimationTimer personViewAnimation = new SmoothAnimationTimer(100.0F);
   CameraType lastPersonView;

   @EventTarget
   public void onRender(EventRender2D e) {
      if (this.lastPersonView != mc.options.getCameraType()) {
         this.lastPersonView = mc.options.getCameraType();
         if (this.lastPersonView == CameraType.FIRST_PERSON || this.lastPersonView == CameraType.THIRD_PERSON_BACK) {
            this.personViewAnimation.value = 0.0F;
         }
      }

      this.personViewAnimation.speed = this.animationSpeed.getCurrentValue();
      this.personViewAnimation.update(true);
   }
}
