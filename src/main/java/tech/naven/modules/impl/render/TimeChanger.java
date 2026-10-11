package tech.naven.modules.impl.render;

import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventMotion;
import tech.naven.events.impl.EventPacket;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

@ModuleInfo(
   name = "TimeChanger",
   description = "Change the time of the world",
   category = Category.RENDER
)
public class TimeChanger extends Module {
   FloatValue time = ValueBuilder.create(this, "World Time")
      .setDefaultFloatValue(8000.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(24000.0F)
      .build()
      .getFloatValue();

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         mc.level.setTimeFromServer(mc.level.getGameTime(), (long)this.time.getCurrentValue(), false);
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getPacket() instanceof ClientboundSetTimePacket) {
         event.setCancelled(true);
      }
   }
}
