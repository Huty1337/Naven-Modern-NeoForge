package tech.naven;

import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.EspRenderTypes;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Render3DUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "naven", dist = Dist.CLIENT)
public class NavenModLoader {
   public NavenModLoader(IEventBus modEventBus, Dist dist) {
      modEventBus.addListener(NavenModLoader::onConfigureMainRenderTarget);
      modEventBus.addListener(NavenModLoader::onRegisterRenderPipelines);
      NeoForge.EVENT_BUS.register(new Render3DUtils.LevelProjectionListener());
   }

   private static void onConfigureMainRenderTarget(ConfigureMainRenderTargetEvent event) {
      event.enableStencil();
   }

   private static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(EspRenderTypes.FILLED_BOX_PIPELINE);
      event.registerPipeline(EspRenderTypes.OUTLINED_BOX_PIPELINE);
      event.registerPipeline(EspRenderTypes.LINE_STRIP_PIPELINE);
   }
}
