package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventClick;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import java.util.Optional;
import java.util.stream.StreamSupport;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

@ModuleInfo(
   name = "CrystalAura",
   category = Category.COMBAT,
   description = "Automatically attacks end crystals"
)
public class AttackCrystal extends Module {
   public static Vector2f rotations;
   private Entity entity;
   BooleanValue packet = ValueBuilder.create(this, "Attack on Packet (Danger)").setDefaultBooleanValue(false).build().getBooleanValue();

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof ClientboundAddEntityPacket && this.packet.getCurrentValue()) {
         ClientboundAddEntityPacket packet = (ClientboundAddEntityPacket)e.getPacket();
         if (packet.getType() == EntityType.END_CRYSTAL) {
            EndCrystal pTarget = new EndCrystal(mc.level, packet.getX(), packet.getY(), packet.getZ());
            pTarget.setId(packet.getId());
            if (mc.player.distanceTo(pTarget) <= 4.0F) {
               Vector2f rotations = RotationUtils.getRotations(pTarget);
               mc.getConnection()
                  .send(new PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotations.getX(), rotations.getY(), mc.player.onGround(), mc.player.horizontalCollision));
               PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
               float currentYaw = mc.player.getYRot();
               float currentPitch = mc.player.getXRot();
               mc.player.setYRot(RotationManager.rotations.x);
               mc.player.setXRot(RotationManager.rotations.y);
               mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(pTarget, false));
               mc.player.swing(InteractionHand.MAIN_HAND);
               mc.player.setYRot(currentYaw);
               mc.player.setXRot(currentPitch);
            }
         }
      }
   }

   @EventTarget
   public void onEarlyTick(EventRunTicks e) {
      if (e.getType() == EventType.PRE && mc.player != null && mc.level != null) {
         Optional<Entity> any = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
            .filter(entityx -> entityx instanceof EndCrystal)
            .findAny();
         rotations = null;
         if (any.isPresent()) {
            Entity entity = any.get();
            Vector2f rots = RotationUtils.getRotations(entity);
            double minDistance = RotationUtils.getMinDistance(entity, rots);
            if (minDistance <= 3.0) {
               rotations = rots;
               this.entity = entity;
            }
         }
      }
   }

   @EventTarget
   public void onClick(EventClick e) {
      if (this.entity != null) {
         float currentYaw = mc.player.getYRot();
         float currentPitch = mc.player.getXRot();
         mc.player.setYRot(rotations.x);
         mc.player.setXRot(rotations.y);
         mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(this.entity, false));
         mc.player.swing(InteractionHand.MAIN_HAND);
         mc.player.setYRot(currentYaw);
         mc.player.setXRot(currentPitch);
         this.entity = null;
      }
   }
}
