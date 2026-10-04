package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.mixin.O.accessors.LocalPlayerAccessor;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.LongJump;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import java.util.concurrent.LinkedBlockingDeque;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
   name = "Velocity",
   description = "Reduces knockback.",
   category = Category.MOVEMENT
)
public class Velocity extends Module {
   LinkedBlockingDeque<Packet<ClientGamePacketListener>> inBound = new LinkedBlockingDeque<>();
   public static Velocity.Stage stage = Velocity.Stage.IDLE;
   public static int grimTick = -1;
   public static int debugTick = 10;
   public BooleanValue log = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
   Packet velocityPacket;
   private BlockHitResult result = null;
   Scaffold.BlockPosWithFacing pos;

   private boolean shouldAvoidInteraction(Block block) {
      return block instanceof ChestBlock
         || block instanceof CraftingTableBlock
         || block instanceof FurnaceBlock
         || block instanceof EnderChestBlock
         || block instanceof BarrelBlock
         || block instanceof ShulkerBoxBlock
         || block instanceof AnvilBlock
         || block instanceof EnchantingTableBlock
         || block instanceof BrewingStandBlock
         || block instanceof BeaconBlock
         || block instanceof HopperBlock
         || block instanceof DispenserBlock
         || block instanceof DropperBlock
         || block instanceof LecternBlock
         || block instanceof CartographyTableBlock
         || block instanceof SmithingTableBlock
         || block instanceof StonecutterBlock
         || block instanceof LoomBlock
         || block instanceof GrindstoneBlock
         || block instanceof ComposterBlock
         || block instanceof CauldronBlock
         || block instanceof BedBlock
         || block instanceof DoorBlock
         || block instanceof TrapDoorBlock
         || block instanceof FenceGateBlock
         || block instanceof ButtonBlock
         || block instanceof LeverBlock
         || block instanceof NoteBlock;
   }

   public void reset() {
      if (mc.getConnection() != null) {
         stage = Velocity.Stage.IDLE;
         grimTick = -1;
         debugTick = 0;
         this.processPackets();
      }
   }

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

   public Direction checkBlock(Vec3 baseVec, BlockPos bp) {
      if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
         return null;
      } else {
         Vec3 center = new Vec3((double)bp.getX() + 0.5, (double)((float)bp.getY() + 0.5F), (double)bp.getZ() + 0.5);
         Direction sbface = Direction.DOWN;
         Vec3 hit = center.add(
            new Vec3((double)sbface.getUnitVec3i().getX() * 0.5, (double)sbface.getUnitVec3i().getY() * 0.5, (double)sbface.getUnitVec3i().getZ() * 0.5)
         );
         Vec3i baseBlock = bp.offset(sbface.getUnitVec3i());
         BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
         if (!mc.level.getBlockState(po).entityCanStandOnFace(mc.level, po, mc.player, sbface)) {
            return null;
         } else {
            Vec3 relevant = hit.subtract(baseVec);
            if (relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(sbface.getUnitVec3i()).normalize()) >= 0.0) {
               this.pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), sbface.getOpposite());
               return sbface.getOpposite();
            } else {
               return null;
            }
         }
      }
   }

   @Override
   public void onEnable() {
      this.reset();
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   private void log(String message) {
      if (this.log.getCurrentValue()) {
         ChatUtils.addChatMessage(message);
      }
   }

   @EventTarget
   public void onWorld(EventRespawn eventRespawn) {
      this.reset();
   }

   @EventTarget
   public void onTick(EventRunTicks eventRunTicks) {
      if (mc.player != null && mc.getConnection() != null && mc.gameMode != null && eventRunTicks.getType() != EventType.POST) {
         if (!Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
            if (mc.player.isDeadOrDying()
               || !mc.player.isAlive()
               || mc.player.getHealth() <= 0.0F
               || mc.screen instanceof ProgressScreen
               || mc.screen instanceof DeathScreen) {
               this.reset();
            }

            if (debugTick > 0) {
               debugTick--;
               if (debugTick == 0) {
                  this.processPackets();
                  stage = Velocity.Stage.IDLE;
               }
            } else {
               stage = Velocity.Stage.IDLE;
            }

            if (grimTick > 0) {
               grimTick--;
            }

            float yaw = RotationManager.rotations.getX();
            float pitch = 89.79F;
            BlockHitResult blockRayTraceResult = (BlockHitResult)PlayerUtils.pickCustom(3.7F, yaw, pitch);
            if (stage == Velocity.Stage.TRANSACTION
               && grimTick == 0
               && blockRayTraceResult != null
               && !BlockUtils.isAirBlock(blockRayTraceResult.getBlockPos())
               && mc.player.getBoundingBox().intersects(new AABB(blockRayTraceResult.getBlockPos().above()))) {
               Block targetBlock = mc.level.getBlockState(blockRayTraceResult.getBlockPos()).getBlock();
               if (targetBlock instanceof ChestBlock
                  || targetBlock instanceof CraftingTableBlock
                  || targetBlock instanceof FurnaceBlock
                  || targetBlock instanceof EnchantingTableBlock
                  || targetBlock instanceof AnvilBlock
                  || targetBlock instanceof BarrelBlock
                  || targetBlock instanceof ShulkerBoxBlock) {
                  return;
               }

               this.result = new BlockHitResult(blockRayTraceResult.getLocation(), blockRayTraceResult.getDirection(), blockRayTraceResult.getBlockPos(), false);
               ((LocalPlayerAccessor)mc.player).setYRotLast(yaw);
               ((LocalPlayerAccessor)mc.player).setXRotLast(pitch);
               RotationManager.setRotations(new Rotation(yaw, pitch).toVec2f());
               if (Aura.rotation != null) {
                  Aura.rotation = new Rotation(yaw, pitch).toVec2f();
               }

               this.processPackets();
               mc.player.connection.send(new Rot(yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
               mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, this.result);
               Naven.skipTasks.add(() -> {
               });

               for (int i = 2; i <= 100; i++) {
                  Naven.skipTasks
                     .add(
                        () -> {
                           EventMotion event1 = new EventMotion(
                              EventType.PRE, mc.player.position().x, mc.player.position().y, mc.player.position().z, yaw, pitch, mc.player.onGround()
                           );
                           Naven.getInstance().getRotationManager().onPre(event1);
                           if (event1.getYaw() != yaw || event1.getPitch() != pitch) {
                              mc.player.connection.send(new Rot(event1.getYaw(), event1.getPitch(), mc.player.onGround(), mc.player.horizontalCollision));
                           }
                        }
                     );
               }

               debugTick = 20;
               stage = Velocity.Stage.BLOCK;
               grimTick = 0;
            }
         }
      }
   }

   @EventTarget
   public void onPacket(EventHandlePacket e) {
      if (mc.player != null && mc.getConnection() != null && mc.gameMode != null && !mc.player.isUsingItem()) {
         if (!Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
            if (mc.player.tickCount < 20) {
               this.reset();
            } else if (!mc.player.isDeadOrDying()
               && mc.player.isAlive()
               && !(mc.player.getHealth() <= 0.0F)
               && !(mc.screen instanceof ProgressScreen)
               && !(mc.screen instanceof DeathScreen)) {
               Packet<?> packet = e.getPacket();
               if (packet instanceof ClientboundLoginPacket) {
                  this.reset();
               } else {
                  if (debugTick > 0 && mc.player.tickCount > 20) {
                     if (stage == Velocity.Stage.BLOCK
                        && packet instanceof ClientboundBlockUpdatePacket cbu
                        && this.result != null
                        && this.result.getBlockPos().equals(cbu.getPos())) {
                        this.processPackets();
                        Naven.skipTasks.clear();
                        debugTick = 0;
                        this.result = null;
                        return;
                     }

                     if (!(packet instanceof ClientboundSystemChatPacket) && !(packet instanceof ClientboundSetTimePacket)) {
                        e.setCancelled(true);
                        this.inBound.add((Packet<ClientGamePacketListener>)packet);
                        return;
                     }
                  }

                  if (packet instanceof ClientboundSetEntityMotionPacket packetEntityVelocity) {
                     if (packetEntityVelocity.getId() != mc.player.getId()) {
                        return;
                     }

                     if (packetEntityVelocity.getMovement().y < 0 || mc.player.getMainHandItem().getItem() instanceof EnderpearlItem) {
                        e.setCancelled(false);
                        return;
                     }

                     grimTick = 2;
                     debugTick = 100;
                     stage = Velocity.Stage.TRANSACTION;
                     e.setCancelled(true);
                  }
               }
            } else {
               this.reset();
            }
         }
      }
   }

   public static enum Stage {
      TRANSACTION,
      ROTATION,
      BLOCK,
      IDLE;
   }
}
