package com.heypixel.heypixelmod.obsoverlay.modules.impl.render.projectiles.datas;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import net.minecraft.world.entity.projectile.Arrow;

public class EntityArrowData extends BasicProjectileData {
   public EntityArrowData() {
      super(new HashSet<>(Collections.singletonList(Arrow.class)), new Color(255, 0, 0));
   }

   @Override
   public float getData1() {
      return 0.25F;
   }

   @Override
   public float getData2() {
      return 0.5F;
   }

   @Override
   public float getGravity() {
      return 0.05F;
   }
}
