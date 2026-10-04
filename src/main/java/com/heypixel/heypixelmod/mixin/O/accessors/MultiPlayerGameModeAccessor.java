package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MultiPlayerGameMode.class})
public interface MultiPlayerGameModeAccessor {
   @Invoker("ensureHasSentCarriedItem")
   void invokeEnsureHasSentCarriedItem();

   @Accessor("localPlayerMode")
   GameType getLocalPlayerMode();
}
