package com.heypixel.heypixelmod.mixin.O.accessors;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientboundMoveEntityPacket.class})
public interface ClientboundMoveEntityPacketAccessor {
   @Accessor("entityId")
   int getEntityId();

   @Accessor("xa")
   short getXa();

   @Accessor("ya")
   short getYa();

   @Accessor("za")
   short getZa();

   @Accessor("yRot")
   byte getYRot();

   @Accessor("xRot")
   byte getXRot();

   @Accessor("onGround")
   boolean getOnGround();

   @Accessor("hasRot")
   boolean getHasRot();

   @Accessor("hasPos")
   boolean getHasPos();
}
