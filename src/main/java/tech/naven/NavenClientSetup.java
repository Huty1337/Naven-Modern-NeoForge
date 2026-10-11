package tech.naven;

import tech.naven.modules.impl.render.EspRenderTypes;
import tech.naven.modules.impl.render.Render3DUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;

public final class NavenClientSetup {
   private NavenClientSetup() {
   }

   public static void init(IEventBus modEventBus) {
      modEventBus.addListener(NavenClientSetup::onConfigureMainRenderTarget);
      modEventBus.addListener(EspRenderTypes::registerPipelines);
      Render3DUtils.register();
   }

   private static void onConfigureMainRenderTarget(ConfigureMainRenderTargetEvent event) {
      event.enableStencil();
   }
}
