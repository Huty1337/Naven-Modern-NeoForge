package tech.naven.modules.impl.render;

import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventMotion;
import tech.naven.events.impl.EventPacket;
import tech.naven.events.impl.EventRender;
import tech.naven.events.impl.EventRespawn;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.utils.BlockUtils;
import tech.naven.utils.ChunkUtils;
import tech.naven.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;

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

@ModuleInfo(
   name = "ChestESP",
   description = "Highlights chests",
   category = Category.RENDER
)
public class ChestESP extends Module {
   private static final float[] chestColor = new float[]{0.0F, 0.8F, 0.0F};
   private static final float[] openedChestColor = new float[]{0.8F, 0.0F, 0.0F};
   private final List<BlockPos> openedChests = new CopyOnWriteArrayList<>();
   private final List<AABB> renderBoundingBoxes = new CopyOnWriteArrayList<>();

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
         for (BlockEntity blockEntity : blockEntities) {
            if (blockEntity instanceof ChestBlockEntity) {
               ChestBlockEntity chestBE = (ChestBlockEntity)blockEntity;
               AABB box = this.getChestBox(chestBE);
               if (box != null) {
                  this.renderBoundingBoxes.add(box);
               }
            }
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
   }
}
