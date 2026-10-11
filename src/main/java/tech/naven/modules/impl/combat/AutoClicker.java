package tech.naven.modules.impl.combat;

import tech.mixin.accessors.MinecraftAccessor;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventMotion;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.HitResult.Type;

@ModuleInfo(
   name = "AutoClicker",
   description = "Automatically clicks for you",
   category = Category.COMBAT
)
public class AutoClicker extends Module {
   private final FloatValue cps = ValueBuilder.create(this, "CPS")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(5.0F)
      .setMaxFloatValue(20.0F)
      .build()
      .getFloatValue();
   private final BooleanValue itemCheck = ValueBuilder.create(this, "Item Check").setDefaultBooleanValue(true).build().getBooleanValue();
   private float counter = 0.0F;

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         MinecraftAccessor accessor = (MinecraftAccessor)mc;
         Item item = mc.player.getMainHandItem().getItem();
         if (mc.options.keyAttack.isDown()
            && (mc.player.getMainHandItem().is(ItemTags.SWORDS) || item instanceof AxeItem || !this.itemCheck.getCurrentValue())
            && mc.hitResult.getType() != Type.BLOCK) {
            this.counter = this.counter + this.cps.getCurrentValue() / 20.0F;
            if (this.counter >= 1.0F / this.cps.getCurrentValue()) {
               accessor.setMissTime(0);
               KeyMapping.click(mc.options.keyAttack.getKey());
               this.counter--;
            }
         } else {
            this.counter = 0.0F;
         }
      }
   }
}
