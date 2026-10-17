package tech.naven.modules.impl.misc;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventClick;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.move.AutoMLG;
import tech.naven.modules.impl.move.Scaffold;
import tech.naven.utils.PlayerUtils;
import tech.naven.utils.Vector2f;
import tech.naven.utils.rotation.RotationManager;
import tech.naven.utils.rotation.RotationUtils;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
   name = "Helper",
   description = "Handles auxiliary water, lava, and fire actions",
   category = Category.MISC
)
public class AutoHelper extends Module {
   private static final float ACTION_RANGE = 4.5F;
   private static final double ACTION_RANGE_SQ = 20.25D;
   private static final int SCAN_RADIUS = (int)Math.ceil((double)ACTION_RANGE);
   private static final int TIMEOUT_TICKS = 6;
   private static final int WATER_ACTION_COOLDOWN_TICKS = 8;
   private static final int FAST_ACTION_COOLDOWN_TICKS = 1;
   private static final int ABORT_COOLDOWN_TICKS = 4;
   private static final float ROTATION_EPSILON = 0.01F;
   private static final Direction[] SUPPORT_FACES = new Direction[]{
      Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN
   };

   private final BooleanValue autoExtinguish = ValueBuilder.create(this, "Auto Extinguish")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   private final BooleanValue retrievePlacedWater = ValueBuilder.create(this, "Retrieve Placed Water")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   private final BooleanValue collectNearbyWater = ValueBuilder.create(this, "Collect Water")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   private final BooleanValue autoBlockLava = ValueBuilder.create(this, "Block Lava")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();
   private final BooleanValue breakNearbyFire = ValueBuilder.create(this, "Anti Fire")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();

   public Vector2f rotation;
   private Action action = Action.NONE;
   private InteractionHand activeHand = InteractionHand.MAIN_HAND;
   private int originalSlot = -1;
   private int activeSlot = -1;
   private int timeout;
   private int postActionPauseTicks;
   private int pickupCooldownTicks;
   private int extinguishCooldownTicks;
   private int lavaCooldownTicks;
   private int fireBreakCooldownTicks;
   private BlockPos queuedPickupPos;
   private BlockPos targetPos;
   private Direction targetFace = Direction.UP;
   private BlockHitResult targetHit;

   @Override
   public void onEnable() {
      this.resetState();
   }

   @Override
   public void onDisable() {
      this.resetState();
   }

   public boolean isBusy() {
      return this.rotation != null || this.action != Action.NONE;
   }

   public void requestPlacedWaterPickup(BlockPos waterPos) {
      if (!this.isEnabled() || !this.retrievePlacedWater.getCurrentValue() || waterPos == null) {
         return;
      }

      this.queuedPickupPos = waterPos;
      this.postActionPauseTicks = Math.max(this.postActionPauseTicks, 1);
   }

   @EventTarget(0)
   public void onTick(EventRunTicks event) {
      if (event.getType() != EventType.PRE || mc.player == null) {
         return;
      }

      if (mc.level == null || mc.screen != null || mc.player.isFallFlying()) {
         this.abortAction(0);
         return;
      }

      this.tickCooldowns();
      if (!this.retrievePlacedWater.getCurrentValue()) {
         this.queuedPickupPos = null;
      }

      if (this.shouldYieldToAutoMLG()) {
         if (this.action != Action.NONE) {
            this.abortAction(0);
         }

         return;
      }

      if (this.action == Action.NONE) {
         if (this.rotation != null) {
            this.rotation = null;
         }

         if (this.postActionPauseTicks == 0) {
            this.selectAction();
         }

         return;
      }

      this.executeAction();
   }

   @EventTarget
   public void onClick(EventClick event) {
      if (!this.isBusy()) {
         return;
      }

      event.setCancelled(true);
   }

   private void selectAction() {
      if (this.shouldExtinguish()) {
         ItemUseSource waterSource = this.findItemSource(Items.WATER_BUCKET);
         if (waterSource != null) {
            BlockPos support = this.findWaterPlacementSupport();
            if (support != null && this.beginPlaceWater(support, waterSource)) {
               return;
            }
         }
      }

      if (this.retrievePlacedWater.getCurrentValue() && this.queuedPickupPos != null) {
         if (!this.isWaterSource(this.queuedPickupPos)) {
            this.queuedPickupPos = null;
         } else if (this.canStartNearbyAction()) {
            ItemUseSource bucketSource = this.findItemSource(Items.BUCKET);
            if (bucketSource != null && this.beginPickupWater(this.queuedPickupPos, bucketSource)) {
               return;
            }
         }
      }

      if (this.autoBlockLava.getCurrentValue() && this.lavaCooldownTicks == 0 && this.canStartNearbyAction()) {
         ItemUseSource blockSource = this.findBlockSource();
         if (blockSource != null) {
            BlockPlacementTarget lavaTarget = this.findNearbyLavaPlacementTarget();
            if (lavaTarget != null && this.beginPlaceBlock(lavaTarget, blockSource)) {
               return;
            }
         }
      }

      if (this.breakNearbyFire.getCurrentValue() && this.fireBreakCooldownTicks == 0 && this.canStartNearbyAction()) {
         FireBreakTarget fireTarget = this.findNearbyFireTarget();
         if (fireTarget != null && this.beginBreakFire(fireTarget)) {
            return;
         }
      }

      if (this.collectNearbyWater.getCurrentValue()
         && this.pickupCooldownTicks == 0
         && this.queuedPickupPos == null
         && this.canStartNearbyAction()
         && !this.hasAccessibleWaterBucket()) {
         ItemUseSource bucketSource = this.findItemSource(Items.BUCKET);
         if (bucketSource != null) {
            BlockPos nearbyWater = this.findNearbyWaterSource();
            if (nearbyWater != null) {
               this.beginPickupWater(nearbyWater, bucketSource);
            }
         }
      }
   }

   private void executeAction() {
      if (this.timeout > 0) {
         this.timeout--;
      }

      if (!this.isRotationApplied()) {
         if (this.timeout <= 0) {
            this.abortAction(this.cooldownFor(this.action));
         }

         return;
      }

      if (!this.isHandReady()) {
         this.abortAction(FAST_ACTION_COOLDOWN_TICKS);
         return;
      }

      if (!this.reverifyTarget()) {
         this.abortAction(this.cooldownFor(this.action));
         return;
      }

      boolean success = this.performInteraction();
      this.applyResult(success);
   }

   private boolean isRotationApplied() {
      Vector2f want = this.rotation;
      if (want == null || !RotationManager.active) {
         return false;
      }

      return this.matches(RotationManager.rotations, want) && this.matches(RotationManager.lastRotations, want);
   }

   private boolean matches(Vector2f a, Vector2f b) {
      return a != null
         && b != null
         && Math.abs(a.x - b.x) <= ROTATION_EPSILON
         && Math.abs(a.y - b.y) <= ROTATION_EPSILON;
   }

   private boolean reverifyTarget() {
      Vector2f applied = RotationManager.rotations;
      if (applied == null || this.targetPos == null) {
         return false;
      }

      return switch (this.action) {
         case PLACE_WATER -> this.isValidSupportFace(this.targetPos, Direction.UP)
            && mc.level.isEmptyBlock(this.targetPos.above())
            && this.isVisiblePlacement(applied, this.targetPos, Direction.UP, ClipContext.Fluid.NONE);
         case PICKUP_WATER -> this.isWaterSource(this.targetPos)
            && this.isVisibleBlock(applied, this.targetPos, ClipContext.Fluid.SOURCE_ONLY);
         case PLACE_BLOCK -> this.targetHit != null
            && this.isLavaSource(this.targetPos)
            && this.isValidSupportFace(this.targetHit.getBlockPos(), this.targetHit.getDirection())
            && this.isVisiblePlacement(
               applied, this.targetHit.getBlockPos(), this.targetHit.getDirection(), ClipContext.Fluid.NONE
            );
         case BREAK_FIRE -> this.isFireAt(this.targetPos)
            && this.isVisibleBlock(applied, this.targetPos, ClipContext.Fluid.NONE);
         default -> false;
      };
   }

   private boolean isHandReady() {
      ItemStack stack = this.getActiveStack();

      return switch (this.action) {
         case PLACE_WATER -> stack.getItem() == Items.WATER_BUCKET;
         case PICKUP_WATER -> stack.getItem() == Items.BUCKET;
         case PLACE_BLOCK -> this.isValidPlaceBlock(stack);
         case BREAK_FIRE -> true;
         default -> false;
      };
   }

   private boolean performInteraction() {
      if (mc.gameMode == null || mc.player == null) {
         return false;
      }

      float appliedYaw = RotationManager.rotations == null ? mc.player.getYRot() : RotationManager.rotations.x;
      float appliedPitch = RotationManager.rotations == null ? mc.player.getXRot() : RotationManager.rotations.y;
      float realYaw = mc.player.getYRot();
      float realPitch = mc.player.getXRot();
      boolean success = false;

      try {
         mc.player.setYRot(appliedYaw);
         mc.player.setXRot(appliedPitch);

         switch (this.action) {
            case PLACE_WATER, PICKUP_WATER -> {
               InteractionResult result = mc.gameMode.useItem(mc.player, this.activeHand);
               success = isAccepted(result);
               if (success) {
                  mc.player.swing(this.activeHand);
               }
            }
            case PLACE_BLOCK -> {
               InteractionResult result = mc.gameMode.useItemOn(mc.player, this.activeHand, this.targetHit);
               success = isAccepted(result);
               if (success) {
                  mc.player.swing(this.activeHand);
               }
            }
            case BREAK_FIRE -> {
               mc.gameMode.startDestroyBlock(this.targetPos, Direction.UP);
               mc.player.swing(InteractionHand.MAIN_HAND);
               success = true;
            }
            default -> {
            }
         }
      } finally {
         mc.player.setYRot(realYaw);
         mc.player.setXRot(realPitch);
      }

      return success;
   }

   private static boolean isAccepted(InteractionResult result) {
      return result != null && result != InteractionResult.PASS && result != InteractionResult.FAIL;
   }

   private void applyResult(boolean success) {
      if (!success) {
         if (this.timeout <= 0) {
            this.abortAction(this.cooldownFor(this.action));
         }

         return;
      }

      switch (this.action) {
         case PLACE_WATER -> {
            if (this.getActiveStack().getItem() == Items.BUCKET) {
               this.extinguishCooldownTicks = WATER_ACTION_COOLDOWN_TICKS;
               if (this.retrievePlacedWater.getCurrentValue() && this.targetPos != null) {
                  this.queuedPickupPos = this.targetPos.above();
               }
            } else {
               this.extinguishCooldownTicks = FAST_ACTION_COOLDOWN_TICKS;
            }

            this.finishAction();
         }
         case PICKUP_WATER -> {
            if (this.getActiveStack().getItem() == Items.WATER_BUCKET) {
               this.clearQueuedIfMatches(this.targetPos);
               this.pickupCooldownTicks = WATER_ACTION_COOLDOWN_TICKS;
            } else {
               this.pickupCooldownTicks = FAST_ACTION_COOLDOWN_TICKS;
            }

            this.finishAction();
         }
         case PLACE_BLOCK -> {
            this.lavaCooldownTicks = FAST_ACTION_COOLDOWN_TICKS;
            this.finishAction();
         }
         case BREAK_FIRE -> {
            this.fireBreakCooldownTicks = FAST_ACTION_COOLDOWN_TICKS;
            this.finishAction();
         }
         default -> this.finishAction();
      }

      this.postActionPauseTicks = Math.max(this.postActionPauseTicks, 1);
   }

   private BlockPos findWaterPlacementSupport() {
      BlockPos feet = BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      BlockPos[] candidates = new BlockPos[]{feet.below(), feet.below(2), feet.below(3)};

      for (BlockPos support : candidates) {
         if (!this.isValidSupportFace(support, Direction.UP)) {
            continue;
         }

         if (!mc.level.isEmptyBlock(support.above())) {
            continue;
         }

         Vector2f rot = this.rotationTo(Scaffold.getFaceCenter(support, Direction.UP));
         if (rot == null) {
            continue;
         }

         if (this.isVisiblePlacement(rot, support, Direction.UP, ClipContext.Fluid.NONE)) {
            return support;
         }
      }

      return null;
   }

   private BlockPos findNearbyWaterSource() {
      BlockPos base = BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      Vec3 eye = mc.player.getEyePosition(1.0F);

      for (int dy = -2; dy <= 2; dy++) {
         for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
               BlockPos pos = base.offset(dx, dy, dz);
               if (!this.isWaterSource(pos)) {
                  continue;
               }

               if (!mc.level.isEmptyBlock(pos.above())) {
                  continue;
               }

               double distance = eye.distanceToSqr(blockCenter(pos));
               if (distance > ACTION_RANGE_SQ || distance >= bestDistance) {
                  continue;
               }

               Vector2f rot = this.rotationTo(Scaffold.getFaceCenter(pos, Direction.UP));
               if (rot == null) {
                  continue;
               }

               if (this.isVisibleBlock(rot, pos, ClipContext.Fluid.SOURCE_ONLY)) {
                  bestDistance = distance;
                  best = pos;
               }
            }
         }
      }

      return best;
   }

   private BlockPlacementTarget findNearbyLavaPlacementTarget() {
      BlockPos base = BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      BlockPlacementTarget best = null;
      double bestDistance = Double.MAX_VALUE;
      Vec3 eye = mc.player.getEyePosition(1.0F);

      for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
         for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
               BlockPos lavaPos = base.offset(dx, dy, dz);
               if (!this.isLavaSource(lavaPos)) {
                  continue;
               }

               double distance = eye.distanceToSqr(blockCenter(lavaPos));
               if (distance > ACTION_RANGE_SQ || distance >= bestDistance) {
                  continue;
               }

               BlockPlacementTarget target = this.buildLavaPlacementTarget(lavaPos);
               if (target != null) {
                  bestDistance = distance;
                  best = target;
               }
            }
         }
      }

      return best;
   }

   private BlockPlacementTarget buildLavaPlacementTarget(BlockPos lavaPos) {
      for (Direction face : SUPPORT_FACES) {
         BlockPos supportPos = lavaPos.relative(face);
         Direction clickedFace = face.getOpposite();
         if (!this.isValidSupportFace(supportPos, clickedFace)) {
            continue;
         }

         Vec3 hitVec = Scaffold.getFaceCenter(supportPos, clickedFace);
         double distance = mc.player.getEyePosition(1.0F).distanceToSqr(hitVec);
         if (distance > ACTION_RANGE_SQ) {
            continue;
         }

         Vector2f rot = this.rotationTo(hitVec);
         if (rot == null) {
            continue;
         }

         if (!this.isVisiblePlacement(rot, supportPos, clickedFace, ClipContext.Fluid.NONE)) {
            continue;
         }

         return new BlockPlacementTarget(lavaPos, new BlockHitResult(hitVec, clickedFace, supportPos, false), rot);
      }

      return null;
   }

   private FireBreakTarget findNearbyFireTarget() {
      BlockPos base = BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      FireBreakTarget best = null;
      double bestDistance = Double.MAX_VALUE;
      Vec3 eye = mc.player.getEyePosition(1.0F);

      for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
         for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
               BlockPos pos = base.offset(dx, dy, dz);
               if (!this.isFireAt(pos)) {
                  continue;
               }

               Vec3 center = blockCenter(pos);
               double distance = eye.distanceToSqr(center);
               if (distance > ACTION_RANGE_SQ || distance >= bestDistance) {
                  continue;
               }

               Vector2f rot = this.rotationTo(center);
               if (rot == null) {
                  continue;
               }

               if (this.isVisibleBlock(rot, pos, ClipContext.Fluid.NONE)) {
                  bestDistance = distance;
                  best = new FireBreakTarget(pos, rot);
               }
            }
         }
      }

      return best;
   }

   private boolean beginPlaceWater(BlockPos support, ItemUseSource source) {
      if (!this.isValidSupportFace(support, Direction.UP) || !mc.level.isEmptyBlock(support.above())) {
         return false;
      }

      Vector2f rot = this.rotationTo(Scaffold.getFaceCenter(support, Direction.UP));
      if (rot == null || !this.isVisiblePlacement(rot, support, Direction.UP, ClipContext.Fluid.NONE)) {
         return false;
      }

      if (!this.prepareMainHandSlot(source)) {
         return false;
      }

      this.activeHand = source.hand;
      this.activeSlot = source.hand == InteractionHand.MAIN_HAND ? source.slot : -1;
      this.targetPos = support;
      this.targetFace = Direction.UP;
      this.targetHit = null;
      this.action = Action.PLACE_WATER;
      this.rotation = rot;
      this.timeout = TIMEOUT_TICKS;
      return true;
   }

   private boolean beginPickupWater(BlockPos waterPos, ItemUseSource source) {
      if (!this.isWaterSource(waterPos)) {
         return false;
      }

      Vector2f rot = this.rotationTo(Scaffold.getFaceCenter(waterPos, Direction.UP));
      if (rot == null || !this.isVisibleBlock(rot, waterPos, ClipContext.Fluid.SOURCE_ONLY)) {
         return false;
      }

      if (!this.prepareMainHandSlot(source)) {
         return false;
      }

      this.activeHand = source.hand;
      this.activeSlot = source.hand == InteractionHand.MAIN_HAND ? source.slot : -1;
      this.targetPos = waterPos;
      this.targetFace = Direction.UP;
      this.targetHit = null;
      this.action = Action.PICKUP_WATER;
      this.rotation = rot;
      this.timeout = TIMEOUT_TICKS;
      return true;
   }

   private boolean beginPlaceBlock(BlockPlacementTarget target, ItemUseSource source) {
      if (target == null || !this.isLavaSource(target.lavaPos)) {
         return false;
      }

      if (!this.prepareMainHandSlot(source)) {
         return false;
      }

      this.activeHand = source.hand;
      this.activeSlot = source.hand == InteractionHand.MAIN_HAND ? source.slot : -1;
      this.targetPos = target.lavaPos;
      this.targetFace = target.hitResult.getDirection();
      this.targetHit = target.hitResult;
      this.action = Action.PLACE_BLOCK;
      this.rotation = target.rotation;
      this.timeout = TIMEOUT_TICKS;
      return true;
   }

   private boolean beginBreakFire(FireBreakTarget target) {
      if (target == null || !this.isFireAt(target.pos)) {
         return false;
      }

      this.activeHand = InteractionHand.MAIN_HAND;
      this.activeSlot = -1;
      this.targetPos = target.pos;
      this.targetFace = Direction.UP;
      this.targetHit = null;
      this.action = Action.BREAK_FIRE;
      this.rotation = target.rotation;
      this.timeout = TIMEOUT_TICKS;
      return true;
   }

   private BlockHitResult rayCast(Vector2f rot, double range, ClipContext.Fluid fluid) {
      if (mc.player == null || mc.level == null || rot == null) {
         return null;
      }

      Vec3 eye = mc.player.getEyePosition(1.0F);
      Vec3 look = PlayerUtils.getVectorForRotation(rot);
      Vec3 end = eye.add(look.x * range, look.y * range, look.z * range);
      HitResult result = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, fluid, mc.player));
      return result instanceof BlockHitResult blockHit ? blockHit : null;
   }

   private boolean isVisiblePlacement(Vector2f rot, BlockPos pos, Direction face, ClipContext.Fluid fluid) {
      BlockHitResult hit = this.rayCast(rot, ACTION_RANGE, fluid);
      return hit != null && hit.getBlockPos().equals(pos) && hit.getDirection() == face;
   }

   private boolean isVisibleBlock(Vector2f rot, BlockPos pos, ClipContext.Fluid fluid) {
      BlockHitResult hit = this.rayCast(rot, ACTION_RANGE, fluid);
      return hit != null && hit.getBlockPos().equals(pos);
   }

   private Vector2f rotationTo(Vec3 target) {
      if (mc.player == null || target == null) {
         return null;
      }

      Vec3 eye = mc.player.getEyePosition(1.0F);
      if (eye.distanceToSqr(target) > ACTION_RANGE_SQ) {
         return null;
      }

      return RotationUtils.getRotations(eye, target).toVec2f();
   }

   private boolean isValidSupportFace(BlockPos pos, Direction face) {
      if (mc.level == null || pos == null) {
         return false;
      }

      BlockState state = mc.level.getBlockState(pos);
      if (state.isAir()) {
         return false;
      }

      if (isInteractiveBlock(state.getBlock())) {
         return false;
      }

      if (mc.level.getFluidState(pos).getType() != Fluids.EMPTY) {
         return false;
      }

      return state.entityCanStandOnFace(mc.level, pos, mc.player, face);
   }

   private static boolean isInteractiveBlock(Block block) {
      return block == Blocks.CHEST
         || block == Blocks.TRAPPED_CHEST
         || block == Blocks.ENDER_CHEST
         || block == Blocks.FURNACE
         || block == Blocks.CRAFTING_TABLE
         || block == Blocks.ANVIL
         || block == Blocks.ENCHANTING_TABLE
         || block == Blocks.BARREL
         || block == Blocks.HOPPER
         || block == Blocks.DISPENSER
         || block == Blocks.DROPPER
         || block == Blocks.CHISELED_BOOKSHELF;
   }

   private boolean isWaterSource(BlockPos pos) {
      if (mc.level == null || pos == null) {
         return false;
      }

      FluidState fluidState = mc.level.getFluidState(pos);
      return fluidState.getType() == Fluids.WATER && fluidState.isSource();
   }

   private boolean isLavaSource(BlockPos pos) {
      if (mc.level == null || pos == null) {
         return false;
      }

      FluidState fluidState = mc.level.getFluidState(pos);
      return fluidState.getType() == Fluids.LAVA && fluidState.isSource();
   }

   private boolean isFireAt(BlockPos pos) {
      if (mc.level == null || pos == null) {
         return false;
      }

      Block block = mc.level.getBlockState(pos).getBlock();
      return block == Blocks.FIRE || block == Blocks.SOUL_FIRE;
   }

   private boolean prepareMainHandSlot(ItemUseSource source) {
      if (mc.player == null || source == null) {
         return false;
      }

      if (source.hand == InteractionHand.OFF_HAND) {
         return true;
      }

      if (source.slot < 0 || source.slot > 8) {
         return false;
      }

      if (this.originalSlot == -1) {
         this.originalSlot = mc.player.getInventory().getSelectedSlot();
      }
      mc.player.getInventory().setSelectedSlot(source.slot);
      return true;
   }

   private ItemStack getActiveStack() {
      if (mc.player == null) {
         return ItemStack.EMPTY;
      }

      if (this.activeHand == InteractionHand.OFF_HAND) {
         return mc.player.getOffhandItem();
      }

      return this.activeSlot < 0 ? ItemStack.EMPTY : mc.player.getInventory().getItem(this.activeSlot);
   }

   private int findHotbarSlot(Item item) {
      if (mc.player == null) {
         return -1;
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private boolean isOffhandItem(Item item) {
      if (mc.player == null) {
         return false;
      }

      ItemStack stack = mc.player.getOffhandItem();
      return !stack.isEmpty() && stack.getItem() == item;
   }

   private ItemUseSource findItemSource(Item item) {
      if (this.isOffhandItem(item)) {
         return new ItemUseSource(InteractionHand.OFF_HAND, -1);
      }

      int slot = this.findHotbarSlot(item);
      return slot == -1 ? null : new ItemUseSource(InteractionHand.MAIN_HAND, slot);
   }

   private ItemUseSource findBlockSource() {
      if (mc.player == null) {
         return null;
      }

      if (this.isValidPlaceBlock(mc.player.getOffhandItem())) {
         return new ItemUseSource(InteractionHand.OFF_HAND, -1);
      }

      int selected = mc.player.getInventory().getSelectedSlot();
      if (this.isValidPlaceBlock(mc.player.getInventory().getItem(selected))) {
         return new ItemUseSource(InteractionHand.MAIN_HAND, selected);
      }

      for (int i = 0; i < 9; i++) {
         if (i == selected) {
            continue;
         }

         if (this.isValidPlaceBlock(mc.player.getInventory().getItem(i))) {
            return new ItemUseSource(InteractionHand.MAIN_HAND, i);
         }
      }

      return null;
   }

   private boolean isValidPlaceBlock(ItemStack stack) {
      return stack != null
         && !stack.isEmpty()
         && stack.getItem() instanceof BlockItem
         && Scaffold.isValidStack(stack);
   }

   private boolean hasAccessibleWaterBucket() {
      return this.isOffhandItem(Items.WATER_BUCKET) || this.findHotbarSlot(Items.WATER_BUCKET) != -1;
   }

   private boolean shouldExtinguish() {
      return this.autoExtinguish.getCurrentValue()
         && this.extinguishCooldownTicks == 0
         && mc.player.fallDistance <= 0.5F
         && mc.player.isOnFire()
         && !mc.player.isInWater();
   }

   private boolean canStartNearbyAction() {
      return this.postActionPauseTicks == 0 && mc.player.fallDistance <= 0.5F;
   }

   private boolean shouldYieldToAutoMLG() {
      AutoMLG autoMLG = (AutoMLG)Naven.getInstance().getModuleManager().getModule(AutoMLG.class);
      return autoMLG != null && autoMLG.isEnabled() && autoMLG.rotation;
   }

   private void tickCooldowns() {
      if (this.postActionPauseTicks > 0) {
         this.postActionPauseTicks--;
      }

      if (this.pickupCooldownTicks > 0) {
         this.pickupCooldownTicks--;
      }

      if (this.extinguishCooldownTicks > 0) {
         this.extinguishCooldownTicks--;
      }

      if (this.lavaCooldownTicks > 0) {
         this.lavaCooldownTicks--;
      }

      if (this.fireBreakCooldownTicks > 0) {
         this.fireBreakCooldownTicks--;
      }
   }

   private int cooldownFor(Action action) {
      return switch (action) {
         case PLACE_WATER, PICKUP_WATER -> WATER_ACTION_COOLDOWN_TICKS;
         case PLACE_BLOCK, BREAK_FIRE -> ABORT_COOLDOWN_TICKS;
         default -> FAST_ACTION_COOLDOWN_TICKS;
      };
   }

   private void abortAction(int cooldownTicks) {
      Action aborted = this.action;
      this.finishAction();

      switch (aborted) {
         case PLACE_WATER -> this.extinguishCooldownTicks = Math.max(this.extinguishCooldownTicks, cooldownTicks);
         case PICKUP_WATER -> this.pickupCooldownTicks = Math.max(this.pickupCooldownTicks, cooldownTicks);
         case PLACE_BLOCK -> this.lavaCooldownTicks = Math.max(this.lavaCooldownTicks, cooldownTicks);
         case BREAK_FIRE -> this.fireBreakCooldownTicks = Math.max(this.fireBreakCooldownTicks, cooldownTicks);
         default -> {
         }
      }
   }

   private void finishAction() {
      this.rotation = null;
      this.action = Action.NONE;
      this.activeHand = InteractionHand.MAIN_HAND;
      this.activeSlot = -1;
      this.timeout = 0;
      this.targetPos = null;
      this.targetFace = Direction.UP;
      this.targetHit = null;
      this.restoreOriginalSlot();
   }

   private void restoreOriginalSlot() {
      if (mc.player != null && this.originalSlot != -1) {
         mc.player.getInventory().setSelectedSlot(this.originalSlot);
      }

      this.originalSlot = -1;
   }

   private void clearQueuedIfMatches(BlockPos pos) {
      if (pos != null && pos.equals(this.queuedPickupPos)) {
         this.queuedPickupPos = null;
      }
   }

   private void resetState() {
      this.queuedPickupPos = null;
      this.postActionPauseTicks = 0;
      this.pickupCooldownTicks = 0;
      this.extinguishCooldownTicks = 0;
      this.lavaCooldownTicks = 0;
      this.fireBreakCooldownTicks = 0;
      this.finishAction();
   }

   private static Vec3 blockCenter(BlockPos pos) {
      return new Vec3((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
   }

   private enum Action {
      NONE,
      PLACE_WATER,
      PICKUP_WATER,
      PLACE_BLOCK,
      BREAK_FIRE
   }

   private static final class ItemUseSource {
      private final InteractionHand hand;
      private final int slot;

      private ItemUseSource(InteractionHand hand, int slot) {
         this.hand = hand;
         this.slot = slot;
      }
   }

   private static final class BlockPlacementTarget {
      private final BlockPos lavaPos;
      private final BlockHitResult hitResult;
      private final Vector2f rotation;

      private BlockPlacementTarget(BlockPos lavaPos, BlockHitResult hitResult, Vector2f rotation) {
         this.lavaPos = lavaPos;
         this.hitResult = hitResult;
         this.rotation = rotation;
      }
   }

   private static final class FireBreakTarget {
      private final BlockPos pos;
      private final Vector2f rotation;

      private FireBreakTarget(BlockPos pos, Vector2f rotation) {
         this.pos = pos;
         this.rotation = rotation;
      }
   }
}
