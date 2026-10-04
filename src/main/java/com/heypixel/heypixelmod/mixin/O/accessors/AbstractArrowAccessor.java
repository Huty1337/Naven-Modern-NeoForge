package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({AbstractArrow.class})
public interface AbstractArrowAccessor {
   @Invoker("isInGround")
   boolean callIsInGround();
}
