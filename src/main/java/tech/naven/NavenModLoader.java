package tech.naven;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = "naven", dist = Dist.CLIENT)
public class NavenModLoader {
   public NavenModLoader(IEventBus modEventBus, Dist dist) {
      NavenClientSetup.init(modEventBus);
   }
}
