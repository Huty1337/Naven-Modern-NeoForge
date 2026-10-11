package tech.naven.modules.impl.combat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventHandlePacket;
import tech.naven.events.impl.EventMoveInput;
import tech.naven.events.impl.EventRespawn;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.move.Scaffold;
import tech.naven.modules.impl.move.Stuck;
import tech.naven.utils.Vector2f;
import tech.naven.utils.rotation.RotationManager;
import tech.naven.utils.rotation.RotationUtils;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(
   name = "Velocity",
   description = "Reduces knockback.",
   category = Category.MOVEMENT
)
public class Velocity extends Module {
   LinkedBlockingDeque<Packet<ClientGamePacketListener>> inBound = new LinkedBlockingDeque<>();
   public static Velocity.Stage stage = Velocity.Stage.IDLE;
   public FloatValue attackAmount = ValueBuilder.create(this, "Attack Amount")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(5.0F)
      .build()
      .getFloatValue();
   public FloatValue maxDelayTicks = ValueBuilder.create(this, "Max Delay Ticks")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(120.0F)
      .build()
      .getFloatValue();
   public BooleanValue autoForwards = ValueBuilder.create(this, "Auto Forwards")
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   public BooleanValue requireKillAura = ValueBuilder.create(this, "Require KillAura")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   public BooleanValue jumpReset = ValueBuilder.create(this, "Jump Reset")
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   Packet velocityPacket;
   private BlockHitResult result = null;
   Scaffold.BlockPosWithFacing pos;

   public void processPackets() {
      ClientPacketListener connection = mc.getConnection();
      if (connection == null) {
         this.inBound.clear();
      } else {
         Packet<ClientGamePacketListener> packet;
         while ((packet = this.inBound.poll()) != null) {
            try {
               packet.handle(connection);
            } catch (Exception var4) {
               var4.printStackTrace();
               this.inBound.clear();
               break;
            }
         }
      }
   }


   @EventTarget
   public void onWorld(EventRespawn eventRespawn) {
      this.onDisconnect();
   }

   @EventTarget
   public void onTick(EventRunTicks eventRunTicks) {
      if (eventRunTicks.getType() != EventType.POST) {
         this.onTick();
      }
   }

   @EventTarget
   public void onPacket(EventHandlePacket e) {
      this.onReceivePacket(e);
   }

   public static enum Stage {
      TRANSACTION,
      ROTATION,
      BLOCK,
      IDLE;
   }

   private final Queue<Packet<ClientGamePacketListener>> packetQueue = new ConcurrentLinkedQueue<>();

   public boolean handlingVelocity;
   public boolean compensating;
   public boolean inDelayWindow;
   public int counterRemaining;

   private boolean positionCorrection;
   private int delayTicks;
   private int noAimTicks;
   private int noSprintTicks;
   private int jumpTicks;
   private boolean jumpKeyForced;
   private long compensateUntilMs = -1L;
   private Entity target;
   private Entity lastAttacker;
   private long lastHurtTime;

   private static Minecraft mc() {
      return Minecraft.getInstance();
   }

   public boolean isActive() {
      return this.inDelayWindow || this.counterRemaining > 0;
   }

   public boolean isCountering() {
      return this.counterRemaining > 0;
   }

   public boolean isInDelayWindow() {
      return this.inDelayWindow;
   }

   public boolean isHandlingVelocity() {
      return this.handlingVelocity;
   }

   public Entity getTarget() {
      return this.target;
   }

   public void onEnable() {
      this.resetAll();
   }

   public void onDisable() {
      this.resetAll();
   }

   public void onTick() {
      Minecraft mc = mc();
      if (mc.player == null || mc.level == null) {
         return;
      }

      this.tickJumpReset();

      this.target = this.requireKillAura.getCurrentValue() ? this.auraOrFallbackTarget() : this.crosshairTarget();

      if (this.compensateUntilMs != -1L && (System.currentTimeMillis() >= this.compensateUntilMs || mc.options.keyUp.isDown())) {
         this.compensating = false;
         this.compensateUntilMs = -1L;
      }

      if (this.shouldNotEngage() || this.noAimTicks >= 3) {
         this.resetAll();
         return;
      }

      if (this.inDelayWindow) {
         this.compensating = true;
         this.counterRemaining = 0;
         if (this.delayTicks >= (int)this.maxDelayTicks.getCurrentValue()) {
            this.resetAll();
            return;
         }

         this.delayTicks++;
         if (this.isAimingAtTarget() && mc.player.isSprinting() && mc.player.onGround()) {
            this.counterRemaining = (int)this.attackAmount.getCurrentValue();
            this.noAimTicks = 0;
            this.inDelayWindow = false;
            this.handlingVelocity = false;
            this.flushQueue();
            if (this.jumpReset.getCurrentValue()) {
               this.jumpTicks = 1;
            }
         }
      }

      if (this.counterRemaining > 0) {
         if (!this.isAimingAtTarget()) {
            this.noAimTicks++;
            return;
         }

         if (!mc.player.isSprinting()) {
            this.noSprintTicks++;
            if (this.noSprintTicks >= 3) {
               this.resetAll();
            }

            return;
         }

         this.noSprintTicks = 0;
         this.attackReduce(this.target);
         this.counterRemaining--;
      } else if (this.compensating) {
         this.compensateUntilMs = System.currentTimeMillis() + 10L;
      }
   }

   private void tickJumpReset() {
      Minecraft mc = mc();
      if (!this.jumpReset.getCurrentValue() || this.jumpTicks <= 0) {
         this.restoreJumpKey();
         return;
      }

      Scaffold scaffold = (Scaffold) Naven.getInstance().getModuleManager().getModule(Scaffold.class);
      if (scaffold != null && scaffold.isEnabled()) {
         return;
      }

      if (mc.player.isOnFire()) {
         this.jumpTicks = 0;
         this.restoreJumpKey();
         return;
      }

      this.jumpKeyForced = true;
      mc.options.keyJump.setDown(true);
      this.jumpTicks--;
   }

   public void onReceivePacket(EventHandlePacket event) {
      Minecraft mc = mc();
      if (mc.player == null || mc.level == null) {
         return;
      }

      Packet<ClientGamePacketListener> packet = event.getPacket();

      if (packet instanceof ClientboundPlayerPositionPacket) {
         this.positionCorrection = true;
         return;
      }

      if (packet instanceof ClientboundHurtAnimationPacket hurt && hurt.id() == mc.player.getId()) {
         Entity candidate = Aura.target != null ? Aura.target : this.crosshairTarget();
         if (candidate != null) {
            this.lastAttacker = candidate;
            this.lastHurtTime = System.currentTimeMillis();
         }
      }

      if (this.shouldNotEngage()) {
         return;
      }

      if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.getId() == mc.player.getId()) {
         if (!this.inDelayWindow) {
            this.inDelayWindow = true;
            this.delayTicks = 0;
            this.handlingVelocity = true;
         }

         event.setCancelled(true);
         this.packetQueue.add(packet);
      }

      if (this.isAllowedPacket(packet)) {
         return;
      }

      if (!this.inDelayWindow) {
         return;
      }

      event.setCancelled(true);
      this.packetQueue.add(packet);
   }

   @EventTarget(0)
   public void onMoveInput(EventMoveInput event) {
      Minecraft mc = mc();
      if (mc.player == null) {
         return;
      }

      if (this.compensating && this.autoForwards.getCurrentValue()) {
         event.setForward(1.0F);
      }
   }

   public void onDisconnect() {
      this.resetAll();
   }

   private void attackReduce(Entity entity) {
      Minecraft mc = mc();
      if (entity == null || mc.player == null || mc.getConnection() == null) {
         return;
      }

      mc.gameMode.attack(mc.player, entity);
      mc.player.swing(InteractionHand.MAIN_HAND);
      Vec3 movement = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(movement.x * 0.6, movement.y, movement.z * 0.6);
   }

   private Entity crosshairTarget() {
      return mc().hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
   }

   private Entity auraOrFallbackTarget() {
      if (Aura.target != null) {
         return Aura.target;
      }

      Entity crosshair = this.crosshairTarget();
      if (crosshair != null) {
         return crosshair;
      }

      return this.lastAttacker != null
              && this.lastAttacker.isAlive()
              && System.currentTimeMillis() - this.lastHurtTime <= 1500L
              ? this.lastAttacker
              : null;
   }

   private boolean isAimingAtTarget() {
      Minecraft mc = mc();
      if (this.target == null || mc.player == null) {
         return false;
      }

      if (!this.requireKillAura.getCurrentValue()) {
         return mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == this.target;
      }

      Vector2f aim = RotationManager.active && RotationManager.rotations != null
              ? RotationManager.rotations
              : new Vector2f(mc.player.getYRot(), mc.player.getXRot());
      return RotationUtils.getDistance(this.target, mc.player.getEyePosition(), aim) < 3.0;
   }

   private boolean shouldNotEngage() {
      Minecraft mc = mc();
      if (mc.player == null || mc.getConnection() == null) {
         return true;
      }

      if (mc.player.isUsingItem() || mc.player.isInLava() || mc.player.isInWater()) {
         return true;
      }

      if (mc.level != null && mc.level.getBlockState(mc.player.blockPosition()).is(Blocks.COBWEB)) {
         return true;
      }

      if (this.positionCorrection) {
         return true;
      }

      if (mc.screen instanceof InventoryScreen) {
         return true;
      }

      if (this.target == null || !this.target.isAlive()) {
         return true;
      }

      if (mc.player.isDeadOrDying() || mc.player.isSpectator()) {
         return true;
      }

      Scaffold scaffold = (Scaffold)Naven.getInstance().getModuleManager().getModule(Scaffold.class);
      if (scaffold != null && scaffold.isEnabled()) {
         return true;
      }

      Stuck stuck = (Stuck)Naven.getInstance().getModuleManager().getModule(Stuck.class);
      return stuck != null && stuck.isEnabled();
   }

   private boolean isAllowedPacket(Packet<?> packet) {
      Minecraft mc = mc();
      return packet instanceof ClientboundSetEntityMotionPacket
              || packet instanceof ClientboundSetHealthPacket
              || packet instanceof ClientboundPlayerPositionPacket
              || packet instanceof ClientboundSoundPacket
              || packet instanceof ClientboundPlayerChatPacket
              || packet instanceof ClientboundPlayerCombatKillPacket
              || packet instanceof ClientboundContainerClosePacket
              || packet instanceof ClientboundHurtAnimationPacket
              || packet instanceof ClientboundSetTitleTextPacket
              || packet instanceof ClientboundSetPlayerTeamPacket
              || packet instanceof ClientboundSystemChatPacket
              || packet instanceof ClientboundDisconnectPacket
              || packet instanceof ClientboundAnimatePacket animation && animation.getId() != mc.player.getId();
   }

   private void flushQueue() {
      Minecraft mc = mc();
      mc.execute(() -> {
         if (mc.getConnection() == null) {
            this.packetQueue.clear();
            return;
         }

         Packet<ClientGamePacketListener> packet;
         while ((packet = this.packetQueue.poll()) != null) {
            try {
               packet.handle(mc.getConnection());
            } catch (Exception exception) {
               this.packetQueue.clear();
               break;
            }
         }
      });
   }

   private void restoreJumpKey() {
      if (!this.jumpKeyForced) {
         return;
      }

      this.jumpKeyForced = false;
      Minecraft mc = mc();
      Scaffold scaffold = (Scaffold)Naven.getInstance().getModuleManager().getModule(Scaffold.class);
      if (mc.player == null || scaffold != null && scaffold.isEnabled()) {
         return;
      }

      boolean down = InputConstants.isKeyDown(mc.getWindow(), mc.options.keyJump.getKey().getValue());
      mc.options.keyJump.setDown(down);
   }

   private void resetAll() {
      this.flushQueue();
      this.inDelayWindow = false;
      this.compensating = false;
      this.positionCorrection = false;
      this.delayTicks = 0;
      this.counterRemaining = 0;
      this.noAimTicks = 0;
      this.noSprintTicks = 0;
      this.compensateUntilMs = -1L;
      this.jumpTicks = 0;
      this.restoreJumpKey();
      this.target = null;
      this.handlingVelocity = false;
      if (mc.getConnection() != null) {
         stage = Velocity.Stage.IDLE;
         this.processPackets();
      }
   }
}
