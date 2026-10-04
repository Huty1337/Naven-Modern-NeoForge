package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
   @Shadow
   private boolean wasSprinting;
   @Shadow
   @Final
   public ClientPacketListener connection;
   @Shadow
   private double xLast;
   @Shadow
   private double yLast;
   @Shadow
   private double zLast;
   @Shadow
   private float yRotLast;
   @Shadow
   private float xRotLast;
   @Shadow
   private int positionReminder;
   @Shadow
   private boolean lastOnGround;
   @Shadow
   private boolean autoJumpEnabled;
   @Shadow
   @Final
   protected Minecraft minecraft;

   @Shadow
   protected abstract boolean isControlledCamera();

   @Shadow
   protected abstract void sendIsSprintingIfNeeded();

   public MixinLocalPlayer(ClientLevel pClientLevel, GameProfile pGameProfile) {
      super(pClientLevel, pGameProfile);
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
         shift = Shift.BEFORE
      )}
   )
   public void injectUpdateEvent(CallbackInfo ci) {
      Naven.getInstance().getEventManager().call(new EventUpdate());
   }

   @Overwrite
   private void sendPosition() {
      EventMotion eventPre = new EventMotion(EventType.PRE, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround());
      Naven.getInstance().getEventManager().call(eventPre);
      if (eventPre.isCancelled()) {
         Naven.getInstance().getEventManager().call(new EventMotion(EventType.POST, eventPre.getYaw(), eventPre.getPitch()));
      } else {
         this.sendIsSprintingIfNeeded();
         if (this.isControlledCamera()) {
            double d4 = eventPre.getX() - this.xLast;
            double d0 = eventPre.getY() - this.yLast;
            double d1 = eventPre.getZ() - this.zLast;
            double d2 = (double)(eventPre.getYaw() - this.yRotLast);
            double d3 = (double)(eventPre.getPitch() - this.xRotLast);
            this.positionReminder++;
            boolean flag1 = Mth.lengthSquared(d4, d0, d1) > Mth.square(2.0E-4) || this.positionReminder >= 20;
            boolean flag2 = d2 != 0.0 || d3 != 0.0;
            if (this.isPassenger()) {
               Vec3 vec3 = this.getDeltaMovement();
               this.connection
                  .send(new PosRot(vec3.x, -999.0, vec3.z, eventPre.getYaw(), eventPre.getPitch(), eventPre.isOnGround(), this.horizontalCollision));
               flag1 = false;
            } else if (flag1 && flag2) {
               this.connection
                  .send(
                     new PosRot(
                        eventPre.getX(),
                        eventPre.getY(),
                        eventPre.getZ(),
                        eventPre.getYaw(),
                        eventPre.getPitch(),
                        eventPre.isOnGround(),
                        this.horizontalCollision
                     )
                  );
            } else if (flag1) {
               this.connection.send(new Pos(eventPre.getX(), eventPre.getY(), eventPre.getZ(), eventPre.isOnGround(), this.horizontalCollision));
            } else if (flag2) {
               this.connection.send(new Rot(eventPre.getYaw(), eventPre.getPitch(), eventPre.isOnGround(), this.horizontalCollision));
            } else if (this.lastOnGround != eventPre.isOnGround()) {
               this.connection.send(new StatusOnly(eventPre.isOnGround(), this.horizontalCollision));
            }

            if (flag1) {
               this.xLast = eventPre.getX();
               this.yLast = eventPre.getY();
               this.zLast = eventPre.getZ();
               this.positionReminder = 0;
            }

            if (flag2) {
               this.yRotLast = eventPre.getYaw();
               this.xRotLast = eventPre.getPitch();
            }

            this.lastOnGround = eventPre.isOnGround();
            this.autoJumpEnabled = (Boolean)this.minecraft.options.autoJump().get();
         }

         Naven.getInstance().getEventManager().call(new EventMotion(EventType.POST, eventPre.getYaw(), eventPre.getPitch()));
      }
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
         ordinal = 0
      )
   )
   public boolean onSlowdown(LocalPlayer localPlayer) {
      EventSlowdown event = new EventSlowdown(localPlayer.isUsingItem());
      Naven.getInstance().getEventManager().call(event);
      return event.isSlowdown();
   }
}
