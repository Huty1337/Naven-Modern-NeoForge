package tech.naven.events.impl;

import tech.naven.events.api.events.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class EventDestroyBlock implements Event {
   private final BlockPos pos;
   private final Direction face;

   public EventDestroyBlock(BlockPos pos, Direction face) {
      this.pos = pos;
      this.face = face;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public Direction getFace() {
      return this.face;
   }
}
