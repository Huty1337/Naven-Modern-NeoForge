package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateHeldItem;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

@ModuleInfo(
   name = "AutoTools",
   description = "Automatically switches to the best tool for the job",
   category = Category.MISC
)
public class AutoTools extends Module {
   private final BooleanValue checkSword = ValueBuilder.create(this, "Check Sword").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue switchBack = ValueBuilder.create(this, "Switch Back").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue silent = ValueBuilder.create(this, "Silent")
      .setVisibility(this.switchBack::getCurrentValue)
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   private int originSlot = -1;

   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      if (this.switchBack.getCurrentValue() && this.silent.getCurrentValue() && e.getHand() == InteractionHand.MAIN_HAND && this.originSlot != -1) {
         e.setItem(mc.player.getInventory().getItem(this.originSlot));
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (mc.gameMode.isDestroying()) {
            if (this.checkSword.getCurrentValue()) {
               ItemStack itemStack = mc.player.getMainHandItem();
               if (InventoryUtils.isSword(itemStack)) {
                  return;
               }
            }

            if (mc.hitResult.getType() == Type.BLOCK) {
               BlockHitResult hitResult = (BlockHitResult)mc.hitResult;
               int bestTool = this.getBestTool(hitResult.getBlockPos());
               if (bestTool != -1 && bestTool != mc.player.getInventory().getSelectedSlot()) {
                  this.originSlot = mc.player.getInventory().getSelectedSlot();
                  mc.player.getInventory().setSelectedSlot(bestTool);
               }
            }
         }
      } else if (!mc.gameMode.isDestroying() && this.switchBack.getCurrentValue() && this.originSlot != -1) {
         mc.player.getInventory().setSelectedSlot(this.originSlot);
         this.originSlot = -1;
      }
   }

   private int getBestTool(BlockPos pos) {
      BlockState blockState = mc.level.getBlockState(pos);
      Block block = blockState.getBlock();
      int slot = 0;
      float dmg = 1.0F;

      for (int index = 0; index < 9; index++) {
         ItemStack itemStack = mc.player.getInventory().getItem(index);
         if (!InventoryUtils.isGodItem(itemStack)
            && !itemStack.isEmpty()
            && !blockState.isAir()
            && (!InventoryUtils.isSword(itemStack) || block instanceof WebBlock)) {
            float strVsBlock = itemStack.getItem().getDestroySpeed(itemStack, blockState);
            if (strVsBlock > 1.0F && !(block instanceof DropExperienceBlock) && !(block instanceof RedStoneOreBlock)) {
               int i = InventoryUtils.getEnchantmentLevel(Enchantments.EFFICIENCY, itemStack);
               if (i > 0) {
                  strVsBlock += (float)(i * i + 1);
               }
            }

            if (strVsBlock > dmg) {
               slot = index;
               dmg = strVsBlock;
            }
         }
      }

      return dmg > 1.0F ? slot : -1;
   }
}
