package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ChunkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.slf4j.Logger;

@ModuleInfo(
   name = "ChestESP",
   description = "Highlights chests",
   category = Category.RENDER
)
public class ChestESP extends Module {
   private static final float[] chestColor = new float[]{0.0F, 1.0F, 0.0F};
   private static final float[] openedChestColor = new float[]{1.0F, 0.0F, 0.0F};
   private final List<BlockPos> openedChests = new CopyOnWriteArrayList<>();
   private final List<AABB> renderBoundingBoxes = new CopyOnWriteArrayList<>();

   private static final boolean DEBUG_ESP = false;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static long debugLastLogMillis;
   private static long debugTicks;
   private static long debugRenders;
   private static int debugChestBlockEntities;
   private static int debugSkippedChests;

   @Override
   protected void initModule() {
      super.initModule();
      if (DEBUG_ESP) {
         LOGGER.info("[ChestESP] phase=init enabled={}", this.isEnabled());
      }
   }

   @Override
   public void onEnable() {
      if (DEBUG_ESP) {
         LOGGER.info("[ChestESP] phase=enable enabled=true");
      }
   }

   @Override
   public void onDisable() {
      if (DEBUG_ESP) {
         LOGGER.info("[ChestESP] phase=disable enabled=false");
      }
   }

   @EventTarget
   public void onRespawn(EventRespawn e) {
      this.openedChests.clear();
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof ClientboundBlockEventPacket) {
         ClientboundBlockEventPacket packet = (ClientboundBlockEventPacket)e.getPacket();
         if ((packet.getBlock() == Blocks.CHEST || packet.getBlock() == Blocks.TRAPPED_CHEST) && packet.getB0() == 1 && packet.getB1() == 1) {
            this.openedChests.add(packet.getPos());
         }
      }
   }

   @EventTarget
   public void onTick(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         ArrayList<BlockEntity> blockEntities = ChunkUtils.getLoadedBlockEntities().collect(Collectors.toCollection(ArrayList::new));
         this.renderBoundingBoxes.clear();
         int chests = 0;
         int skipped = 0;

         for (BlockEntity blockEntity : blockEntities) {
            if (blockEntity instanceof ChestBlockEntity) {
               ChestBlockEntity chestBE = (ChestBlockEntity)blockEntity;
               chests++;
               AABB box = this.getChestBox(chestBE);
               if (box != null) {
                  this.renderBoundingBoxes.add(box);
               } else {
                  skipped++;
               }
            }
         }

         if (DEBUG_ESP) {
            debugTicks++;
            debugChestBlockEntities = chests;
            debugSkippedChests = skipped;
            this.logState("tick", false);
         }
      }
   }

   private AABB getChestBox(ChestBlockEntity chestBE) {
      BlockState state = chestBE.getBlockState();
      if (!state.hasProperty(ChestBlock.TYPE)) {
         return null;
      } else {
         ChestType chestType = (ChestType)state.getValue(ChestBlock.TYPE);
         if (chestType == ChestType.LEFT) {
            return null;
         } else {
            BlockPos pos = chestBE.getBlockPos();
            AABB box = BlockUtils.getBoundingBox(pos);
            if (chestType != ChestType.SINGLE) {
               BlockPos pos2 = pos.relative(ChestBlock.getConnectedDirection(state));
               if (BlockUtils.canBeClicked(pos2)) {
                  AABB box2 = BlockUtils.getBoundingBox(pos2);
                  box = box.minmax(box2);
               }
            }

            return box;
         }
      }
   }

   @EventTarget
   public void onRender(EventRender e) {
      PoseStack stack = e.getPMatrixStack();
      stack.pushPose();

      Vec3 cameraPos = RenderUtils.getCameraPos();

      for (AABB box : this.renderBoundingBoxes) {
         BlockPos pos = BlockPos.containing(box.minX, box.minY, box.minZ);
         float[] color = this.openedChests.contains(pos) ? openedChestColor : chestColor;
         AABB relativeBox = box.move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
         Render3DUtils.filledBox(stack, relativeBox, color[0], color[1], color[2], 0.25F);
      }

      stack.popPose();

      if (DEBUG_ESP) {
         debugRenders++;
         this.logState("render", true);
      }
   }

   private void logState(String phase, boolean geometry) {
      if (!DEBUG_ESP) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - debugLastLogMillis < 1000L) {
         return;
      }

      debugLastLogMillis = now;
      AABB first = this.renderBoundingBoxes.isEmpty() ? null : this.renderBoundingBoxes.get(0);
      Vec3 camera = !geometry || mc.getEntityRenderDispatcher().camera == null ? null : RenderUtils.getCameraPos();
      String cameraText = camera == null ? "-" : "(" + fmt(camera.x) + ", " + fmt(camera.y) + ", " + fmt(camera.z) + ")";
      String worldText = first == null ? "-" : "(" + fmt(first.minX) + ", " + fmt(first.minY) + ", " + fmt(first.minZ) + ")";
      String relativeText = first == null || camera == null
         ? "-"
         : "(" + fmt(first.minX - camera.x) + ", " + fmt(first.minY - camera.y) + ", " + fmt(first.minZ - camera.z) + ")";
      String distanceText = first == null || camera == null ? "-" : fmt(distanceToCamera(first, camera));
      String within100 = "-";
      if (camera != null) {
         int inside = 0;
         for (AABB box : this.renderBoundingBoxes) {
            if (distanceToCamera(box, camera) < 100.0) {
               inside++;
            }
         }

         within100 = Integer.toString(inside);
      }

      LOGGER.info(
         "[ChestESP] phase={} enabled={} ticks={} renders={} chestBEs={} skipped={} boxes={} opened={} dist0={} within100={} camera={} world0={} rel0={}",
         phase,
         this.isEnabled(),
         debugTicks,
         debugRenders,
         debugChestBlockEntities,
         debugSkippedChests,
         this.renderBoundingBoxes.size(),
         this.openedChests.size(),
         distanceText,
         within100,
         cameraText,
         worldText,
         relativeText
      );
   }

   public static final class StateReminder {
      private static long lastReminderMillis;

      @SubscribeEvent
      public void onLevelStage(RenderLevelStageEvent.AfterLevel event) {
         if (!DEBUG_ESP) {
            return;
         }

         Naven naven = Naven.getInstance();
         if (naven == null || naven.getModuleManager() == null) {
            return;
         }

         ChestESP module = null;
         for (Module candidate : naven.getModuleManager().getModules()) {
            if (candidate.getClass() == ChestESP.class) {
               module = (ChestESP)candidate;
               break;
            }
         }

         if (module == null || module.isEnabled()) {
            return;
         }

         long now = System.currentTimeMillis();
         if (now - lastReminderMillis < 5000L) {
            return;
         }

         lastReminderMillis = now;
         LOGGER.info("[ChestESP] phase=disabled enabled=false hint=\"enable via ClickGUI -> Render -> Chest ESP\"");
      }
   }

   private static double distanceToCamera(AABB box, Vec3 camera) {
      Vec3 center = box.getCenter();
      double dx = center.x - camera.x;
      double dy = center.y - camera.y;
      double dz = center.z - camera.z;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static String fmt(double value) {
      return String.format("%.1f", value);
   }
}
