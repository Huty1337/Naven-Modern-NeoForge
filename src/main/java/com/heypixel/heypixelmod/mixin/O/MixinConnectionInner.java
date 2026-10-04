package com.heypixel.heypixelmod.mixin.O;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(
   targets = {"net.minecraft.network.Connection$1"}
)
public class MixinConnectionInner {
}
