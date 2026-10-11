package tech.naven.modules.impl.move;

import tech.mixin.accessors.LivingEntityAccessor;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventMotion;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;

@ModuleInfo(
   name = "NoJumpDelay",
   description = "Removes the delay when jumping",
   category = Category.MOVEMENT
)
public class NoJumpDelay extends Module {
   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         ((LivingEntityAccessor)mc.player).setNoJumpDelay(0);
      }
   }
}
