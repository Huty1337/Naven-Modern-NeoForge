package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

@ModuleInfo(
   name = "AutoMLG",
   description = "Automatically places water when falling",
   category = Category.MOVEMENT
)
public class AutoMLG extends Module {
   FloatValue distance = ValueBuilder.create(this, "Fall Distance")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(15.0F)
      .build()
      .getFloatValue();
   public boolean rotation = false;
   private BlockPos above;
   private boolean placeWater = false;
   private int originalSlot;
   private int timeout;

   public static boolean isOnGround(double height) {
      Iterable<VoxelShape> collisions = mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, height, 0.0));
      return collisions.iterator().hasNext();
   }

   @EventTarget
   public void onPre(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         if (mc.player.fallDistance > this.distance.getCurrentValue()) {
            if (this.rotation && isOnGround(mc.player.getDeltaMovement().y)) {
               this.placeWater = true;
            } else if (isOnGround(mc.player.getDeltaMovement().y * 2.0)) {
               for (int i = 0; i < 9; i++) {
                  ItemStack item = mc.player.getInventory().getItem(i);
                  if (!item.isEmpty() && item.getItem() == Items.WATER_BUCKET) {
                     this.originalSlot = mc.player.getInventory().getSelectedSlot();
                     mc.player.getInventory().setSelectedSlot(i);
                     this.rotation = true;
                     this.timeout = 5;
                  }
               }
            }
         }

         if (--this.timeout == 0 && this.rotation) {
            this.rotation = false;
            Notification notification = new Notification(NotificationLevel.WARNING, "Failed to place water!", 3000L);
            Naven.getInstance().getNotificationManager().addNotification(notification);
         }
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (this.placeWater) {
         this.placeWater = false;
         if (mc.hitResult.getType() == Type.BLOCK && ((BlockHitResult)mc.hitResult).getDirection() == Direction.UP) {
            this.above = ((BlockHitResult)mc.hitResult).getBlockPos().above();
            this.useItem(mc.player, mc.level, InteractionHand.MAIN_HAND);
         } else {
            Notification notification = new Notification(NotificationLevel.WARNING, "Failed to place water!", 3000L);
            Naven.getInstance().getNotificationManager().addNotification(notification);
            this.rotation = false;
         }
      } else if (this.above != null) {
         this.rotation = false;
         BlockPos above = ((BlockHitResult)mc.hitResult).getBlockPos().above();
         if (above.equals(this.above)) {
            this.useItem(mc.player, mc.level, InteractionHand.MAIN_HAND);
         } else {
            Notification notification = new Notification(NotificationLevel.WARNING, "Failed to recycle the water dues to moving!", 3000L);
            Naven.getInstance().getNotificationManager().addNotification(notification);
         }

         mc.player.getInventory().setSelectedSlot(this.originalSlot);
         this.above = null;
      }
   }

   public InteractionResult useItem(Player pPlayer, Level pLevel, InteractionHand pHand) {
      return mc.gameMode.useItem(pPlayer, pHand);
   }
}
