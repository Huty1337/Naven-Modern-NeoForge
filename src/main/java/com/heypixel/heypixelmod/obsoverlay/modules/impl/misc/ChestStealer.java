package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TickTimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(
   name = "ChestStealer",
   description = "Automatically steals items from chests",
   category = Category.MISC
)
public class ChestStealer extends Module {
   private static final TickTimeHelper timer = new TickTimeHelper();
   private final FloatValue delay = ValueBuilder.create(this, "Delay (Ticks)")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   private final BooleanValue pickEnderChest = ValueBuilder.create(this, "Ender Chest").setDefaultBooleanValue(false).build().getBooleanValue();
   private Screen lastTickScreen;

   public static boolean isWorking() {
      return !timer.delay(3);
   }

   @EventTarget(1)
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         Screen currentScreen = mc.screen;
         if (currentScreen instanceof ContainerScreen container) {
            ChestMenu menu = (ChestMenu)container.getMenu();
            if (currentScreen != this.lastTickScreen) {
               timer.reset();
            } else {
               String chestTitle = container.getTitle().getString();
               String chest = Component.translatable("container.chest").getString();
               String largeChest = Component.translatable("container.chestDouble").getString();
               String enderChest = Component.translatable("container.enderchest").getString();
               if (chestTitle.equals(chest)
                  || chestTitle.equals(largeChest)
                  || chestTitle.equals("Chest")
                  || this.pickEnderChest.getCurrentValue() && chestTitle.equals(enderChest)) {
                  if (this.isChestEmpty(menu) && timer.delay(this.delay.getCurrentValue())) {
                     mc.player.closeContainer();
                  } else {
                     List<Integer> slots = IntStream.range(0, menu.getRowCount() * 9).boxed().collect(Collectors.toList());
                     Collections.shuffle(slots);

                     for (Integer pSlotId : slots) {
                        ItemStack stack = menu.getSlot(pSlotId).getItem();
                        if (isItemUseful(stack) && this.isBestItemInChest(menu, stack) && timer.delay(this.delay.getCurrentValue())) {
                           mc.gameMode.handleInventoryMouseClick(menu.containerId, pSlotId, 0, ClickType.QUICK_MOVE, mc.player);
                           timer.reset();
                           break;
                        }
                     }
                  }
               }
            }
         }

         this.lastTickScreen = currentScreen;
      }
   }

   private boolean isBestItemInChest(ChestMenu menu, ItemStack stack) {
      if (!InventoryUtils.isGodItem(stack) && !InventoryUtils.isSharpnessAxe(stack)) {
         for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack checkStack = menu.getSlot(i).getItem();
            if (InventoryUtils.isArmor(stack) && InventoryUtils.isArmor(checkStack)) {
               EquipmentSlot slot = InventoryUtils.getEquipmentSlot(stack);
               EquipmentSlot checkSlot = InventoryUtils.getEquipmentSlot(checkStack);
               if (slot == checkSlot && InventoryUtils.getProtection(checkStack) > InventoryUtils.getProtection(stack)) {
                  return false;
               }
            } else if (InventoryUtils.isSword(stack) && InventoryUtils.isSword(checkStack)) {
               if (InventoryUtils.getSwordDamage(checkStack) > InventoryUtils.getSwordDamage(stack)) {
                  return false;
               }
            } else if (InventoryUtils.isPickaxe(stack) && InventoryUtils.isPickaxe(checkStack)) {
               if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                  return false;
               }
            } else if (InventoryUtils.isAxe(stack) && InventoryUtils.isAxe(checkStack)) {
               if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                  return false;
               }
            } else if (InventoryUtils.isShovel(stack)
               && InventoryUtils.isShovel(checkStack)
               && InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private boolean isChestEmpty(ChestMenu menu) {
      for (int i = 0; i < menu.getRowCount() * 9; i++) {
         ItemStack item = menu.getSlot(i).getItem();
         if (!item.isEmpty() && isItemUseful(item) && this.isBestItemInChest(menu, item)) {
            return false;
         }
      }

      return true;
   }

   public static boolean isItemUseful(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (InventoryUtils.isGodItem(stack) || InventoryUtils.isSharpnessAxe(stack)) {
         return true;
      } else if (InventoryUtils.isArmor(stack)) {
         EquipmentSlot slot = InventoryUtils.getEquipmentSlot(stack);
         float protection = InventoryUtils.getProtection(stack);
         float bestArmor = InventoryUtils.getBestArmorScore(slot);
         return !(protection <= bestArmor);
      } else if (InventoryUtils.isSword(stack)) {
         float damage = InventoryUtils.getSwordDamage(stack);
         float bestDamage = InventoryUtils.getBestSwordDamage();
         return !(damage <= bestDamage);
      } else if (InventoryUtils.isPickaxe(stack)) {
         float score = InventoryUtils.getToolScore(stack);
         float bestScore = InventoryUtils.getBestPickaxeScore();
         return !(score <= bestScore);
      } else if (InventoryUtils.isAxe(stack)) {
         float score = InventoryUtils.getToolScore(stack);
         float bestScore = InventoryUtils.getBestAxeScore();
         return !(score <= bestScore);
      } else if (InventoryUtils.isShovel(stack)) {
         float score = InventoryUtils.getToolScore(stack);
         float bestScore = InventoryUtils.getBestShovelScore();
         return !(score <= bestScore);
      } else if (stack.getItem() instanceof CrossbowItem) {
         float score = InventoryUtils.getCrossbowScore(stack);
         float bestScore = InventoryUtils.getBestCrossbowScore();
         return !(score <= bestScore);
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPunchBow(stack)) {
         float score = InventoryUtils.getPunchBowScore(stack);
         float bestScore = InventoryUtils.getBestPunchBowScore();
         return !(score <= bestScore);
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPowerBow(stack)) {
         float score = InventoryUtils.getPowerBowScore(stack);
         float bestScore = InventoryUtils.getBestPowerBowScore();
         return !(score <= bestScore);
      } else if (stack.getItem() == Items.COMPASS) {
         return !InventoryUtils.hasItem(stack.getItem());
      } else if (stack.getItem() == Items.WATER_BUCKET && InventoryUtils.getItemCount(Items.WATER_BUCKET) >= InventoryCleaner.getWaterBucketCount()) {
         return false;
      } else if (stack.getItem() == Items.LAVA_BUCKET && InventoryUtils.getItemCount(Items.LAVA_BUCKET) >= InventoryCleaner.getLavaBucketCount()) {
         return false;
      } else if (stack.getItem() instanceof BlockItem
         && Scaffold.isValidStack(stack)
         && InventoryUtils.getBlockCountInInventory() + stack.getCount() >= InventoryCleaner.getMaxBlockSize()) {
         return false;
      } else if (stack.getItem() == Items.ARROW && InventoryUtils.getItemCount(Items.ARROW) + stack.getCount() >= InventoryCleaner.getMaxArrowSize()) {
         return false;
      } else if (stack.getItem() instanceof FishingRodItem && InventoryUtils.getItemCount(Items.FISHING_ROD) >= 1) {
         return false;
      } else if (stack.getItem() != Items.SNOWBALL && stack.getItem() != Items.EGG
         || InventoryUtils.getItemCount(Items.SNOWBALL) + InventoryUtils.getItemCount(Items.EGG) + stack.getCount() < InventoryCleaner.getMaxProjectileSize()
            && InventoryCleaner.shouldKeepProjectile()) {
         return InventoryUtils.isPlantableSeed(stack) ? false : InventoryUtils.isCommonItemUseful(stack);
      } else {
         return false;
      }
   }
}
