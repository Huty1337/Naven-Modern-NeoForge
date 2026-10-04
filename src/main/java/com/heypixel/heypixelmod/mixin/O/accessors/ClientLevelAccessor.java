package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientLevel.class})
public interface ClientLevelAccessor {
   @Accessor
   BlockStatePredictionHandler getBlockStatePredictionHandler();
}
