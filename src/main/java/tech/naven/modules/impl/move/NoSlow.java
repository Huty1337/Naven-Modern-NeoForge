package tech.naven.modules.impl.move;

import tech.naven.events.api.EventTarget;
import tech.naven.events.impl.EventSlowdown;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;

@ModuleInfo(
   name = "NoSlow",
   description = "NoSlowDown",
   category = Category.MOVEMENT
)
public class NoSlow extends Module {
   @EventTarget
   public void onSlow(EventSlowdown eventSlowdown) {
      if (mc.player.getUseItemRemainingTicks() % 2 != 0 && mc.player.getUseItemRemainingTicks() <= 30) {
         eventSlowdown.setSlowdown(false);
         mc.player.setSprinting(true);
      }
   }
}
