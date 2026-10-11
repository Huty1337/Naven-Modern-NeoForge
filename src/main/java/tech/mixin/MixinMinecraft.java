package tech.mixin;

import tech.naven.Naven;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventClick;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.events.impl.EventShutdown;
import tech.naven.modules.impl.render.Glow;
import tech.naven.utils.AnimationUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public class MixinMinecraft {
   @Unique
   private long naven_Modern$lastFrame;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void onInit(CallbackInfo info) {
      Naven.modRegister();
   }

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void onInit(GameConfig pGameConfig, CallbackInfo ci) {
      System.setProperty("java.awt.headless", "false");
      try {
         ModList.get().getMods().removeIf(modInfox -> modInfox.getModId().contains("naven"));
         List<IModFileInfo> fileInfoToRemove = new ArrayList<>();

         for (IModFileInfo fileInfo : ModList.get().getModFiles()) {
            for (IModInfo modInfo : fileInfo.getMods()) {
               if (modInfo.getModId().contains("naven")) {
                  fileInfoToRemove.add(fileInfo);
               }
            }
         }

         ModList.get().getModFiles().removeAll(fileInfoToRemove);
      } catch (UnsupportedOperationException ignored) {
      }
   }

   @Inject(
      method = {"close"},
      at = {@At("HEAD")},
      remap = false
   )
   private void shutdown(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventShutdown());
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void tickPre(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.PRE));
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void tickPost(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventRunTicks(EventType.POST));
      }
   }

   @Inject(
      method = {"shouldEntityAppearGlowing"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
      if (Glow.shouldGlow(pEntity)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"runTick"},
      at = {@At("HEAD")}
   )
   private void runTick(CallbackInfo ci) {
      long currentTime = System.nanoTime() / 1000000L;
      int deltaTime = (int)(currentTime - this.naven_Modern$lastFrame);
      this.naven_Modern$lastFrame = currentTime;
      AnimationUtils.delta = deltaTime;
   }

   @Inject(
      method = {"handleKeybinds"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
         ordinal = 0,
         shift = Shift.BEFORE
      )},
      cancellable = true
   )
   private void clickEvent(CallbackInfo ci) {
      if (Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         EventClick event = new EventClick();
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
