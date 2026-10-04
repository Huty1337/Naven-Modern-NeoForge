package com.heypixel.heypixelmod.obsoverlay.utils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PotionResolver {
   private static final Logger log = LogManager.getLogger(PotionResolver.class);
   private static final Map<Integer, List<MobEffect>> colorMap = new HashMap<>();

   public static List<MobEffect> resolve(int color) {
      if (colorMap.containsKey(color)) {
         return colorMap.get(color);
      } else if (colorMap.containsKey(color + 1)) {
         return colorMap.get(color + 1);
      } else {
         return colorMap.containsKey(color - 1) ? colorMap.get(color - 1) : Collections.emptyList();
      }
   }

   static {
      InputStream stream = PotionResolver.class.getResourceAsStream("/assets/heypixel/VcX6svVqmeT8/potion_effects.dat");
      if (stream != null) {
         try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(stream);

            for (String s : IOUtils.readLines(gzipInputStream)) {
               String[] split = s.split(":");
               if (split.length == 2) {
                  int color = Integer.parseInt(split[0]);
                  String data = split[1];
                  String[] potionIds = data.split("\\+");
                  List<MobEffect> potions = Arrays.stream(potionIds)
                     .map(Integer::parseInt)
                     .map(id -> BuiltInRegistries.MOB_EFFECT.get(id).map(holder -> (MobEffect)holder.value()).orElse(null))
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
                  colorMap.put(color, potions);
               }
            }
         } catch (Exception var9) {
            log.error("Failed to load potion effects", var9);
         }
      }
   }
}
