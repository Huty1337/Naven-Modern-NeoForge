package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({LocalPlayer.class})
public interface LocalPlayerAccessor {
   @Accessor
   boolean isWasSprinting();

   @Accessor("yRotLast")
   float getYRotLast();

   @Accessor("xRotLast")
   float getXRotLast();

   @Accessor("xRotLast")
   void setXRotLast(float var1);

   @Accessor("yRotLast")
   void setYRotLast(float var1);
}
