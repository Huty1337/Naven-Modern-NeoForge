package com.heypixel.heypixelmod.obsoverlay.modules.impl.render.projectiles;

import java.awt.Color;
import net.minecraft.world.entity.Entity;

public interface ProjectileData {
   Color getColor(Object var1);

   default float getData1() {
      return 0.125F;
   }

   boolean isTargetEntity(Entity var1);

   default float getData2() {
      return 0.25F;
   }

   default float getGravity() {
      return 0.03F;
   }
}
