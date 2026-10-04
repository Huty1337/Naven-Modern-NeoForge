package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Minecraft.class})
public interface MinecraftAccessor {
   @Accessor
   void setRightClickDelay(int var1);

   @Accessor
   void setMissTime(int var1);
}
