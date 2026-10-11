package tech.naven.modules.impl.misc;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventMotion;
import tech.naven.events.impl.EventPacket;
import tech.naven.events.impl.EventSprint;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.combat.Aura;
import tech.naven.modules.impl.move.Scaffold;
import tech.naven.ui.ClickGUI;
import tech.naven.ui.notification.Notification;
import tech.naven.ui.notification.NotificationLevel;
import tech.naven.utils.InventoryUtils;
import tech.naven.utils.MoveUtils;
import tech.naven.utils.NetworkUtils;
import tech.naven.utils.TimeHelper;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;
import tech.naven.values.impl.ModeValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;

@ModuleInfo(
   name = "InventoryManager",
   description = "Automatically manage your inventory",
   category = Category.MISC
)
public class InventoryCleaner extends Module {
   private static final TimeHelper timer = new TimeHelper();
   private int sprintStopCooldown = 0;
   private boolean lastSprintState = false;
   private int actionCooldown = 999;
   private final FloatValue delay = ValueBuilder.create(this, "Delay (ms)")
      .setDefaultFloatValue(110.0F)
      .setFloatStep(10.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(500.0F)
      .build()
      .getFloatValue();
   ModeValue offhandItems = ValueBuilder.create(this, "Offhand Items")
      .setModes("None", "Golden Apple", "Projectile", "Fishing Rod", "Block")
      .build()
      .getModeValue();
   BooleanValue autoArmor = ValueBuilder.create(this, "Auto Armor").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue inventoryOnly = ValueBuilder.create(this, "Inventory Only").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue pauseOnKillAura = ValueBuilder.create(this, "Pause On KillAura").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue switchSword = ValueBuilder.create(this, "Switch Sword").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue swordSlot = ValueBuilder.create(this, "Sword Slot")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchSword.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchBlock = ValueBuilder.create(this, "Switch Block")
      .setVisibility(() -> !this.offhandItems.isCurrentMode("Block"))
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   FloatValue blockSlot = ValueBuilder.create(this, "Block Slot")
      .setDefaultFloatValue(2.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchBlock.getCurrentValue() && !this.offhandItems.isCurrentMode("Block"))
      .build()
      .getFloatValue();
   FloatValue maxBlockSize = ValueBuilder.create(this, "Max Block Size")
      .setDefaultFloatValue(256.0F)
      .setFloatStep(64.0F)
      .setMinFloatValue(64.0F)
      .setMaxFloatValue(512.0F)
      .setVisibility(() -> this.switchBlock.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchPickaxe = ValueBuilder.create(this, "Switch Pickaxe").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue pickaxeSlot = ValueBuilder.create(this, "Pickaxe Slot")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchPickaxe.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchAxe = ValueBuilder.create(this, "Switch Axe").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue axeSlot = ValueBuilder.create(this, "Axe Slot")
      .setDefaultFloatValue(4.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchAxe.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchBow = ValueBuilder.create(this, "Switch Bow or Crossbow").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue bowSlot = ValueBuilder.create(this, "Bow Slot")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchBow.getCurrentValue())
      .build()
      .getFloatValue();
   ModeValue preferBow = ValueBuilder.create(this, "Bow Priority")
      .setModes("Crossbow", "Power Bow", "Punch Bow")
      .setVisibility(() -> this.switchBow.getCurrentValue())
      .build()
      .getModeValue();
   FloatValue maxArrowSize = ValueBuilder.create(this, "Max Arrow Size")
      .setDefaultFloatValue(256.0F)
      .setFloatStep(64.0F)
      .setMinFloatValue(64.0F)
      .setMaxFloatValue(512.0F)
      .setVisibility(() -> this.switchBow.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchWaterBucket = ValueBuilder.create(this, "Switch Water Bucket").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue waterBucketSlot = ValueBuilder.create(this, "Water Bucket Slot")
      .setDefaultFloatValue(6.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchWaterBucket.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchEnderPearl = ValueBuilder.create(this, "Switch Ender Pearl").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue enderPearlSlot = ValueBuilder.create(this, "Ender Pearl Slot")
      .setDefaultFloatValue(7.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchEnderPearl.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchFireball = ValueBuilder.create(this, "Switch Fireball").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue fireballSlot = ValueBuilder.create(this, "Fireball Slot")
      .setDefaultFloatValue(8.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchFireball.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchGoldenApple = ValueBuilder.create(this, "Switch Golden Apple")
      .setVisibility(() -> !this.offhandItems.isCurrentMode("Golden Apple"))
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   FloatValue goldenAppleSlot = ValueBuilder.create(this, "Golden Apple Slot")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchGoldenApple.getCurrentValue() && !this.offhandItems.isCurrentMode("Golden Apple"))
      .build()
      .getFloatValue();
   BooleanValue throwItems = ValueBuilder.create(this, "Throw Items").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue waterBucketCount = ValueBuilder.create(this, "Keep Water Buckets")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> this.throwItems.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue lavaBucketCount = ValueBuilder.create(this, "Keep Lava Buckets")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> this.throwItems.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue keepProjectile = ValueBuilder.create(this, "Keep Eggs & Snowballs").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue switchProjectile = ValueBuilder.create(this, "Switch Eggs & Snowballs")
      .setDefaultBooleanValue(false)
      .setVisibility(() -> this.keepProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile"))
      .build()
      .getBooleanValue();
   FloatValue projectileSlot = ValueBuilder.create(this, "Eggs & Snowballs Slot")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchProjectile.getCurrentValue() && this.keepProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile"))
      .build()
      .getFloatValue();
   FloatValue maxProjectileSize = ValueBuilder.create(this, "Max Eggs & Snowballs Size")
      .setDefaultFloatValue(64.0F)
      .setFloatStep(16.0F)
      .setMinFloatValue(16.0F)
      .setMaxFloatValue(256.0F)
      .setVisibility(() -> this.keepProjectile.getCurrentValue())
      .build()
      .getFloatValue();
   BooleanValue switchRod = ValueBuilder.create(this, "Switch Rod")
      .setVisibility(() -> !this.offhandItems.isCurrentMode("Fishing Rod"))
      .setDefaultBooleanValue(false)
      .build()
      .getBooleanValue();
   FloatValue rodSlot = ValueBuilder.create(this, "Rod Slot")
      .setDefaultFloatValue(9.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(9.0F)
      .setVisibility(() -> this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("Fishing Rod"))
      .build()
      .getFloatValue();
   int noMoveTicks = 0;
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
      EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
   };
   private boolean clickOffHand = false;
   private boolean inventoryOpen = false;

   public static int getMaxBlockSize() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).maxBlockSize.getCurrentValue();
   }

   public static boolean shouldKeepProjectile() {
      return ((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).keepProjectile.getCurrentValue();
   }

   public static int getMaxProjectileSize() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).maxProjectileSize.getCurrentValue();
   }

   public static int getMaxArrowSize() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).maxArrowSize.getCurrentValue();
   }

   public static int getWaterBucketCount() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).waterBucketCount.getCurrentValue();
   }

   public static int getLavaBucketCount() {
      return (int)((InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class)).lavaBucketCount.getCurrentValue();
   }

   public boolean isItemUseful(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (InventoryUtils.isGodItem(stack)) {
         return true;
      } else if (stack.getDisplayName().getString().contains("点击使用")) {
         return true;
      } else if (InventoryUtils.isArmor(stack)) {
         EquipmentSlot armorSlot = InventoryUtils.getEquipmentSlot(stack);
         float protection = InventoryUtils.getProtection(stack);
         if (InventoryUtils.getCurrentArmorScore(armorSlot) >= protection) {
            return false;
         } else {
            float bestArmor = InventoryUtils.getBestArmorScore(armorSlot);
            return !(protection < bestArmor);
         }
      } else if (InventoryUtils.isSword(stack)) {
         return InventoryUtils.getBestSword() == stack;
      } else if (InventoryUtils.isPickaxe(stack)) {
         return InventoryUtils.getBestPickaxe() == stack;
      } else if (InventoryUtils.isAxe(stack) && !InventoryUtils.isSharpnessAxe(stack)) {
         return InventoryUtils.getBestAxe() == stack;
      } else if (InventoryUtils.isShovel(stack)) {
         return InventoryUtils.getBestShovel() == stack;
      } else if (stack.getItem() instanceof CrossbowItem) {
         return InventoryUtils.getBestCrossbow() == stack;
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPunchBow(stack)) {
         return InventoryUtils.getBestPunchBow() == stack;
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPowerBow(stack)) {
         return InventoryUtils.getBestPowerBow() == stack;
      } else if (stack.getItem() instanceof BowItem && InventoryUtils.getItemCount(Items.BOW) > 1) {
         return false;
      } else if (stack.getItem() == Items.WATER_BUCKET && InventoryUtils.getItemCount(Items.WATER_BUCKET) > getWaterBucketCount()) {
         return false;
      } else if (stack.getItem() == Items.LAVA_BUCKET && InventoryUtils.getItemCount(Items.LAVA_BUCKET) > getLavaBucketCount()) {
         return false;
      } else if (stack.getItem() instanceof FishingRodItem && InventoryUtils.getItemCount(Items.FISHING_ROD) > 1) {
         return false;
      } else if ((stack.getItem() == Items.SNOWBALL || stack.getItem() == Items.EGG) && !shouldKeepProjectile()) {
         return false;
      } else {
         return InventoryUtils.isPlantableSeed(stack) ? false : InventoryUtils.isCommonItemUseful(stack);
      }
   }

   @EventTarget
   public void onPacket(EventPacket e) {
      if (e.getType() == EventType.SEND) {
         if (e.getPacket() instanceof ServerboundContainerClosePacket) {
            this.inventoryOpen = false;
         }

         if (this.inventoryOpen && !this.inventoryOnly.getCurrentValue()) {
            if (e.getPacket() instanceof ServerboundMovePlayerPacket) {
               if (MoveUtils.isMoving()) {
                  mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
               }
            } else if (e.getPacket() instanceof ServerboundUseItemOnPacket
               || e.getPacket() instanceof ServerboundUseItemPacket
               || e.getPacket() instanceof ServerboundInteractPacket
               || e.getPacket() instanceof ServerboundPlayerActionPacket) {
               mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
            }
         }
      }
   }

   private boolean checkConfig() {
      List<Pair<BooleanValue, FloatValue>> pairs = new ArrayList<>();
      if (!this.keepProjectile.getCurrentValue()) {
         this.switchProjectile.setCurrentValue(false);
      }

      pairs.add(Pair.of(this.switchSword, this.swordSlot));
      pairs.add(Pair.of(this.switchPickaxe, this.pickaxeSlot));
      pairs.add(Pair.of(this.switchAxe, this.axeSlot));
      pairs.add(Pair.of(this.switchBow, this.bowSlot));
      pairs.add(Pair.of(this.switchWaterBucket, this.waterBucketSlot));
      pairs.add(Pair.of(this.switchEnderPearl, this.enderPearlSlot));
      pairs.add(Pair.of(this.switchFireball, this.fireballSlot));
      if (!this.offhandItems.isCurrentMode("Golden Apple")) {
         pairs.add(Pair.of(this.switchGoldenApple, this.goldenAppleSlot));
      }

      if (!this.offhandItems.isCurrentMode("Projectile")) {
         pairs.add(Pair.of(this.switchProjectile, this.projectileSlot));
      }

      if (!this.offhandItems.isCurrentMode("Fishing Rod")) {
         pairs.add(Pair.of(this.switchRod, this.rodSlot));
      }

      if (!this.offhandItems.isCurrentMode("Block")) {
         pairs.add(Pair.of(this.switchBlock, this.blockSlot));
      }

      Set<Integer> usedSlot = new HashSet<>();

      for (Pair<BooleanValue, FloatValue> pair : pairs) {
         if (((BooleanValue)pair.getKey()).getCurrentValue()) {
            int targetSlot = (int)(((FloatValue)pair.getValue()).getCurrentValue() - 1.0F);
            if (usedSlot.contains(targetSlot)) {
               return false;
            }

            usedSlot.add(targetSlot);
         }
      }

      return true;
   }

   @EventTarget
   public void onSprint(EventSprint event) {
      if (this.inventoryOnly.getCurrentValue() || mc.player == null) {
         return;
      }

      boolean nowSprinting = mc.player.isSprinting();
      if (this.lastSprintState && !nowSprinting) {
         this.sprintStopCooldown = Math.max(this.sprintStopCooldown, 2);
      }

      this.lastSprintState = nowSprinting;
      if (this.isPerformingSilentAction()) {
         this.suppressSprintForInventory();
      }
   }

   public void suppressSprintForInventory() {
      if (mc.player == null) {
         return;
      }

      mc.options.keySprint.setDown(false);
      if (mc.player.isSprinting()) {
         mc.player.setSprinting(false);
         this.sprintStopCooldown = Math.max(this.sprintStopCooldown, 2);
      }
   }

   public boolean isPerformingSilentAction() {
      return this.isEnabled()
         && !this.inventoryOnly.getCurrentValue()
         && !this.shouldPauseForAction()
         && (this.hasPendingActions() || this.sprintStopCooldown > 0 || this.inventoryOpen);
   }

   public boolean shouldPauseForAction() {
      if (mc.player == null) {
         return true;
      }

      if (InventoryUtils.shouldDisableFeatures()) {
         return true;
      }

      if (ChestStealer.isWorking()) {
         return true;
      }

      if (Naven.getInstance().getModuleManager().getModule(Scaffold.class).isEnabled()) {
         return true;
      }

      if (mc.player.isUsingItem()) {
         return true;
      }

      if (this.pauseOnKillAura.getCurrentValue() && Aura.target != null) {
         return true;
      }

      return this.inventoryOnly.getCurrentValue() && !(mc.screen instanceof InventoryScreen);
   }

   public boolean hasPendingActions() {
      if (mc.player == null || mc.level == null) {
         return false;
      }

      if (this.actionCooldown < 5) {
         return true;
      }

      if (this.autoArmor.getCurrentValue()) {
         for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack equipped = mc.player.getItemBySlot(ARMOR_SLOTS[i]);
            if (InventoryUtils.isArmor(equipped)
               && !equipped.isEmpty()
               && InventoryUtils.getBestArmorScore(InventoryUtils.getEquipmentSlot(equipped)) > InventoryUtils.getProtection(equipped)) {
               return true;
            }
         }

         for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack candidate = mc.player.getInventory().getItem(i);
            if (!candidate.isEmpty() && InventoryUtils.isArmor(candidate)) {
               EquipmentSlot slot = InventoryUtils.getEquipmentSlot(candidate);
               float score = InventoryUtils.getProtection(candidate);
               if (InventoryUtils.getBestArmorScore(slot) == score && InventoryUtils.getCurrentArmorScore(slot) < score) {
                  return true;
               }
            }
         }
      }

      if (this.clickOffHand) {
         return true;
      }

      if (this.offhandItems.isCurrentMode("Golden Apple")) {
         ItemStack offHand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
         int slot = InventoryUtils.getItemSlot(Items.GOLDEN_APPLE);
         if (slot != -1) {
            if (offHand.getItem() == Items.GOLDEN_APPLE) {
               ItemStack goldenApple = mc.player.getInventory().getItem(slot);
               if (offHand.getCount() + goldenApple.getCount() <= 64) {
                  return true;
               }
            } else {
               return true;
            }
         }
      } else if (this.offhandItems.isCurrentMode("Projectile")) {
         ItemStack offHand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
         ItemStack bestProjectile = InventoryUtils.getBestProjectile();
         if (bestProjectile != null && InventoryUtils.getItemStackSlot(bestProjectile) != -1) {
            if (offHand.getItem() != Items.EGG && offHand.getItem() != Items.SNOWBALL) {
               return true;
            }

            if (offHand.getCount() < bestProjectile.getCount()) {
               return true;
            }
         }
      } else if (this.offhandItems.isCurrentMode("Fishing Rod")) {
         ItemStack offHand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
         if (InventoryUtils.getItemSlot(Items.FISHING_ROD) != -1 && offHand.getItem() != Items.FISHING_ROD) {
            return true;
         }
      } else if (this.offhandItems.isCurrentMode("Block")) {
         ItemStack offHand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
         ItemStack bestBlock = InventoryUtils.getBestBlock();
         if (bestBlock != null && InventoryUtils.getItemStackSlot(bestBlock) != -1) {
            if (!Scaffold.isValidStack(offHand) || offHand.getCount() < bestBlock.getCount()) {
               return true;
            }
         }
      }

      if (this.switchGoldenApple.getCurrentValue()
         && !this.offhandItems.isCurrentMode("Golden Apple")
         && this.needsItemSwap((int)(this.goldenAppleSlot.getCurrentValue() - 1.0F), Items.GOLDEN_APPLE)) {
         return true;
      }

      if (this.switchBlock.getCurrentValue()) {
         int blockSlot = (int)(this.blockSlot.getCurrentValue() - 1.0F);
         ItemStack currentBlock = mc.player.getInventory().getItem(blockSlot);
         ItemStack bestBlock = InventoryUtils.getBestBlock();
         if (bestBlock != null
            && (bestBlock.getCount() > currentBlock.getCount() || !Scaffold.isValidStack(currentBlock))
            && !this.offhandItems.isCurrentMode("Block")
            && InventoryUtils.isItemValid(currentBlock)
            && InventoryUtils.getItemStackSlot(bestBlock) != -1) {
            return true;
         }

         if ((float)InventoryUtils.getBlockCountInInventory() > this.maxBlockSize.getCurrentValue()) {
            ItemStack worstBlock = InventoryUtils.getWorstBlock();
            if (InventoryUtils.isItemValid(worstBlock) && InventoryUtils.getItemStackSlot(worstBlock) != -1) {
               return true;
            }
         }
      }

      if (this.switchSword.getCurrentValue()) {
         int slot = (int)(this.swordSlot.getCurrentValue() - 1.0F);
         ItemStack currentSword = mc.player.getInventory().getItem(slot);
         ItemStack bestSword = InventoryUtils.getBestSword();
         ItemStack bestShapeAxe = InventoryUtils.getBestShapeAxe();
         if (InventoryUtils.getAxeDamage(bestShapeAxe) > InventoryUtils.getSwordDamage(bestSword)) {
            bestSword = bestShapeAxe;
         }

         if (bestSword != null && InventoryUtils.isItemValid(currentSword) && InventoryUtils.getItemStackSlot(bestSword) != -1) {
            float currentDamage = InventoryUtils.isSword(currentSword)
               ? InventoryUtils.getSwordDamage(currentSword)
               : InventoryUtils.getAxeDamage(currentSword);
            float bestDamage = InventoryUtils.isSword(bestSword)
               ? InventoryUtils.getSwordDamage(bestSword)
               : InventoryUtils.getAxeDamage(bestSword);
            if (bestDamage > currentDamage) {
               return true;
            }
         }
      }

      if (this.switchPickaxe.getCurrentValue()) {
         int slot = (int)(this.pickaxeSlot.getCurrentValue() - 1.0F);
         ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
         ItemStack currentPickaxe = mc.player.getInventory().getItem(slot);
         if (bestPickaxe != null
            && InventoryUtils.isPickaxe(bestPickaxe)
            && InventoryUtils.isItemValid(currentPickaxe)
            && InventoryUtils.getItemStackSlot(bestPickaxe) != -1
            && (InventoryUtils.getToolScore(bestPickaxe) > InventoryUtils.getToolScore(currentPickaxe)
               || !InventoryUtils.isPickaxe(currentPickaxe))) {
            return true;
         }
      }

      if (this.switchAxe.getCurrentValue()) {
         int slot = (int)(this.axeSlot.getCurrentValue() - 1.0F);
         ItemStack bestAxe = InventoryUtils.getBestAxe();
         ItemStack currentAxe = mc.player.getInventory().getItem(slot);
         if (bestAxe != null
            && InventoryUtils.isAxe(bestAxe)
            && InventoryUtils.isItemValid(currentAxe)
            && InventoryUtils.getItemStackSlot(bestAxe) != -1
            && (InventoryUtils.getToolScore(bestAxe) > InventoryUtils.getToolScore(currentAxe) || !InventoryUtils.isAxe(currentAxe))) {
            return true;
         }
      }

      if (this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("Fishing Rod")) {
         int slot = (int)(this.rodSlot.getCurrentValue() - 1.0F);
         ItemStack bestRod = InventoryUtils.getFishingRod();
         ItemStack currentRod = mc.player.getInventory().getItem(slot);
         if (bestRod != null
            && InventoryUtils.isItemValid(currentRod)
            && !(currentRod.getItem() instanceof FishingRodItem)
            && InventoryUtils.getItemStackSlot(bestRod) != -1) {
            return true;
         }
      }

      if (this.switchBow.getCurrentValue()) {
         int slot = (int)(this.bowSlot.getCurrentValue() - 1.0F);
         ItemStack currentBow = mc.player.getInventory().getItem(slot);
         ItemStack bestBow;
         float bestBowScore;
         float currentBowScore;
         if (this.preferBow.isCurrentMode("Crossbow")) {
            bestBow = InventoryUtils.getBestCrossbow();
            bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
            currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
         } else if (this.preferBow.isCurrentMode("Power Bow")) {
            bestBow = InventoryUtils.getBestPowerBow();
            bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
            currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
         } else {
            bestBow = InventoryUtils.getBestPunchBow();
            bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
            currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
         }

         if (bestBow == null) {
            bestBow = InventoryUtils.getBestCrossbow();
            bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
            currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
         }

         if (bestBow == null) {
            bestBow = InventoryUtils.getBestPowerBow();
            bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
            currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
         }

         if (bestBow == null) {
            bestBow = InventoryUtils.getBestPunchBow();
            bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
            currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
         }

         if (bestBow != null
            && bestBowScore > currentBowScore
            && InventoryUtils.isItemValid(currentBow)
            && InventoryUtils.getItemStackSlot(bestBow) != -1) {
            return true;
         }

         if ((float)InventoryUtils.getItemCount(Items.ARROW) > this.maxArrowSize.getCurrentValue()) {
            ItemStack worstArrow = InventoryUtils.getWorstArrow();
            if (InventoryUtils.isItemValid(worstArrow) && InventoryUtils.getItemStackSlot(worstArrow) != -1) {
               return true;
            }
         }
      }

      if (this.switchEnderPearl.getCurrentValue() && this.needsItemSwap((int)(this.enderPearlSlot.getCurrentValue() - 1.0F), Items.ENDER_PEARL)) {
         return true;
      }

      if (this.switchWaterBucket.getCurrentValue() && this.needsItemSwap((int)(this.waterBucketSlot.getCurrentValue() - 1.0F), Items.WATER_BUCKET)) {
         return true;
      }

      if (this.switchFireball.getCurrentValue() && this.needsItemSwap((int)(this.fireballSlot.getCurrentValue() - 1.0F), Items.FIRE_CHARGE)) {
         return true;
      }

      if (this.keepProjectile.getCurrentValue()) {
         if ((float)(InventoryUtils.getItemCount(Items.EGG) + InventoryUtils.getItemCount(Items.SNOWBALL)) > this.maxProjectileSize.getCurrentValue()) {
            ItemStack worstProjectile = InventoryUtils.getWorstProjectile();
            if (InventoryUtils.isItemValid(worstProjectile) && InventoryUtils.getItemStackSlot(worstProjectile) != -1) {
               return true;
            }
         }

         if (this.switchProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile")) {
            int slot = (int)(this.projectileSlot.getCurrentValue() - 1.0F);
            if (InventoryUtils.getItemCount(Items.EGG) > 0) {
               if (this.needsItemSwap(slot, Items.EGG)) {
                  return true;
               }
            } else if (InventoryUtils.getItemCount(Items.SNOWBALL) > 0 && this.needsItemSwap(slot, Items.SNOWBALL)) {
               return true;
            }
         }
      }

      if (this.throwItems.getCurrentValue()) {
         for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty()
               && !this.isItemUseful(stack)
               && InventoryUtils.isItemValid(stack)
               && InventoryUtils.getItemStackSlot(stack) != -1) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean needsItemSwap(int targetSlot, Item item) {
      ItemStack current = mc.player.getInventory().getItem(targetSlot);
      if (!InventoryUtils.isItemValid(current)) {
         return false;
      }

      int source = InventoryUtils.getItemSlot(item);
      if (source == -1) {
         return false;
      }

      ItemStack sourceStack = mc.player.getInventory().getItem(source);
      return current.getItem() != item || current.getCount() < sourceStack.getCount();
   }

   private void click(int slotId, int button, ClickType clickType) {
      mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slotId, button, clickType, mc.player);
   }

   public static void appendSilentClose(int containerId) {
      if (mc.player == null || mc.screen != null || mc.getConnection() == null) {
         return;
      }

      InventoryCleaner inventory = (InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class);
      if (!inventory.isEnabled() || inventory.inventoryOnly.getCurrentValue()) {
         return;
      }

      NetworkUtils.sendPacketNoEvent(new ServerboundContainerClosePacket(containerId));
      inventory.inventoryOpen = false;
      inventory.actionCooldown = 0;
   }

   @EventTarget
   public void onMotionEnd(EventMotion e) {
      if (e.getType() != EventType.POST || mc.player == null) {
         return;
      }

      if (this.inventoryOpen) {
         this.inventoryOpen = false;
         this.actionCooldown = 0;
      } else {
         this.actionCooldown++;
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (!(mc.screen instanceof ClickGUI) && !this.checkConfig()) {
            Notification notification = new Notification(
               NotificationLevel.ERROR, "Duplicate slot config in Inventory Manager! Please check your config!", 8000L
            );
            Naven.getInstance().getNotificationManager().addNotification(notification);
            this.toggle();
            return;
         }

         if (InventoryUtils.shouldDisableFeatures()) {
            return;
         }

         if (MoveUtils.isMoving()) {
            this.noMoveTicks = 0;
         } else {
            this.noMoveTicks++;
         }

         if (this.shouldPauseForAction()) {
            this.clickOffHand = false;
            return;
         }

         if (mc.screen instanceof AbstractContainerScreen container && container.getMenu().containerId != mc.player.inventoryMenu.containerId) {
            return;
         }

         if (!this.inventoryOnly.getCurrentValue()) {
            if (mc.player.isSprinting()) {
               if (this.hasPendingActions()) {
                  this.suppressSprintForInventory();
               }

               return;
            }

            if (this.sprintStopCooldown > 0) {
               this.sprintStopCooldown--;
               return;
            }
         }

         if (this.autoArmor.getCurrentValue()) {
            for (int i = 0; i < ARMOR_SLOTS.length; i++) {
               ItemStack stack = mc.player.getItemBySlot(ARMOR_SLOTS[i]);
               if (InventoryUtils.isArmor(stack)) {
                  EquipmentSlot armorSlot = InventoryUtils.getEquipmentSlot(stack);
                  if (!stack.isEmpty()
                     && timer.delay(this.delay.getCurrentValue())
                     && InventoryUtils.getBestArmorScore(armorSlot) > InventoryUtils.getProtection(stack)) {
                     this.click(4 + (4 - i), 1, ClickType.THROW);
                     this.inventoryOpen = true;
                     timer.reset();
                  }
               }
            }

            for (int ix = 0; ix < Inventory.INVENTORY_SIZE; ix++) {
               ItemStack stack = mc.player.getInventory().getItem(ix);
               if (!stack.isEmpty() && InventoryUtils.isArmor(stack)) {
                  EquipmentSlot armorSlot = InventoryUtils.getEquipmentSlot(stack);
                  float currentItemScore = InventoryUtils.getProtection(stack);
                  boolean isBestItem = InventoryUtils.getBestArmorScore(armorSlot) == currentItemScore;
                  boolean isBetterItem = InventoryUtils.getCurrentArmorScore(armorSlot) < currentItemScore;
                  if (isBestItem && isBetterItem && timer.delay(this.delay.getCurrentValue())) {
                     if (ix < 9) {
                        this.click(ix + 36, 0, ClickType.QUICK_MOVE);
                     } else {
                        this.click(ix, 0, ClickType.QUICK_MOVE);
                     }

                     this.inventoryOpen = true;
                     timer.reset();
                  }
               }
            }
         }

         if (this.clickOffHand && timer.delay(this.delay.getCurrentValue())) {
            this.click(45, 0, ClickType.PICKUP);
            this.inventoryOpen = true;
            this.clickOffHand = false;
            timer.reset();
         }

         if (this.offhandItems.isCurrentMode("Golden Apple")) {
            ItemStack offHand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
            int slot = InventoryUtils.getItemSlot(Items.GOLDEN_APPLE);
            if (slot != -1 && timer.delay(this.delay.getCurrentValue())) {
               if (offHand.getItem() == Items.GOLDEN_APPLE) {
                  ItemStack goldenAppleStack = mc.player.getInventory().getItem(slot);
                  if (offHand.getCount() + goldenAppleStack.getCount() <= 64) {
                     if (slot < 9) {
                        this.click(slot + 36, 0, ClickType.PICKUP);
                     } else {
                        this.click(slot, 0, ClickType.PICKUP);
                     }

                     this.inventoryOpen = true;
                     this.clickOffHand = true;
                     timer.reset();
                  }
               } else {
                  this.swapOffHand(slot);
               }
            }
         } else if (this.offhandItems.isCurrentMode("Projectile")) {
            ItemStack offHand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
            ItemStack bestProjectile = InventoryUtils.getBestProjectile();
            if (bestProjectile != null) {
               int slot = InventoryUtils.getItemStackSlot(bestProjectile);
               boolean shouldSwap = false;
               if (offHand.getItem() != Items.EGG && offHand.getItem() != Items.SNOWBALL) {
                  shouldSwap = true;
               } else if (offHand.getCount() < bestProjectile.getCount()) {
                  shouldSwap = true;
               }

               if (shouldSwap && slot != -1 && timer.delay(this.delay.getCurrentValue())) {
                  this.swapOffHand(slot);
               }
            }
         } else if (this.offhandItems.isCurrentMode("Fishing Rod")) {
            ItemStack offHand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
            int slotx = InventoryUtils.getItemSlot(Items.FISHING_ROD);
            if (slotx != -1 && timer.delay(this.delay.getCurrentValue()) && offHand.getItem() != Items.FISHING_ROD) {
               this.swapOffHand(slotx);
            }
         } else if (this.offhandItems.isCurrentMode("Block")) {
            ItemStack offHand = mc.player.getItemBySlot(EquipmentSlot.OFFHAND);
            ItemStack bestBlock = InventoryUtils.getBestBlock();
            if (bestBlock != null) {
               int slotx = InventoryUtils.getItemStackSlot(bestBlock);
               boolean shouldSwapx = false;
               if (Scaffold.isValidStack(offHand)) {
                  if (offHand.getCount() < bestBlock.getCount()) {
                     shouldSwapx = true;
                  }
               } else {
                  shouldSwapx = true;
               }

               if (shouldSwapx && slotx != -1 && timer.delay(this.delay.getCurrentValue())) {
                  this.swapOffHand(slotx);
               }
            }
         }

         if (this.switchGoldenApple.getCurrentValue() && !this.offhandItems.isCurrentMode("Golden Apple")) {
            this.swapItem((int)(this.goldenAppleSlot.getCurrentValue() - 1.0F), Items.GOLDEN_APPLE);
         }

         if (this.switchBlock.getCurrentValue()) {
            int blockSlot = (int)(this.blockSlot.getCurrentValue() - 1.0F);
            ItemStack currentBlock = mc.player.getInventory().getItem(blockSlot);
            ItemStack bestBlock = InventoryUtils.getBestBlock();
            if (bestBlock != null
               && (bestBlock.getCount() > currentBlock.getCount() || !Scaffold.isValidStack(currentBlock))
               && !this.offhandItems.isCurrentMode("Block")) {
               this.swapItem(blockSlot, bestBlock);
            }

            if ((float)InventoryUtils.getBlockCountInInventory() > this.maxBlockSize.getCurrentValue()) {
               ItemStack worstBlock = InventoryUtils.getWorstBlock();
               this.throwItem(worstBlock);
            }
         }

         if (this.switchSword.getCurrentValue()) {
            int slotxx = (int)(this.swordSlot.getCurrentValue() - 1.0F);
            ItemStack currentSword = mc.player.getInventory().getItem(slotxx);
            ItemStack bestSword = InventoryUtils.getBestSword();
            ItemStack bestShapeAxe = InventoryUtils.getBestShapeAxe();
            if (InventoryUtils.getAxeDamage(bestShapeAxe) > InventoryUtils.getSwordDamage(bestSword)) {
               bestSword = bestShapeAxe;
            }

            if (bestSword != null) {
               float currentDamage = InventoryUtils.isSword(currentSword)
                  ? InventoryUtils.getSwordDamage(currentSword)
                  : InventoryUtils.getAxeDamage(currentSword);
               float bestWeaponDamage = InventoryUtils.isSword(bestSword)
                  ? InventoryUtils.getSwordDamage(bestSword)
                  : InventoryUtils.getAxeDamage(bestSword);
               if (bestWeaponDamage > currentDamage) {
                  this.swapItem(slotxx, bestSword);
               }
            }
         }

         if (this.switchPickaxe.getCurrentValue()) {
            int slotxxx = (int)(this.pickaxeSlot.getCurrentValue() - 1.0F);
            ItemStack bestPickaxe = InventoryUtils.getBestPickaxe();
            ItemStack currentPickaxe = mc.player.getInventory().getItem(slotxxx);
            if (bestPickaxe != null
               && InventoryUtils.isPickaxe(bestPickaxe)
               && (InventoryUtils.getToolScore(bestPickaxe) > InventoryUtils.getToolScore(currentPickaxe) || !InventoryUtils.isPickaxe(currentPickaxe))
               )
             {
               this.swapItem(slotxxx, bestPickaxe);
            }
         }

         if (this.switchAxe.getCurrentValue()) {
            int slotxxx = (int)(this.axeSlot.getCurrentValue() - 1.0F);
            ItemStack bestAxe = InventoryUtils.getBestAxe();
            ItemStack currentAxe = mc.player.getInventory().getItem(slotxxx);
            if (bestAxe != null
               && InventoryUtils.isAxe(bestAxe)
               && (InventoryUtils.getToolScore(bestAxe) > InventoryUtils.getToolScore(currentAxe) || !InventoryUtils.isAxe(currentAxe))) {
               this.swapItem(slotxxx, bestAxe);
            }
         }

         if (this.switchRod.getCurrentValue() && !this.offhandItems.isCurrentMode("Fishing Rod")) {
            int slotxxx = (int)(this.rodSlot.getCurrentValue() - 1.0F);
            ItemStack bestRod = InventoryUtils.getFishingRod();
            ItemStack currentRod = mc.player.getInventory().getItem(slotxxx);
            if (!(currentRod.getItem() instanceof FishingRodItem)) {
               this.swapItem(slotxxx, bestRod);
            }
         }

         if (this.switchBow.getCurrentValue()) {
            int slotxxx = (int)(this.bowSlot.getCurrentValue() - 1.0F);
            ItemStack currentBow = mc.player.getInventory().getItem(slotxxx);
            ItemStack bestBow;
            float bestBowScore;
            float currentBowScore;
            if (this.preferBow.isCurrentMode("Crossbow")) {
               bestBow = InventoryUtils.getBestCrossbow();
               bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
               currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
            } else if (this.preferBow.isCurrentMode("Power Bow")) {
               bestBow = InventoryUtils.getBestPowerBow();
               bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
               currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
            } else {
               bestBow = InventoryUtils.getBestPunchBow();
               bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
               currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
            }

            if (bestBow == null) {
               bestBow = InventoryUtils.getBestCrossbow();
               bestBowScore = InventoryUtils.getCrossbowScore(bestBow);
               currentBowScore = InventoryUtils.getCrossbowScore(currentBow);
            }

            if (bestBow == null) {
               bestBow = InventoryUtils.getBestPowerBow();
               bestBowScore = InventoryUtils.getPowerBowScore(bestBow);
               currentBowScore = InventoryUtils.getPowerBowScore(currentBow);
            }

            if (bestBow == null) {
               bestBow = InventoryUtils.getBestPunchBow();
               bestBowScore = InventoryUtils.getPunchBowScore(bestBow);
               currentBowScore = InventoryUtils.getPunchBowScore(currentBow);
            }

            if (bestBow != null && bestBowScore > currentBowScore) {
               this.swapItem(slotxxx, bestBow);
            }

            if ((float)InventoryUtils.getItemCount(Items.ARROW) > this.maxArrowSize.getCurrentValue()) {
               ItemStack worstArrow = InventoryUtils.getWorstArrow();
               this.throwItem(worstArrow);
            }
         }

         if (this.switchEnderPearl.getCurrentValue()) {
            this.swapItem((int)(this.enderPearlSlot.getCurrentValue() - 1.0F), Items.ENDER_PEARL);
         }

         if (this.switchWaterBucket.getCurrentValue()) {
            this.swapItem((int)(this.waterBucketSlot.getCurrentValue() - 1.0F), Items.WATER_BUCKET);
         }

         if (this.switchFireball.getCurrentValue()) {
            this.swapItem((int)(this.fireballSlot.getCurrentValue() - 1.0F), Items.FIRE_CHARGE);
         }

         if (this.keepProjectile.getCurrentValue()) {
            if ((float)(InventoryUtils.getItemCount(Items.EGG) + InventoryUtils.getItemCount(Items.SNOWBALL)) > this.maxProjectileSize.getCurrentValue()) {
               ItemStack worstProjectile = InventoryUtils.getWorstProjectile();
               this.throwItem(worstProjectile);
            }

            if (this.switchProjectile.getCurrentValue() && !this.offhandItems.isCurrentMode("Projectile")) {
               int projectileSlot = (int)(this.projectileSlot.getCurrentValue() - 1.0F);
               if (InventoryUtils.getItemCount(Items.EGG) > 0) {
                  this.swapItem(projectileSlot, Items.EGG);
               } else if (InventoryUtils.getItemCount(Items.SNOWBALL) > 0) {
                  this.swapItem(projectileSlot, Items.SNOWBALL);
               }
            }
         }

         if (this.throwItems.getCurrentValue()) {
            List<Integer> slots = IntStream.range(0, Inventory.INVENTORY_SIZE).boxed().collect(Collectors.toList());
            Collections.shuffle(slots);

            for (Integer slotxxxx : slots) {
               ItemStack stack = mc.player.getInventory().getItem(slotxxxx);
               if (!stack.isEmpty() && !this.isItemUseful(stack)) {
                  this.throwItem(stack);
               }
            }
         }
      }
   }

   private void swapOffHand(int slot) {
      if (slot < 9) {
         this.click(slot + 36, 40, ClickType.SWAP);
      } else {
         this.click(slot, 40, ClickType.SWAP);
      }

      this.inventoryOpen = true;
      timer.reset();
   }

   private void throwItem(ItemStack item) {
      if (InventoryUtils.isItemValid(item) && timer.delay(this.delay.getCurrentValue())) {
         int itemSlot = InventoryUtils.getItemStackSlot(item);
         if (itemSlot != -1) {
            if (itemSlot < 9) {
               this.click(itemSlot + 36, 1, ClickType.THROW);
            } else {
               this.click(itemSlot, 1, ClickType.THROW);
            }

            this.inventoryOpen = true;
            timer.reset();
         }
      }
   }

   private void swapItem(int targetSlot, ItemStack bestItem) {
      ItemStack currentSlot = mc.player.getInventory().getItem(targetSlot);
      if (InventoryUtils.isItemValid(currentSlot) && bestItem != currentSlot && timer.delay(this.delay.getCurrentValue())) {
         int bestItemSlot = InventoryUtils.getItemStackSlot(bestItem);
         if (bestItemSlot != -1) {
            if (bestItemSlot < 9) {
               this.click(bestItemSlot + 36, targetSlot, ClickType.SWAP);
            } else {
               this.click(bestItemSlot, targetSlot, ClickType.SWAP);
            }

            this.inventoryOpen = true;
            timer.reset();
         }
      }
   }

   private void swapItem(int targetSlot, Item item) {
      ItemStack currentSlot = mc.player.getInventory().getItem(targetSlot);
      if (InventoryUtils.isItemValid(currentSlot) && timer.delay(this.delay.getCurrentValue())) {
         int bestItemSlot = InventoryUtils.getItemSlot(item);
         if (bestItemSlot != -1) {
            ItemStack bestItemStack = mc.player.getInventory().getItem(bestItemSlot);
            if (currentSlot.getItem() != item || currentSlot.getItem() == item && currentSlot.getCount() < bestItemStack.getCount()) {
               if (bestItemSlot < 9) {
                  this.click(bestItemSlot + 36, targetSlot, ClickType.SWAP);
               } else {
                  this.click(bestItemSlot, targetSlot, ClickType.SWAP);
               }

               this.inventoryOpen = true;
               timer.reset();
            }
         }
      }
   }
}
