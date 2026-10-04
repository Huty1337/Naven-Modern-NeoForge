package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StemBlock;

public class InventoryUtils {
   private static final Minecraft mc = Minecraft.getInstance();

   public static boolean isSword(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ItemTags.SWORDS);
   }

   public static boolean isPickaxe(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ItemTags.PICKAXES);
   }

   public static boolean isAxe(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ItemTags.AXES);
   }

   public static boolean isShovel(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ItemTags.SHOVELS);
   }

   public static EquipmentSlot getEquipmentSlot(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return null;
      } else {
         Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
         return equippable == null ? null : equippable.slot();
      }
   }

   public static boolean isArmor(ItemStack stack) {
      EquipmentSlot slot = getEquipmentSlot(stack);
      return slot != null && slot.isArmor() && getArmorValue(stack) > 0.0;
   }

   public static double getArmorValue(ItemStack stack) {
      EquipmentSlot slot = getEquipmentSlot(stack);
      if (slot == null || !slot.isArmor()) {
         return 0.0;
      } else {
         return getAttributeValue(stack, Attributes.ARMOR, slot);
      }
   }

   public static double getAttributeValue(ItemStack stack, Holder<Attribute> attribute, EquipmentSlot slot) {
      if (stack == null || stack.isEmpty() || slot == null) {
         return 0.0;
      } else {
         ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
         double total = 0.0;

         for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.slot().test(slot)
               && entry.attribute().is(attribute)
               && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
               total += entry.modifier().amount();
            }
         }

         return total;
      }
   }

   public static boolean isPlantableSeed(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (stack.getItem() instanceof BlockItem blockItem) {
         Block block = blockItem.getBlock();
         return block instanceof CropBlock || block instanceof StemBlock || block instanceof NetherWartBlock;
      } else {
         return false;
      }
   }

   public static Holder<Enchantment> getEnchantment(ResourceKey<Enchantment> enchantment) {
      if (mc.level == null) {
         return null;
      } else {
         return mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantment).orElse(null);
      }
   }

   public static int getEnchantmentLevel(ResourceKey<Enchantment> enchantment, ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return 0;
      } else {
         Holder<Enchantment> holder = getEnchantment(enchantment);
         return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
      }
   }

   public static float getSharpnessDamageBonus(int level) {
      if (level <= 0) {
         return 0.0F;
      } else {
         Holder<Enchantment> holder = getEnchantment(Enchantments.SHARPNESS);
         if (holder == null) {
            return 0.5F * (float)level + 0.5F;
         } else {
            float bonus = 0.0F;

            for (ConditionalEffect<EnchantmentValueEffect> effect : holder.value().getEffects(EnchantmentEffectComponents.DAMAGE)) {
               bonus = effect.effect().process(level, RandomSource.create(0L), bonus);
            }

            return bonus;
         }
      }
   }

   public static boolean shouldDisableFeatures() {
      return getAllItems().stream().anyMatch(item -> {
         if (item.isEmpty()) {
            return false;
         } else {
            String string = item.getDisplayName().getString();
            return string.contains("长按点击") || string.contains("点击使用") || string.contains("离开游戏") || string.contains("选择一个队伍") || string.contains("再来一局");
         }
      });
   }

   public static boolean isGoldenHead(ItemStack e) {
      if (e.isEmpty()) {
         return false;
      } else {
         if (e.getItem() instanceof BlockItem) {
            BlockItem item = (BlockItem)e.getItem();
            if (item.getBlock() instanceof SkullBlock) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean isSharpnessAxe(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (!isAxe(stack)) {
         return false;
      } else {
         int itemEnchantmentLevel = getEnchantmentLevel(Enchantments.SHARPNESS, stack);
         return itemEnchantmentLevel >= 8 && itemEnchantmentLevel < 50;
      }
   }

   public static boolean isGodAxe(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() != Items.GOLDEN_AXE ? false : getEnchantmentLevel(Enchantments.SHARPNESS, stack) > 100;
      }
   }

   public static boolean isEnchantedGApple(ItemStack stack) {
      return stack.isEmpty() ? false : stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
   }

   public static boolean isEndCrystal(ItemStack stack) {
      return stack.isEmpty() ? false : stack.getItem() == Items.END_CRYSTAL;
   }

   public static boolean isKBBall(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() != Items.SLIME_BALL ? false : getEnchantmentLevel(Enchantments.KNOCKBACK, stack) > 1;
      }
   }

   public static boolean isKBStick(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() != Items.STICK ? false : getEnchantmentLevel(Enchantments.KNOCKBACK, stack) > 1;
      }
   }

   public static int findEmptyInventory() {
      for (int i = 9; i < Inventory.INVENTORY_SIZE; i++) {
         if (mc.player.getInventory().getItem(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public static int findEmptySlot() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public static int getPunchLevel(ItemStack stack) {
      return getEnchantmentLevel(Enchantments.PUNCH, stack);
   }

   public static int getPowerLevel(ItemStack stack) {
      return getEnchantmentLevel(Enchantments.POWER, stack);
   }

   public static List<ItemStack> getAllItems() {
      ArrayList<ItemStack> list = new ArrayList<>(40);
      if (mc.player == null) {
         return list;
      } else {
         Inventory inventory = mc.player.getInventory();

         for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            list.add(inventory.getItem(i));
         }

         list.add(mc.player.getItemBySlot(EquipmentSlot.FEET));
         list.add(mc.player.getItemBySlot(EquipmentSlot.LEGS));
         list.add(mc.player.getItemBySlot(EquipmentSlot.CHEST));
         list.add(mc.player.getItemBySlot(EquipmentSlot.HEAD));
         return list;
      }
   }

   public static float getBestArmorScore(EquipmentSlot slot) {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isArmor(item) && getEquipmentSlot(item) == slot)
         .map(InventoryUtils::getProtection)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static float getCurrentArmorScore(EquipmentSlot slot) {
      if (slot != null && slot.isArmor() && mc.player != null) {
         return getProtection(mc.player.getItemBySlot(slot));
      } else {
         return 0.0F;
      }
   }

   public static float getBestSwordDamage() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isSword(item))
         .map(InventoryUtils::getSwordDamage)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestSword() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isSword(item))
         .max(Comparator.comparingInt(s -> (int)(getSwordDamage(s) * 100.0F)))
         .orElse(null);
   }

   public static int getItemStackSlot(ItemStack stack) {
      if (stack == null) {
         return -1;
      } else {
         for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (mc.player.getInventory().getItem(i) == stack) {
               return i;
            }
         }

         return -1;
      }
   }

   public static boolean isItemValid(ItemStack s) {
      if (!s.isEmpty()) {
         if (s.getItem() instanceof PlayerHeadItem) {
            return false;
         }

         String string = s.getDisplayName().getString();
         if (string.contains("Click")) {
            return false;
         }

         if (string.contains("Right")) {
            return false;
         }

         if (string.contains("点击")) {
            return false;
         }

         if (string.contains("Teleport")) {
            return false;
         }

         if (string.contains("使用")) {
            return false;
         }

         if (string.contains("传送")) {
            return false;
         }

         if (string.contains("再来")) {
            return false;
         }
      }

      return true;
   }

   public static int getItemSlot(Item item) {
      for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (itemStack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   public static ItemStack getBestProjectile() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL) && isItemValid(item))
         .max(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static ItemStack getFishingRod() {
      return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof FishingRodItem && isItemValid(item)).findAny().orElse(null);
   }

   public static int getBlockCountInInventory() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item))
         .mapToInt(ItemStack::getCount)
         .sum();
   }

   public static ItemStack getWorstProjectile() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL))
         .min(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static ItemStack getWorstArrow() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof ArrowItem && isItemValid(item))
         .min(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static ItemStack getWorstBlock() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item))
         .min(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static ItemStack getBestBlock() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && Scaffold.isValidStack(item) && isItemValid(item))
         .max(Comparator.comparingInt(ItemStack::getCount))
         .orElse(null);
   }

   public static float getBestPickaxeScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isPickaxe(item) && isItemValid(item))
         .map(InventoryUtils::getToolScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestPickaxe() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isPickaxe(item) && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getToolScore(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestAxeScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isAxe(item) && !isSharpnessAxe(item) && isItemValid(item))
         .map(InventoryUtils::getToolScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestAxe() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isAxe(item) && !isSharpnessAxe(item) && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getToolScore(s) * 100.0F)))
         .orElse(null);
   }

   public static ItemStack getBestShapeAxe() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isAxe(item) && isSharpnessAxe(item) && isItemValid(item) && !isGodAxe(item))
         .max(Comparator.comparingInt(s -> (int)(getAxeDamage(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestShovelScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isShovel(item) && isItemValid(item))
         .map(InventoryUtils::getToolScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestShovel() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && isShovel(item) && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getToolScore(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestCrossbowScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && isItemValid(item))
         .map(InventoryUtils::getCrossbowScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestCrossbow() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getCrossbowScore(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestPunchBowScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
         .map(InventoryUtils::getPunchBowScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestPunchBow() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getPunchBowScore(s) * 100.0F)))
         .orElse(null);
   }

   public static float getBestPowerBowScore() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
         .map(InventoryUtils::getPowerBowScore)
         .max(Float::compareTo)
         .orElse(0.0F);
   }

   public static ItemStack getBestPowerBow() {
      return getAllItems()
         .stream()
         .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
         .max(Comparator.comparingInt(s -> (int)(getPowerBowScore(s) * 100.0F)))
         .orElse(null);
   }

   public static boolean isPunchBow(ItemStack stack) {
      return getPunchBowScore(stack) > 10.0F && isItemValid(stack);
   }

   public static boolean isPowerBow(ItemStack stack) {
      return getPowerBowScore(stack) > 10.0F && isItemValid(stack);
   }

   public static boolean hasItem(Item checkItem) {
      return getAllItems().stream().anyMatch(item -> !item.isEmpty() && item.getItem() == checkItem);
   }

   public static int getItemCount(Item checkItem) {
      return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() == checkItem).mapToInt(ItemStack::getCount).sum();
   }

   public static float getPunchBowScore(ItemStack stack) {
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (stack.getItem() instanceof BowItem) {
         float valence = 10.0F;
         valence += (float)getEnchantmentLevel(Enchantments.PUNCH, stack);
         valence += (float)getEnchantmentLevel(Enchantments.INFINITY, stack);
         valence += (float)getEnchantmentLevel(Enchantments.FLAME, stack);
         valence += (float)getEnchantmentLevel(Enchantments.POWER, stack) / 10.0F;
         return valence + (float)stack.getDamageValue() / (float)stack.getMaxDamage();
      } else {
         return 0.0F;
      }
   }

   public static float getPowerBowScore(ItemStack stack) {
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (stack.getItem() instanceof BowItem) {
         float valence = 10.0F;
         valence += (float)getEnchantmentLevel(Enchantments.PUNCH, stack) / 10.0F;
         valence += (float)getEnchantmentLevel(Enchantments.INFINITY, stack);
         valence += (float)getEnchantmentLevel(Enchantments.FLAME, stack);
         valence += (float)getEnchantmentLevel(Enchantments.POWER, stack);
         return valence + (float)stack.getDamageValue() / (float)stack.getMaxDamage();
      } else {
         return 0.0F;
      }
   }

   public static float getToolScore(ItemStack stack) {
      float valence = 0.0F;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else if (isGodItem(stack)) {
         return 0.0F;
      } else if (isSharpnessAxe(stack)) {
         return 0.0F;
      } else {
         if (isPickaxe(stack)) {
            valence += stack.getDestroySpeed(Blocks.STONE.defaultBlockState());
         } else if (isAxe(stack)) {
            valence += stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState());
         } else {
            if (!isShovel(stack)) {
               return 0.0F;
            }

            valence += stack.getDestroySpeed(Blocks.DIRT.defaultBlockState());
         }

         int efficiency = getEnchantmentLevel(Enchantments.EFFICIENCY, stack);
         if (efficiency > 0) {
            valence += (float)efficiency * 0.0075F;
         }

         return valence;
      }
   }

   public static float getAxeDamage(ItemStack stack) {
      float valence = 0.0F;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else {
         if (isAxe(stack) && isSharpnessAxe(stack)) {
            Item axe = stack.getItem();
            if (axe == Items.WOODEN_AXE) {
               valence += 4.0F;
            } else if (axe == Items.STONE_AXE) {
               valence += 5.0F;
            } else if (axe == Items.IRON_AXE) {
               valence += 6.0F;
            } else if (axe == Items.GOLDEN_AXE) {
               valence += 4.0F;
            } else if (axe == Items.DIAMOND_AXE) {
               valence += 7.0F;
            }
         }

         int itemEnchantmentLevel = getEnchantmentLevel(Enchantments.SHARPNESS, stack);
         if (itemEnchantmentLevel > 0) {
            valence += getSharpnessDamageBonus(itemEnchantmentLevel);
         }

         return valence;
      }
   }

   public static float getSwordDamage(ItemStack stack) {
      float valence = 0.0F;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else {
         if (isSword(stack)) {
            valence += (float)getAttributeValue(stack, Attributes.ATTACK_DAMAGE, EquipmentSlot.MAINHAND) + 1.0F;
         }

         int itemEnchantmentLevel = getEnchantmentLevel(Enchantments.SHARPNESS, stack);
         if (itemEnchantmentLevel > 0) {
            valence += getSharpnessDamageBonus(itemEnchantmentLevel);
         }

         return valence;
      }
   }

   public static float getProtection(ItemStack itemStack) {
      int valence = 0;
      if (itemStack == null) {
         return 0.0F;
      } else if (itemStack.isEmpty()) {
         return 0.0F;
      } else {
         if (isArmor(itemStack)) {
            valence += (int)(getArmorValue(itemStack) * 100.0);
            valence += (int)(getAttributeValue(itemStack, Attributes.ARMOR_TOUGHNESS, getEquipmentSlot(itemStack)) * 10.0);
         }

         valence += getEnchantmentLevel(Enchantments.PROTECTION, itemStack);
         return (float)valence;
      }
   }

   public static float getCrossbowScore(ItemStack stack) {
      int valence = 0;
      if (stack == null) {
         return 0.0F;
      } else if (stack.isEmpty()) {
         return 0.0F;
      } else {
         if (stack.getItem() instanceof CrossbowItem) {
            valence += getEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
            valence += getEnchantmentLevel(Enchantments.MULTISHOT, stack);
            valence += getEnchantmentLevel(Enchantments.PIERCING, stack);
         }

         return (float)valence;
      }
   }

   public static boolean isGodItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (isAxe(stack)
         && stack.getItem() == Items.GOLDEN_AXE
         && getEnchantmentLevel(Enchantments.SHARPNESS, stack) > 100) {
         return true;
      } else if (stack.getItem() == Items.SLIME_BALL && getEnchantmentLevel(Enchantments.KNOCKBACK, stack) > 1) {
         return true;
      } else {
         return stack.getItem() == Items.TOTEM_OF_UNDYING ? true : stack.getItem() == Items.END_CRYSTAL;
      }
   }

   public static boolean isCommonItemUseful(ItemStack stack) {
      if (stack.isEmpty()) {
         return true;
      } else {
         Item item = stack.getItem();
         if (item instanceof BlockItem block) {
            if (block.getBlock() == Blocks.ENCHANTING_TABLE) {
               return false;
            }

            if (block.getBlock() == Blocks.COBWEB) {
               return false;
            }
         } else {
            if (item == Items.BOOK) {
               return false;
            }

            if (item instanceof ExperienceBottleItem) {
               return false;
            }

            if (item instanceof FireworkRocketItem) {
               return false;
            }

            if (item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS || item == Items.MELON_SEEDS || item == Items.PUMPKIN_SEEDS) {
               return false;
            }

            if (item == Items.FLINT_AND_STEEL) {
               return false;
            }
         }

         return true;
      }
   }
}
