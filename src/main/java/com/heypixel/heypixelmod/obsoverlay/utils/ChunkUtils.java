package com.heypixel.heypixelmod.obsoverlay.utils;

import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   public static Stream<BlockEntity> getLoadedBlockEntities() {
      return getLoadedChunks().flatMap(chunk -> chunk.getBlockEntities().values().stream());
   }

   public static Stream<LevelChunk> getLoadedChunks() {
      int radius = Math.max(2, mc.options.getEffectiveRenderDistance()) + 3;
      int diameter = radius * 2 + 1;
      ChunkPos center = mc.player.chunkPosition();
      ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
      ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);
      return Stream.<ChunkPos>iterate(min, pos -> {
         int x = pos.x;
         int z = pos.z;
         if (++x > max.x) {
            x = min.x;
            z++;
         }

         if (z > max.z) {
            throw new IllegalStateException("Stream limit didn't work.");
         } else {
            return new ChunkPos(x, z);
         }
      }).limit((long)diameter * (long)diameter).filter(c -> mc.level.hasChunk(c.x, c.z)).map(c -> mc.level.getChunk(c.x, c.z)).filter(Objects::nonNull);
   }
}
