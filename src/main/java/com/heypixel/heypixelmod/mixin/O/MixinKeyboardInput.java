package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends ClientInput {
   @Inject(
      at = {@At("TAIL")},
      method = {"tick"}
   )
   private void onTickTail(CallbackInfo ci) {
      Input keyPresses = this.keyPresses;
      float forward = keyPresses.forward() == keyPresses.backward() ? 0.0F : (keyPresses.forward() ? 1.0F : -1.0F);
      float strafe = keyPresses.left() == keyPresses.right() ? 0.0F : (keyPresses.left() ? 1.0F : -1.0F);
      EventMoveInput eventMoveInput = new EventMoveInput(forward, strafe, keyPresses.jump(), keyPresses.shift(), 0.3);
      Naven.getInstance().getEventManager().call(eventMoveInput);
      this.keyPresses = new Input(
         keyPresses.forward(),
         keyPresses.backward(),
         keyPresses.left(),
         keyPresses.right(),
         eventMoveInput.isJump(),
         eventMoveInput.isSneak(),
         keyPresses.sprint()
      );
      this.moveVector = new Vec2(eventMoveInput.getStrafe(), eventMoveInput.getForward()).normalized();
   }
}
