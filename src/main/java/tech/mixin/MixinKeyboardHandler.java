package tech.mixin;

import tech.naven.Naven;
import tech.naven.events.impl.EventKey;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardHandler.class})
public class MixinKeyboardHandler {
   @Inject(
      at = {@At("HEAD")},
      method = {"keyPress"}
   )
   private void onKeyPress(long pWindowPointer, int pAction, KeyEvent pEvent, CallbackInfo ci) {
      int key = pEvent.key();
      if (key != -1 && Naven.getInstance() != null && Naven.getInstance().getEventManager() != null) {
         Naven.getInstance().getEventManager().call(new EventKey(key, pAction != 0));
      }
   }
}
