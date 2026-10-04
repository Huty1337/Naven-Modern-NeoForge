package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.callables.EventCancellable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EventStuckInBlock extends EventCancellable {
   private BlockState state;
   private Vec3 stuckSpeedMultiplier;

   public BlockState getState() {
      return this.state;
   }

   public Vec3 getStuckSpeedMultiplier() {
      return this.stuckSpeedMultiplier;
   }

   public void setState(BlockState state) {
      this.state = state;
   }

   public void setStuckSpeedMultiplier(Vec3 stuckSpeedMultiplier) {
      this.stuckSpeedMultiplier = stuckSpeedMultiplier;
   }

   @Override
   public String toString() {
      return "EventStuckInBlock(state=" + this.getState() + ", stuckSpeedMultiplier=" + this.getStuckSpeedMultiplier() + ")";
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventStuckInBlock other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         Object this$state = this.getState();
         Object other$state = other.getState();
         if (this$state == null ? other$state == null : this$state.equals(other$state)) {
            Object this$stuckSpeedMultiplier = this.getStuckSpeedMultiplier();
            Object other$stuckSpeedMultiplier = other.getStuckSpeedMultiplier();
            return this$stuckSpeedMultiplier == null ? other$stuckSpeedMultiplier == null : this$stuckSpeedMultiplier.equals(other$stuckSpeedMultiplier);
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventStuckInBlock;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $state = this.getState();
      result = result * 59 + ($state == null ? 43 : $state.hashCode());
      Object $stuckSpeedMultiplier = this.getStuckSpeedMultiplier();
      return result * 59 + ($stuckSpeedMultiplier == null ? 43 : $stuckSpeedMultiplier.hashCode());
   }

   public EventStuckInBlock(BlockState state, Vec3 stuckSpeedMultiplier) {
      this.state = state;
      this.stuckSpeedMultiplier = stuckSpeedMultiplier;
   }
}
