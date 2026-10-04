package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class EventUpdateHeldItem implements Event {
   private final InteractionHand hand;
   private ItemStack item;

   public EventUpdateHeldItem(InteractionHand hand, ItemStack item) {
      this.hand = hand;
      this.item = item;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public void setItem(ItemStack item) {
      this.item = item;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EventUpdateHeldItem other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else {
         Object this$hand = this.getHand();
         Object other$hand = other.getHand();
         if (this$hand == null ? other$hand == null : this$hand.equals(other$hand)) {
            Object this$item = this.getItem();
            Object other$item = other.getItem();
            return this$item == null ? other$item == null : this$item.equals(other$item);
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EventUpdateHeldItem;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      Object $hand = this.getHand();
      result = result * 59 + ($hand == null ? 43 : $hand.hashCode());
      Object $item = this.getItem();
      return result * 59 + ($item == null ? 43 : $item.hashCode());
   }

   @Override
   public String toString() {
      return "EventUpdateHeldItem(hand=" + this.getHand() + ", item=" + this.getItem() + ")";
   }
}
