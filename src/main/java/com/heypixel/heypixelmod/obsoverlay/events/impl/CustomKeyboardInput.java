package com.heypixel.heypixelmod.obsoverlay.events.impl;

import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public class CustomKeyboardInput extends ClientInput {
   private final Options options;
   private boolean cancel;

   public CustomKeyboardInput(Options options) {
      this.options = options;
      this.cancel = false;
   }

   @Override
   public void tick() {
      this.keyPresses = new Input(
         this.options.keyUp.isDown(),
         this.options.keyDown.isDown(),
         this.options.keyLeft.isDown(),
         this.options.keyRight.isDown(),
         this.options.keyJump.isDown(),
         this.options.keyShift.isDown(),
         this.options.keySprint.isDown()
      );
      float forward = calculateImpulse(this.keyPresses.forward(), this.keyPresses.backward());
      float strafe = calculateImpulse(this.keyPresses.left(), this.keyPresses.right());
      if (this.cancel) {
         forward = 0.0F;
         strafe = 0.0F;
      }

      this.moveVector = new Vec2(strafe, forward).normalized();
   }

   public void setCancel(boolean cancel) {
      this.cancel = cancel;
   }

   private static float calculateImpulse(boolean input, boolean otherInput) {
      if (input == otherInput) {
         return 0.0F;
      } else {
         return input ? 1.0F : -1.0F;
      }
   }
}
