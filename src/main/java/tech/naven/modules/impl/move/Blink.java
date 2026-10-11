package tech.naven.modules.impl.move;

import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventMotion;
import tech.naven.events.impl.EventPacket;
import tech.naven.events.impl.EventRender2D;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.combat.AntiBots;
import tech.naven.modules.impl.misc.Teams;
import tech.naven.modules.impl.render.projectiles.ProjectileData;
import tech.naven.modules.impl.render.projectiles.datas.BasicProjectileData;
import tech.naven.modules.impl.render.projectiles.datas.EntityArrowData;
import tech.naven.utils.BlinkingPlayer;
import tech.naven.utils.FriendManager;
import tech.naven.utils.NetworkUtils;
import tech.naven.utils.RayTraceUtils;
import tech.naven.utils.RenderUtils;
import tech.naven.utils.SmoothAnimationTimer;
import tech.naven.utils.rotation.RotationUtils;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.FloatValue;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

@ModuleInfo(
   name = "Blink",
   category = Category.MOVEMENT,
   description = "Suspends all movement packets for teleporting!"
)
public class Blink extends Module {
   private final EntityArrowData arrowData = new EntityArrowData();
   private final BasicProjectileData eggData = new BasicProjectileData(Collections.singleton(ThrownEgg.class), new Color(255, 238, 154));
   private final BasicProjectileData snowballData = new BasicProjectileData(Collections.singleton(Snowball.class), new Color(255, 255, 255));
   private static final int mainColor = new Color(150, 45, 45, 255).getRGB();
   public static final Set<Class<?>> whitelist = new HashSet<Class<?>>() {
      {
         this.add(ClientIntentionPacket.class);
         this.add(ServerboundStatusRequestPacket.class);
         this.add(ServerboundPingRequestPacket.class);
         this.add(ServerboundHelloPacket.class);
         this.add(ServerboundKeyPacket.class);
      }
   };
   private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
   private final SmoothAnimationTimer progress = new SmoothAnimationTimer(0.0F, 0.2F);
   public FloatValue releaseOnDamage = ValueBuilder.create(this, "Release Ticks on Damage")
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(50.0F)
      .setDefaultFloatValue(20.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue releaseSpeed = ValueBuilder.create(this, "Release Speed (Tick)")
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(20.0F)
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue maxTicks = ValueBuilder.create(this, "Max Ticks")
      .setMinFloatValue(10.0F)
      .setMaxFloatValue(500.0F)
      .setDefaultFloatValue(200.0F)
      .setFloatStep(1.0F)
      .build()
      .getFloatValue();
   public FloatValue playerDistance = ValueBuilder.create(this, "Player Distance")
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(10.0F)
      .setDefaultFloatValue(4.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public FloatValue tntDistance = ValueBuilder.create(this, "TNT Distance")
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(10.0F)
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.1F)
      .build()
      .getFloatValue();
   public FloatValue projectilesExpands = ValueBuilder.create(this, "Fake Player HitBoxes")
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(3.0F)
      .setDefaultFloatValue(0.2F)
      .setFloatStep(0.01F)
      .build()
      .getFloatValue();
   private boolean disabling = false;
   private RemotePlayer fakePlayer;
   private int shouldReleaseTicks = 0;
   private int releasedTicks = 0;

   private long getBlinkTicks() {
      return this.packets.stream().filter(packet -> packet instanceof ServerboundMovePlayerPacket).count();
   }

   private void handleMove(ServerboundMovePlayerPacket packet) {
      this.fakePlayer
         .moveOrInterpolateTo(
            new Vec3(
               packet.getX(this.fakePlayer.getX()),
               packet.getY(this.fakePlayer.getY()),
               packet.getZ(this.fakePlayer.getZ())
            ),
            packet.getYRot(this.fakePlayer.getYRot()),
            packet.getXRot(this.fakePlayer.getXRot())
         );
      if (packet.hasRotation()) {
         this.fakePlayer.setYRot(packet.getYRot(this.fakePlayer.getYRot()));
         this.fakePlayer.setYHeadRot(packet.getYRot(this.fakePlayer.getYRot()));
         this.fakePlayer.setXRot(packet.getXRot(this.fakePlayer.getXRot()));
      }
   }

   private void releaseTick() {
      while (!this.packets.isEmpty()) {
         Packet<?> poll = this.packets.poll();
         NetworkUtils.sendPacketNoEvent(poll);
         if (poll instanceof ServerboundMovePlayerPacket) {
            this.releasedTicks++;
            this.handleMove((ServerboundMovePlayerPacket)poll);
            break;
         }
      }
   }

   @Override
   public void onEnable() {
      this.packets.clear();
      this.shouldReleaseTicks = 0;
      this.disabling = false;
      this.fakePlayer = new BlinkingPlayer(mc.player);
      this.fakePlayer.setSprinting(mc.player.isSprinting());
      mc.level.addEntity(this.fakePlayer);
   }

   @Override
   public void onDisable() {
      if (this.fakePlayer != null) {
         mc.level.removeEntity(this.fakePlayer.getId(), RemovalReason.DISCARDED);
         this.fakePlayer = null;
      }
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      int x = mc.getWindow().getGuiScaledWidth() / 2 - 50;
      int y = mc.getWindow().getGuiScaledHeight() / 2 + 15;
      this.progress.update(true);
      RenderUtils.drawRoundedRect(e.getGuiGraphics(), (float)x, (float)y, 100.0F, 5.0F, 2.0F, Integer.MIN_VALUE);
      RenderUtils.drawRoundedRect(e.getGuiGraphics(), (float)x, (float)y, this.progress.value, 5.0F, 2.0F, mainColor);
   }

   private boolean isPlayerNear(double distance) {
      long players = mc.level.players().stream().filter(player -> {
         if (player == mc.player) {
            return false;
         } else if (player instanceof BlinkingPlayer) {
            return false;
         } else if (Teams.isSameTeam(player)) {
            return false;
         } else if (FriendManager.isFriend(player)) {
            return false;
         } else if (AntiBots.isBot(player)) {
            return false;
         } else {
            Vec3 eyePosition = player.getEyePosition();
            Vec3 closestPoint = RotationUtils.getClosestPoint(eyePosition, this.fakePlayer.getBoundingBox());
            return eyePosition.distanceTo(closestPoint) < distance;
         }
      }).count();
      return players > 0L;
   }

   private boolean isTNTNear(double distance) {
      Stream<Entity> stream = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), true);
      long tnt = stream.filter(entity -> entity instanceof PrimedTnt && (double)this.fakePlayer.distanceTo(entity) <= distance).count();
      return tnt > 0L;
   }

   private boolean isArrowNear(double expands) {
      for (Entity entity : mc.level.entitiesForRendering()) {
         ProjectileData data;
         if (entity instanceof Arrow) {
            data = this.arrowData;
         } else if (entity instanceof ThrownEgg) {
            data = this.eggData;
         } else {
            if (!(entity instanceof Snowball)) {
               continue;
            }

            data = this.snowballData;
         }

         if (data != null && this.checkProjectile(entity, data, expands)) {
            return true;
         }
      }

      return false;
   }

   private boolean checkProjectile(Entity entity, ProjectileData projectileInfo, double expands) {
      LocalPlayer thePlayer = mc.player;
      ClientLevel theWorld = mc.level;
      double posX = entity.getX();
      double posY = entity.getY();
      double posZ = entity.getZ();
      double motionX = entity.getDeltaMovement().x;
      double motionY = entity.getDeltaMovement().y;
      double motionZ = entity.getDeltaMovement().z;

      while (true) {
         float data1 = projectileInfo.getData1();
         float data2 = projectileInfo.getData2();
         AABB aabb = new AABB(posX - (double)data1, posY, posZ - (double)data1, posX + (double)data1, posY + (double)data2, posZ + (double)data1);
         Vec3 vec3 = new Vec3(posX, posY, posZ);
         Vec3 vec3WithMotion = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
         HitResult movingObj = RayTraceUtils.rayTraceBlocks(vec3, vec3WithMotion, false, entity instanceof Arrow, false, entity);
         List<Entity> entities = theWorld.getEntities(
            thePlayer, aabb.contract(motionX, motionY, motionZ).expandTowards(1.0, 1.0, 1.0).inflate(expands, expands, expands)
         );
         if (entities.contains(this.fakePlayer)) {
            return true;
         }

         posX += motionX;
         posY += motionY;
         posZ += motionZ;
         if (!movingObj.getType().equals(Type.MISS) || posY < -128.0) {
            return false;
         }

         motionX *= entity.isInWater() ? 0.8 : 0.99;
         double var39 = motionY * (entity.isInWater() ? 0.8 : 0.99);
         motionZ *= entity.isInWater() ? 0.8 : 0.99;
         motionY = var39 - (double)projectileInfo.getGravity();
      }
   }

   private boolean isPlayerInDanger() {
      return this.isTNTNear((double)this.tntDistance.getCurrentValue())
         || this.isPlayerNear((double)this.playerDistance.getCurrentValue())
         || this.isArrowNear((double)this.projectilesExpands.getCurrentValue());
   }

   @Override
   public void setEnabled(boolean enabled) {
      if (mc.player != null) {
         if (enabled) {
            super.setEnabled(true);
         } else if (!this.disabling) {
            this.disabling = true;
         } else if (this.packets.isEmpty()) {
            super.setEnabled(false);
         }
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && mc.player != null) {
         this.setSuffix(this.getBlinkTicks() + " Ticks Behind");
         this.progress.target = Mth.clamp((float)this.getBlinkTicks() / this.maxTicks.getCurrentValue() * 100.0F, 0.0F, 100.0F);
         this.releasedTicks = 0;
         if (mc.player.hurtTime == 10) {
            this.shouldReleaseTicks = this.shouldReleaseTicks + (int)this.releaseOnDamage.getCurrentValue();
         }

         while ((float)this.releasedTicks < this.releaseSpeed.getCurrentValue() && this.shouldReleaseTicks > 0 && !this.packets.isEmpty()) {
            this.releaseTick();
            this.shouldReleaseTicks--;
         }

         while ((float)this.releasedTicks < this.releaseSpeed.getCurrentValue() && this.isPlayerInDanger() && !this.packets.isEmpty()) {
            this.releaseTick();
         }

         while (
            (float)this.releasedTicks < this.releaseSpeed.getCurrentValue()
               && (float)this.getBlinkTicks() >= this.maxTicks.getCurrentValue()
               && !this.packets.isEmpty()
         ) {
            this.releaseTick();
         }

         if (this.disabling) {
            while ((float)this.releasedTicks < this.releaseSpeed.getCurrentValue() && !this.packets.isEmpty()) {
               this.releaseTick();
            }

            if (this.packets.isEmpty()) {
               this.setEnabled(false);
            }
         }
      }
   }

   @EventTarget(4)
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.SEND && mc.player != null && !e.isCancelled()) {
         if (whitelist.contains(e.getPacket().getClass())) {
            return;
         }

         e.setCancelled(true);
         this.packets.offer(e.getPacket());
      }
   }
}
