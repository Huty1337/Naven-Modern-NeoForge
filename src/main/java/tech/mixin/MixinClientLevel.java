package tech.mixin;

import tech.naven.Naven;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ClientLevel.class})
public class MixinClientLevel {
   @Redirect(
      method = {"tickNonPassenger"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;tick()V"
      )
   )
   public void hookSkipTicks(Entity instance) {
      if (Naven.skipTicks > 0 && instance == Minecraft.getInstance().player) {
         Naven.skipTicks--;
         return;
      }

      if (!Naven.skipTasks.isEmpty() && instance == Minecraft.getInstance().player) {
         Runnable task = Naven.skipTasks.poll();
         if (task != null) {
            task.run();
         }
      } else {
         instance.tick();
      }
   }
}
