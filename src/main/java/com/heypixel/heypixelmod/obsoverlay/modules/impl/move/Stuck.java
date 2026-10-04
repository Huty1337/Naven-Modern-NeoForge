package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.NetworkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(
   name = "Stuck",
   description = "Stuck in air!",
   category = Category.MOVEMENT
)
public class Stuck extends Module {
   int stage = 0;
   Packet<?> packet;
   float lastYaw;
   float lastPitch;
   boolean tryDisable = false;
   Queue<ServerboundPongPacket> packets = new ConcurrentLinkedQueue<>();

   @Override
   public void onEnable() {
      this.stage = 0;
      this.packet = null;
      this.lastYaw = RotationManager.rotations.x;
      this.lastPitch = RotationManager.rotations.y;
      this.tryDisable = false;
   }

   @Override
   public void setEnabled(boolean enabled) {
      if (mc.player != null) {
         if (enabled) {
            super.setEnabled(true);
         } else if (this.stage == 3) {
            super.setEnabled(false);
         } else {
            this.tryDisable = true;
         }
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      Module scaffold = Naven.getInstance().getModuleManager().getModule(Scaffold.class);
      if (scaffold.isEnabled()) {
         scaffold.toggle();
      } else {
         if (e.getType() == EventType.PRE) {
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
            if (this.stage == 1) {
               this.stage = 2;
               float rotationYaw = mc.player.getYRot();
               float rotationPitch = mc.player.getXRot();
               if (this.shouldRotate() && (this.lastYaw != rotationYaw || this.lastPitch != rotationPitch)) {
                  NetworkUtils.sendPacketNoEvent(new Rot(rotationYaw, rotationPitch, mc.player.onGround(), mc.player.horizontalCollision));

                  while (!this.packets.isEmpty()) {
                     NetworkUtils.sendPacketNoEvent((Packet<?>)this.packets.poll());
                  }

                  this.lastYaw = rotationYaw;
                  this.lastPitch = rotationPitch;
               }

               NetworkUtils.sendPacketNoEvent(this.packet);
            }

            if (this.tryDisable) {
               NetworkUtils.sendPacketNoEvent(new Pos(mc.player.getX() + 1337.0, mc.player.getY(), mc.player.getZ() + 1337.0, mc.player.onGround(), mc.player.horizontalCollision));

               while (!this.packets.isEmpty()) {
                  NetworkUtils.sendPacketNoEvent((Packet<?>)this.packets.poll());
               }

               this.tryDisable = false;
            }
         }
      }
   }

   private static boolean isBowlFood(ItemStack stack) {
      return stack.is(Items.MUSHROOM_STEW) || stack.is(Items.RABBIT_STEW) || stack.is(Items.BEETROOT_SOUP) || stack.is(Items.SUSPICIOUS_STEW);
   }

   private boolean shouldRotate() {
      if (this.packet instanceof ServerboundUseItemPacket blockPlacement) {
         ItemStack item = mc.player.getItemInHand(blockPlacement.getHand());
         return !isBowlFood(item) && !(item.getItem() instanceof BowItem);
      } else {
         return this.packet instanceof ServerboundPlayerActionPacket playerDigging
            ? playerDigging.getAction() == Action.RELEASE_USE_ITEM && mc.player.getUseItem().getItem() instanceof BowItem
            : false;
      }
   }

   @EventTarget
   public void onMoveInput(EventMoveInput e) {
      e.setForward(0.0F);
      e.setStrafe(0.0F);
      e.setJump(false);
      e.setSneak(false);
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      this.stage = 3;
      this.packet = null;
      this.toggle();
   }

   @EventTarget(1)
   public void onPacket(EventPacket e) {
      if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
         e.setCancelled(true);
      } else if (e.getPacket() instanceof ServerboundPongPacket) {
         this.packets.offer((ServerboundPongPacket)e.getPacket());
         e.setCancelled(true);
      } else if (e.getPacket() instanceof ServerboundUseItemPacket || e.getPacket() instanceof ServerboundPlayerActionPacket) {
         this.packet = e.getPacket();
         this.stage = 1;
         e.setCancelled(true);
      } else if (e.getPacket() instanceof ClientboundPlayerPositionPacket) {
         while (!this.packets.isEmpty()) {
            NetworkUtils.sendPacketNoEvent((Packet<?>)this.packets.poll());
         }

         this.stage = 3;
         this.toggle();
      }
   }
}
