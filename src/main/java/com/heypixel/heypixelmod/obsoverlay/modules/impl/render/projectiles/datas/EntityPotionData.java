package com.heypixel.heypixelmod.obsoverlay.modules.impl.render.projectiles.datas;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import net.minecraft.world.entity.projectile.AbstractThrownPotion;

public class EntityPotionData extends BasicProjectileData {
   public EntityPotionData() {
      super(new HashSet<>(Collections.singleton(AbstractThrownPotion.class)), new Color(255, 66, 249));
   }

   @Override
   public float getGravity() {
      return 0.05F;
   }
}
