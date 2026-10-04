package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.utils.MoveUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.concurrent.LinkedBlockingQueue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.protocol.Packet;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(
   name = "LongJump",
   category = Category.MOVEMENT,
   description = "Allows you to use fireball longjump"
)
public class LongJump extends Module {
   public static Rotation rotation = null;
   private boolean notMoving = false;
   private boolean enabled = false;
   private int rotateTick = 0;
   private int lastSlot = -1;
   private boolean delayed = false;
   private boolean shouldDisableAndRelease = false;
   private boolean isUsingItem = false;
   private boolean mouse4Pressed = false;
   private boolean mouse5Pressed = false;
   private long delayStartTime = 0L;
   private int usedFireballCount = 0;
   private int receivedKnockbacks = 0;
   private int initialFireballCount = 0;
   private int releasedKnockbacks = 0;
   private final List<Integer> knockbackPositions = new ArrayList<>();
   private final LinkedBlockingQueue<Packet<?>> packets = new LinkedBlockingQueue<>();

   private void releaseAll() {
      while (!this.packets.isEmpty()) {
         try {
            Packet<?> packet = this.packets.poll();
            if (packet != null && mc.getConnection() != null) {
               ((Packet)packet).handle(mc.getConnection());
            }
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
   }

   private void releaseToKnockback(int knockbackIndex) {
      if (knockbackIndex < this.knockbackPositions.size()) {
         int targetPosition = this.knockbackPositions.get(knockbackIndex);
         int releasedCount = 0;

         while (!this.packets.isEmpty() && releasedCount <= targetPosition) {
            try {
               Packet<?> packet = this.packets.poll();
               if (packet != null && mc.getConnection() != null) {
                  ((Packet)packet).handle(mc.getConnection());
               }

               releasedCount++;
            } catch (Exception var6) {
               var6.printStackTrace();
            }
         }

         for (int i = knockbackIndex + 1; i < this.knockbackPositions.size(); i++) {
            this.knockbackPositions.set(i, this.knockbackPositions.get(i) - (targetPosition + 1));
         }
      }
   }

   private int getFireballSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.getItem() == Items.FIRE_CHARGE) {
            return i;
         }
      }

      return -1;
   }

   private int getFireballCount() {
      int count = 0;

      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (itemStack.getItem() == Items.FIRE_CHARGE) {
            count += itemStack.getCount();
         }
      }

      return count;
   }

   private int setupFireballSlot() {
      int fireballSlot = this.getFireballSlot();
      if (fireballSlot == -1) {
         ChatUtils.addChatMessage("§cNo FireBall!");
         this.setEnabled(false);
      }

      return fireballSlot;
   }

   @Override
   public void onEnable() {
      this.releaseAll();
      this.rotateTick = 0;
      this.enabled = true;
      this.lastSlot = -1;
      this.notMoving = false;
      this.delayed = false;
      this.isUsingItem = false;
      rotation = null;
      this.shouldDisableAndRelease = false;
      this.mouse4Pressed = false;
      this.mouse5Pressed = false;
      this.delayStartTime = 0L;
      this.usedFireballCount = 0;
      this.receivedKnockbacks = 0;
      this.initialFireballCount = 0;
      this.releasedKnockbacks = 0;
      this.knockbackPositions.clear();
      ChatUtils.addChatMessage("§aLongJump enabled! Press Mouse4 to jump & use fireball, Mouse5 to release each knockback");
   }

   @Override
   public void onDisable() {
      this.releaseAll();
      if (this.lastSlot != -1 && mc.player != null) {
         mc.player.getInventory().setSelectedSlot(this.lastSlot);
      }

      mc.options.keyUse.setDown(false);
      mc.options.keyJump.setDown(false);
      rotation = null;
      this.isUsingItem = false;
      this.shouldDisableAndRelease = false;
      this.mouse4Pressed = false;
      this.mouse5Pressed = false;
      this.delayStartTime = 0L;
      this.usedFireballCount = 0;
      this.receivedKnockbacks = 0;
      this.initialFireballCount = 0;
      this.releasedKnockbacks = 0;
      this.knockbackPositions.clear();
      super.onDisable();
   }

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (this.isEnabled()) {
         if (this.shouldDisableAndRelease) {
            this.setEnabled(false);
         } else {
            if (this.enabled) {
               if (!MoveUtils.isMoving()) {
                  this.notMoving = true;
               }

               this.enabled = false;
            }

            boolean currentMouse4 = GLFW.glfwGetMouseButton(mc.getWindow().handle(), 3) == 1;
            if (currentMouse4 && !this.mouse4Pressed) {
               this.mouse4Pressed = true;
               if (!this.isUsingItem && this.rotateTick == 0) {
                  int fireballSlot = this.setupFireballSlot();
                  if (fireballSlot != -1) {
                     this.lastSlot = mc.player.getInventory().getSelectedSlot();
                     mc.player.getInventory().setSelectedSlot(fireballSlot);
                     this.rotateTick = 1;
                     ChatUtils.addChatMessage("§eStarting fireball usage #" + (this.usedFireballCount + 1));
                  }
               }
            } else if (!currentMouse4) {
               this.mouse4Pressed = false;
            }

            boolean currentMouse5 = GLFW.glfwGetMouseButton(mc.getWindow().handle(), 4) == 1;
            if (currentMouse5 && !this.mouse5Pressed) {
               this.mouse5Pressed = true;
               if (this.delayed && this.releasedKnockbacks < this.receivedKnockbacks) {
                  ChatUtils.addChatMessage("§aReleasing " + (this.releasedKnockbacks + 1) + "/" + this.receivedKnockbacks);
                  this.releaseToKnockback(this.releasedKnockbacks);
                  this.releasedKnockbacks++;
                  if (this.releasedKnockbacks >= this.receivedKnockbacks) {
                     ChatUtils.addChatMessage("§aAll released! Stopping LongJump.");
                     this.delayed = false;
                     this.setEnabled(false);
                  }
               } else if (!this.delayed) {
                  ChatUtils.addChatMessage("§cNo intercepted packets");
                  this.setEnabled(false);
               } else {
                  ChatUtils.addChatMessage("§cAll already released");
               }
            } else if (!currentMouse5) {
               this.mouse5Pressed = false;
            }
         }
      }
   }

   @EventTarget
   public void onRender2D(EventRender2D event) {
      if (this.isEnabled()) {
         int screenWidth = mc.getWindow().getGuiScaledWidth();
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         String statusText;
         if (this.delayed) {
            int packetCount = this.packets.size();
            long currentTime = System.currentTimeMillis();
            long delayDuration = currentTime - this.delayStartTime;
            statusText = String.format(
               "§eIntercepting: %d packets | Time: %.1fs | Press Mouse5 (%d/%d)",
               packetCount,
               (float)delayDuration / 1000.0F,
               this.releasedKnockbacks,
               this.receivedKnockbacks
            );
         } else if (this.isUsingItem) {
            statusText = "§aUsing fireball #" + this.usedFireballCount;
         } else {
            statusText = "§bWaiting for input | Mouse4: Jump & use fireball | Mouse5: Release";
         }

         float textX = (float)screenWidth / 2.0F - (float)mc.font.width(statusText) / 2.0F;
         float textY = (float)screenHeight / 2.0F + 20.0F;
         event.getGuiGraphics().drawString(mc.font, statusText, (int)textX, (int)textY, -1);
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (this.isEnabled() && mc.level != null) {
         if (this.delayed && event.getType() == EventType.RECEIVE) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof ClientboundPlayerPositionPacket) {
               this.shouldDisableAndRelease = true;
               event.setCancelled(true);
            } else {
               if (packet instanceof ClientboundSetEntityMotionPacket motionPacket && motionPacket.getId() == mc.player.getId()) {
                  this.receivedKnockbacks++;
                  this.knockbackPositions.add(this.packets.size());
                  mc.execute(() -> ChatUtils.addChatMessage("§e" + this.receivedKnockbacks + " received"));
               }

               event.setCancelled(true);
               this.packets.add(packet);
            }
         } else {
            if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet
               && event.getType() == EventType.RECEIVE
               && packet.getId() == mc.player.getId()
               && this.usedFireballCount > 0
               && !this.delayed) {
               this.receivedKnockbacks++;
               this.knockbackPositions.add(this.packets.size());
               mc.execute(() -> ChatUtils.addChatMessage("§eReceived #" + this.receivedKnockbacks + ", starting packet interception"));
               event.setCancelled(true);
               this.packets.add(event.getPacket());
               this.delayed = true;
               this.delayStartTime = System.currentTimeMillis();
               mc.execute(() -> ChatUtils.addChatMessage("§ePacket interception started, press Mouse5 to release each"));
            }
         }
      } else {
         if (this.delayed) {
            mc.execute(() -> {
               this.releaseAll();
               this.delayed = false;
            });
         }
      }
   }

   @EventTarget
   public void onMotion(EventMotion event) {
      if (this.isEnabled()) {
         if (event.getType() == EventType.PRE) {
            if (this.rotateTick > 0) {
               if (this.rotateTick == 1) {
                  this.usedFireballCount++;
                  ChatUtils.addChatMessage("§aJumping for fireball #" + this.usedFireballCount);
                  mc.options.keyJump.setDown(true);
                  float yaw;
                  float pitch;
                  if (!this.notMoving) {
                     yaw = mc.player.getYRot() - 180.0F;
                     pitch = 88.0F;
                  } else {
                     yaw = mc.player.getYRot();
                     pitch = 90.0F;
                  }

                  rotation = new Rotation(yaw, pitch);
               }

               if (this.rotateTick >= 2) {
                  this.rotateTick = 0;
                  int fireballSlot = this.setupFireballSlot();
                  if (fireballSlot != -1) {
                     mc.player.getInventory().setSelectedSlot(fireballSlot);
                     this.initialFireballCount = this.getFireballCount();
                     mc.options.keyUse.setDown(true);
                     this.isUsingItem = true;
                     ChatUtils.addChatMessage("§eFireball #" + this.usedFireballCount + " started, initial count: " + this.initialFireballCount);
                  } else {
                     this.setEnabled(false);
                  }
               }

               if (this.rotateTick != 0) {
                  this.rotateTick++;
               }
            }
         } else if (this.isUsingItem) {
            int currentFireballCount = this.getFireballCount();
            if (currentFireballCount < this.initialFireballCount) {
               mc.options.keyUse.setDown(false);
               mc.options.keyJump.setDown(false);
               rotation = null;
               this.isUsingItem = false;
               ChatUtils.addChatMessage(
                  "§eFireball #"
                     + this.usedFireballCount
                     + " used! Count: "
                     + this.initialFireballCount
                     + " -> "
                     + currentFireballCount
                     + ", waiting for next input"
               );
            } else if (this.getFireballSlot() == -1) {
               mc.options.keyUse.setDown(false);
               mc.options.keyJump.setDown(false);
               rotation = null;
               this.isUsingItem = false;
               ChatUtils.addChatMessage("§cNo more fireballs available!");
            }
         }
      }
   }
}
