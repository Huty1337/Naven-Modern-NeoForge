package tech.mixin;

import tech.naven.Naven;
import tech.naven.events.impl.EventUpdateFoV;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public abstract class MixinAbstractClientPlayer {
   @Inject(
      method = {"getFieldOfViewModifier"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void hookFoV(CallbackInfoReturnable<Float> cir) {
      Float returnValue = (Float)cir.getReturnValue();
      EventUpdateFoV event = new EventUpdateFoV(returnValue);
      Naven.getInstance().getEventManager().call(event);
      cir.setReturnValue(event.getFov());
   }
}
