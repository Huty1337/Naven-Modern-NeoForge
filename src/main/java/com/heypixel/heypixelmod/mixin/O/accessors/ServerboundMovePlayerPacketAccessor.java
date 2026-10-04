package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ServerboundMovePlayerPacket.class})
public interface ServerboundMovePlayerPacketAccessor {
   @Accessor
   void setYRot(float var1);
}
