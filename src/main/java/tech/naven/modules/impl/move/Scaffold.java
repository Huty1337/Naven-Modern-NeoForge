package tech.naven.modules.impl.move;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventClick;
import tech.naven.events.impl.EventHandlePacket;
import tech.naven.events.impl.EventMotion;
import tech.naven.events.impl.EventMoveInput;
import tech.naven.events.impl.EventRender;
import tech.naven.events.impl.EventRender2D;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.events.impl.EventUpdateFoV;
import tech.naven.events.impl.EventUpdateHeldItem;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.render.Render3DUtils;
import tech.naven.utils.ChatUtils;
import tech.naven.utils.FallingPlayer;
import tech.naven.utils.InventoryUtils;
import tech.naven.utils.MathUtils;
import tech.naven.utils.MoveUtils;
import tech.naven.utils.NetworkUtils;
import tech.naven.utils.PlayerUtils;
import tech.naven.utils.RayTraceUtils;
import tech.naven.utils.RenderUtils;
import tech.naven.utils.SmoothAnimationTimer;
import tech.naven.utils.Vector2f;
import tech.naven.utils.renderer.Fonts;
import tech.naven.utils.rotation.Rotation;
import tech.naven.utils.rotation.RotationUtils;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;
import tech.naven.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

@ModuleInfo(
   name = "Scaffold",
   description = "Automatically places blocks under you",
   category = Category.MOVEMENT
)
public class Scaffold extends Module {
   public static final List<Block> blacklistedBlocks = Arrays.asList(
      net.minecraft.world.level.block.Blocks.AIR,
      net.minecraft.world.level.block.Blocks.WATER,
      net.minecraft.world.level.block.Blocks.LAVA,
      net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE,
      net.minecraft.world.level.block.Blocks.GLASS_PANE,
      net.minecraft.world.level.block.Blocks.IRON_BARS,
      net.minecraft.world.level.block.Blocks.SNOW,
      net.minecraft.world.level.block.Blocks.COAL_ORE,
      net.minecraft.world.level.block.Blocks.DIAMOND_ORE,
      net.minecraft.world.level.block.Blocks.EMERALD_ORE,
      net.minecraft.world.level.block.Blocks.CHEST,
      net.minecraft.world.level.block.Blocks.TRAPPED_CHEST,
      net.minecraft.world.level.block.Blocks.TORCH,
      net.minecraft.world.level.block.Blocks.ANVIL,
      net.minecraft.world.level.block.Blocks.NOTE_BLOCK,
      net.minecraft.world.level.block.Blocks.JUKEBOX,
      net.minecraft.world.level.block.Blocks.TNT,
      net.minecraft.world.level.block.Blocks.GOLD_ORE,
      net.minecraft.world.level.block.Blocks.IRON_ORE,
      net.minecraft.world.level.block.Blocks.LAPIS_ORE,
      net.minecraft.world.level.block.Blocks.STONE_PRESSURE_PLATE,
      net.minecraft.world.level.block.Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
      net.minecraft.world.level.block.Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
      net.minecraft.world.level.block.Blocks.STONE_BUTTON,
      net.minecraft.world.level.block.Blocks.LEVER,
      net.minecraft.world.level.block.Blocks.TALL_GRASS,
      net.minecraft.world.level.block.Blocks.TRIPWIRE,
      net.minecraft.world.level.block.Blocks.TRIPWIRE_HOOK,
      net.minecraft.world.level.block.Blocks.RAIL,
      net.minecraft.world.level.block.Blocks.CORNFLOWER,
      net.minecraft.world.level.block.Blocks.RED_MUSHROOM,
      net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM,
      net.minecraft.world.level.block.Blocks.VINE,
      net.minecraft.world.level.block.Blocks.SUNFLOWER,
      net.minecraft.world.level.block.Blocks.LADDER,
      net.minecraft.world.level.block.Blocks.FURNACE,
      net.minecraft.world.level.block.Blocks.SAND,
      net.minecraft.world.level.block.Blocks.CACTUS,
      net.minecraft.world.level.block.Blocks.DISPENSER,
      net.minecraft.world.level.block.Blocks.DROPPER,
      net.minecraft.world.level.block.Blocks.CRAFTING_TABLE,
      net.minecraft.world.level.block.Blocks.COBWEB,
      net.minecraft.world.level.block.Blocks.PUMPKIN,
      net.minecraft.world.level.block.Blocks.COBBLESTONE_WALL,
      net.minecraft.world.level.block.Blocks.OAK_FENCE,
      net.minecraft.world.level.block.Blocks.REDSTONE_TORCH,
      net.minecraft.world.level.block.Blocks.FLOWER_POT
   );

   public Vector2f correctRotation = new Vector2f();
   public Vector2f rots = new Vector2f();
   public Vector2f lastRots = new Vector2f();

   public ModeValue mode = ValueBuilder.create(this, "Mode")
      .setDefaultModeIndex(0)
      .setModes("Normal", "Telly Bridge", "Keep Y")
      .build()
      .getModeValue();
   FloatValue tellyAirTicks = ValueBuilder.create(this, "AirTicks")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(3.0F)
      .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
      .build()
      .getFloatValue();
   FloatValue tellyPlaceDelay = ValueBuilder.create(this, "PlaceDelay")
      .setDefaultFloatValue(0.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(20.0F)
      .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
      .build()
      .getFloatValue();
   public BooleanValue eagle = ValueBuilder.create(this, "Eagle")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.mode.isCurrentMode("Normal"))
      .build()
      .getBooleanValue();
   public BooleanValue sneak = ValueBuilder.create(this, "Sneak")
      .setDefaultBooleanValue(false)
      .setVisibility(() -> this.mode.isCurrentMode("Normal"))
      .build()
      .getBooleanValue();
   public BooleanValue snap = ValueBuilder.create(this, "Snap")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.mode.isCurrentMode("Normal"))
      .build()
      .getBooleanValue();
   public BooleanValue hideSnap = ValueBuilder.create(this, "Hide Snap Rotation")
      .setDefaultBooleanValue(true)
      .setVisibility(() -> this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue())
      .build()
      .getBooleanValue();
   public BooleanValue renderItemSpoof = ValueBuilder.create(this, "Render Item Spoof").setDefaultBooleanValue(true).build().getBooleanValue();
   public BooleanValue clutch = ValueBuilder.create(this, "Clutch").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue clutchDepth = ValueBuilder.create(this, "Clutch Depth")
      .setDefaultFloatValue(4.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(2.0F)
      .setMaxFloatValue(5.0F)
      .setVisibility(() -> this.clutch.getCurrentValue())
      .build()
      .getFloatValue();
   public BooleanValue onTickRot = ValueBuilder.create(this, "OnTickRot").setDefaultBooleanValue(false).build().getBooleanValue();
   public BooleanValue syncRotSpeed = ValueBuilder.create(this, "Sync RotSpeed").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue rotationSpeed = ValueBuilder.create(this, "Rotation Speed")
      .setDefaultFloatValue(180.0F)
      .setFloatStep(5.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(360.0F)
      .setVisibility(() -> !this.syncRotSpeed.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue jitter = ValueBuilder.create(this, "Jitter")
      .setDefaultFloatValue(1.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(5.0F)
      .build()
      .getFloatValue();
   FloatValue turnSpeed = ValueBuilder.create(this, "Turn Speed")
      .setDefaultFloatValue(180.0F)
      .setFloatStep(5.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(360.0F)
      .setVisibility(() -> this.syncRotSpeed.getCurrentValue())
      .build()
      .getFloatValue();
   FloatValue returnSpeed = ValueBuilder.create(this, "Return Speed")
      .setDefaultFloatValue(180.0F)
      .setFloatStep(5.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(360.0F)
      .setVisibility(() -> this.syncRotSpeed.getCurrentValue())
      .build()
      .getFloatValue();
   public BooleanValue bpsLimit = ValueBuilder.create(this, "Limit BPS").setDefaultBooleanValue(false).build().getBooleanValue();
   FloatValue bps = ValueBuilder.create(this, "BPS")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(2.0F)
      .setMaxFloatValue(10.0F)
      .setVisibility(() -> this.bpsLimit.getCurrentValue())
      .build()
      .getFloatValue();
   public BooleanValue keepFoV = ValueBuilder.create(this, "Keep FoV").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue fov = ValueBuilder.create(this, "FoV")
      .setDefaultFloatValue(1.15F)
      .setMaxFloatValue(2.0F)
      .setMinFloatValue(1.0F)
      .setFloatStep(0.05F)
      .setVisibility(() -> this.keepFoV.getCurrentValue())
      .build()
      .getFloatValue();

   private final RotationSmoother rotSmoother = new RotationSmoother(new Random());
   private final List<Runnable> pendingTicks = new ArrayList<>();
   private final Vector2f lastSentRot = new Vector2f();

   int oldSlot;
   private int altSlot = -1;
   private boolean slotActive;
   private int slotActiveTick;
   private int slotTicks;
   private BlockPosWithFacing pos;
   private int airTicks;
   private int rotationDelay;
   private int velocityDelay;
   private int tellyPlaceDelayTimer;
   private int lastSneakTicks;
   private int offGroundTicks;
   private boolean canBuildNow = true;
   private BlockPos lastC05Position;
   private int baseY = -1;
   private int initialBlocks;
   private boolean hudInitialized;
   private String aimBranch = "none";
   private boolean usedSkipTicks;
   private double lastHorizontalSpeed;

   public static boolean isValidStack(ItemStack stack) {
      if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
         return false;
      } else if (!InventoryUtils.isItemValid(stack)) {
         return false;
      } else {
         String string = stack.getDisplayName().getString();
         if (string.contains("Click") || string.contains("点击")) {
            return false;
         } else {
            Block block = ((BlockItem)stack.getItem()).getBlock();
            if (block instanceof FlowerBlock) {
               return false;
            } else if (block instanceof BushBlock) {
               return false;
            } else if (block instanceof FungusBlock) {
               return false;
            } else if (block instanceof CropBlock) {
               return false;
            } else {
               return block instanceof SlabBlock ? false : !blacklistedBlocks.contains(block);
            }
         }
      }
   }

   public static boolean isOnBlockEdge(float inflate) {
      if (mc.level == null || mc.player == null) {
         return false;
      }

      double halfWidth = mc.player.getBbWidth() / 2.0;
      if (inflate >= halfWidth) {
         return true;
      }

      double margin = Math.min(inflate, halfWidth * 0.99);
      Vec3 position = mc.player.position();
      for (int sx = -1; sx <= 1; sx += 2) {
         for (int sz = -1; sz <= 1; sz += 2) {
            BlockPos corner = BlockPos.containing(position.x + sx * margin, position.y - 1.0, position.z + sz * margin);
            BlockState state = mc.level.getBlockState(corner);
            if (!state.entityCanStandOn(mc.level, corner, mc.player)) {
               return true;
            }
         }
      }

      return false;
   }

   public static double getSpeed() {
      if (mc.player == null) {
         return 0.0;
      }

      Vec3 delta = mc.player.getDeltaMovement();
      return Math.sqrt(delta.x * delta.x + delta.z * delta.z);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.oldSlot = mc.player.getInventory().getSelectedSlot();
         this.altSlot = this.oldSlot;
         this.slotActive = false;
         this.slotActiveTick = 0;
         this.slotTicks = 0;
         this.rots.set(mc.player.getYRot() - 180.0F, mc.player.getXRot());
         this.lastRots.set(mc.player.yRotO - 180.0F, mc.player.xRotO);
         this.lastSentRot.set(mc.player.getYRot(), mc.player.getXRot());
         this.pos = null;
         this.baseY = 10000;
         this.velocityDelay = 0;
         this.rotationDelay = 0;
         this.canBuildNow = true;
         this.lastC05Position = null;
         this.airTicks = 0;
         this.tellyPlaceDelayTimer = 0;
         this.rotSmoother.reset();
      }

      this.pendingTicks.clear();
   }

   @Override
   public void onDisable() {
      this.pendingTicks.clear();
      if (mc.player != null && mc.getWindow() != null) {
         boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow(), mc.options.keyJump.getKey().getValue());
         boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow(), mc.options.keyShift.getKey().getValue());
         mc.options.keyJump.setDown(isHoldingJump);
         mc.options.keyShift.setDown(isHoldingShift);
         mc.options.keyUse.setDown(false);
         if (this.slotActive) {
            mc.player.getInventory().setSelectedSlot(this.altSlot);
            this.slotActive = false;
         }
      }

      this.canBuildNow = true;
      this.lastC05Position = null;
      this.rotSmoother.reset();
   }

   @EventTarget
   public void onFoV(EventUpdateFoV e) {
      if (this.keepFoV.getCurrentValue() && MoveUtils.isMoving()) {
         e.setFov(this.fov.getCurrentValue() + (float)PlayerUtils.getMoveSpeedEffectAmplifier() * 0.13F);
      }
   }

   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      if (mc.player == null || e.getHand() != InteractionHand.MAIN_HAND) {
         return;
      }

      boolean silenceActive = this.slotActive;
      if ((silenceActive || this.renderItemSpoof.getCurrentValue()) && this.altSlot >= 0) {
         e.setItem(mc.player.getInventory().getItem(this.altSlot));
      }
   }

   @EventTarget
   public void onPacket(EventHandlePacket e) {
      if (mc.player == null || mc.level == null) {
         return;
      }

      if (e.getPacket() instanceof ClientboundSetEntityMotionPacket motion && motion.getId() == mc.player.getId()) {
         Vec3 movement = motion.getMovement();
         double length = new Vec3(movement.x, 0.0, movement.z).length();
         if (length >= 0.6) {
            this.velocityDelay = 60;
         }
      }
   }

   @EventTarget(4)
   public void onMoveInput(EventMoveInput e) {
      if (mc.player == null || !this.bpsLimit.getCurrentValue()) {
         return;
      }

      double limit = this.bps.getCurrentValue();
      if (limit >= 10.0) {
         return;
      }

      if (getSpeed() > limit / 20.0) {
         e.setForward(0.0F);
         e.setStrafe(0.0F);
      }
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (mc.player == null || mc.level == null || mc.screen != null) {
         return;
      }

      if (e.getType() == EventType.POST) {
         if (mc.player.onGround()) {
            this.airTicks = 0;
         } else {
            this.airTicks++;
         }

         return;
      }

      if (e.getType() != EventType.PRE) {
         return;
      }

      if (this.pos == null) {
         return;
      }

      e.setCancelled(true);
      if (!this.canBuildNow && this.rotationDelay > 0) {
         mc.options.keyJump.setDown(false);
      }

      if (this.onTickRot.getCurrentValue() || !this.canBuildNow && this.rotationDelay > 0) {
         if (this.mode.isCurrentMode("Telly Bridge") && this.airTicks < (int)this.tellyAirTicks.getCurrentValue()) {
            return;
         }

         if (this.mode.isCurrentMode("Telly Bridge")
            && (int)this.tellyPlaceDelay.getCurrentValue() > 0
            && this.tellyPlaceDelayTimer < (int)this.tellyPlaceDelay.getCurrentValue()) {
            return;
         }

         this.c06Place();
         return;
      }

      if (this.mode.isCurrentMode("Telly Bridge") && this.airTicks < (int)this.tellyAirTicks.getCurrentValue()) {
         return;
      }

      if (this.mode.isCurrentMode("Telly Bridge")
         && (int)this.tellyPlaceDelay.getCurrentValue() > 0
         && this.tellyPlaceDelayTimer < (int)this.tellyPlaceDelay.getCurrentValue()) {
         return;
      }

      if (!this.canBuildNow && !this.isPlacementReachable()) {
         return;
      }

      if (this.rotationDelay <= 0 && !this.mode.isCurrentMode("Telly Bridge") && !this.isRotationValidForPlacement(this.rots)) {
         return;
      }

      this.doSnap();
   }

   @EventTarget(1)
   public void onEventEarlyTick(EventRunTicks e) {
      if (e.getType() != EventType.PRE || mc.player == null || mc.screen != null) {
         return;
      }

      if (!this.pendingTicks.isEmpty()) {
         List<Runnable> tasks = new ArrayList<>(this.pendingTicks);
         this.pendingTicks.clear();
         tasks.forEach(Runnable::run);
      }

      if (this.slotActive && this.slotActiveTick != this.slotTicks) {
         mc.player.getInventory().setSelectedSlot(this.altSlot);
         this.slotActive = false;
      }

      this.slotTicks++;
      if (this.velocityDelay > 0) {
         this.velocityDelay--;
      }

      double horizontalSpeed = Math.sqrt(
         mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z
      );
      if (!mc.player.onGround() && horizontalSpeed - this.lastHorizontalSpeed > 0.25) {
         this.velocityDelay = Math.max(this.velocityDelay, 40);
      }

      this.lastHorizontalSpeed = horizontalSpeed;

      if (mc.player.onGround() && this.velocityDelay <= 30) {
         this.velocityDelay = 0;
      }

      boolean offhandPlaceable = isValidStack(mc.player.getOffhandItem());
      int placeableSlot = -1;
      if (!offhandPlaceable) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isValidStack(stack)) {
               placeableSlot = i;
               break;
            }
         }
      }

      boolean hasBlocks = offhandPlaceable || placeableSlot != -1;
      if (placeableSlot != -1 && placeableSlot != mc.player.getInventory().getSelectedSlot()) {
         if (!this.slotActive) {
            this.altSlot = mc.player.getInventory().getSelectedSlot();
         }

         mc.player.getInventory().setSelectedSlot(placeableSlot);
         this.slotActive = true;
         this.slotActiveTick = this.slotTicks;
      }

      if (mc.player.onGround()) {
         this.offGroundTicks = 0;
      } else {
         this.offGroundTicks++;
      }

      boolean jumpHeld = InputConstants.isKeyDown(mc.getWindow(), mc.options.keyJump.getKey().getValue());
      if (this.baseY == -1
         || this.baseY > (int)Math.floor(mc.player.getY()) - 1
         || mc.player.onGround()
         || !PlayerUtils.movementInput()
         || jumpHeld
         || this.mode.isCurrentMode("Normal")) {
         this.baseY = (int)Math.floor(mc.player.getY()) - 1;
      }

      this.updatePlacement();
      this.tellyPlaceDelayTimer = this.pos == null ? 0 : this.tellyPlaceDelayTimer + 1;

      boolean airborne = !mc.player.onGround();
      boolean knocked = airborne && (this.velocityDelay > 0 || mc.player.hurtTime > 0);
      boolean falling = airborne && mc.player.getDeltaMovement().y < -0.1;
      this.canBuildNow = true;
      if (this.clutch.getCurrentValue() && hasBlocks && (falling || knocked)) {
         if (this.pos == null && knocked) {
            this.findClutchTarget();
         }

         if (this.pos != null) {
            FallingPlayer sim = new FallingPlayer(mc.player);
            sim.calculate(2);
            double distanceToTarget = sim.getEyePos().distanceTo(Vec3.atCenterOf(this.pos.position()));
            if (this.pos.position().getY() > sim.y || distanceToTarget >= this.clutchDepth.getCurrentValue()) {
               this.canBuildNow = false;
            }
         } else if (knocked) {
            this.canBuildNow = false;
         }
      }

      if (mc.player.onGround()) {
         this.canBuildNow = true;
      }

      if (this.clutch.getCurrentValue() && hasBlocks && (!this.canBuildNow || this.velocityDelay > 0 || mc.player.hurtTime > 0) && this.rotationDelay <= 8) {
         Naven.skipTicks = Math.max(Naven.skipTicks, 1);
         this.usedSkipTicks = true;
         boolean useC06 = this.onTickRot.getCurrentValue() || !this.canBuildNow || this.pos == null;
         if (useC06) {
            this.aimBranch = "C06-real-cam";
            this.rots.set(mc.player.getYRot(), mc.player.getXRot());
            this.logLargeDelta(this.rots);
            this.rotationDelay++;
         } else {
            Vec3 targetVec = getFaceCenter(this.pos.position(), this.pos.facing());
            Vec3 predictedEye = new Vec3(
               mc.player.getX() + mc.player.getDeltaMovement().x,
               mc.player.getY() + mc.player.getEyeHeight() + mc.player.getDeltaMovement().y,
               mc.player.getZ() + mc.player.getDeltaMovement().z
            );
            Rotation rotationToBlock = new Rotation(predictedEye, targetVec);
            this.rots.set(rotationToBlock.getYaw(), rotationToBlock.getPitch());
            this.rotationDelay++;
            this.pendingTicks.add(() -> {
               float yaw = rotationToBlock.getYaw();
               if (yaw > -360.0F && yaw < 360.0F) {
                  yaw += 720.0F;
               }

               this.lastSentRot.set(rotationToBlock.getYaw(), rotationToBlock.getPitch());
               NetworkUtils.sendPacketNoEvent(
                  new ServerboundMovePlayerPacket.Rot(yaw, rotationToBlock.getPitch(), mc.player.onGround(), mc.player.horizontalCollision)
               );
               this.doSnap();
            });
         }
      } else {
         this.canBuildNow = true;
         this.clearSkipTicks();
         boolean wasInRescue = this.rotationDelay > 0;
         this.rotationDelay = 0;
         if (wasInRescue) {
            this.aimBranch = "resume-jitter";
            Vector2f resumeRot = new Vector2f(mc.player.getYRot() + rotationJitter(), mc.player.getXRot() + rotationJitter());
            this.logLargeDelta(resumeRot);
            NetworkUtils.sendPacketNoEvent(
               new ServerboundMovePlayerPacket.PosRot(
                  mc.player.getX(),
                  mc.player.getY(),
                  mc.player.getZ(),
                  resumeRot.getX(),
                  resumeRot.getY(),
                  mc.player.onGround(),
                  mc.player.horizontalCollision
               )
            );
         }

         if (this.onTickRot.getCurrentValue()) {
            this.aimBranch = "C06-real-cam";
            this.rots.set(mc.player.getYRot(), mc.player.getXRot());
            this.logLargeDelta(this.rots);
         } else {
            this.calculateTargetRotation();
         }

         if (this.mode.isCurrentMode("Telly Bridge")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || jumpHeld);
            if (this.airTicks < (int)this.tellyAirTicks.getCurrentValue() && PlayerUtils.movementInput()) {
               this.rots.setX(mc.player.getYRot());
               this.lastRots.set(this.rots.getX(), this.rots.getY());
               return;
            }
         } else if (this.mode.isCurrentMode("Keep Y")) {
            mc.options.keyJump.setDown(PlayerUtils.movementInput() || jumpHeld);
         } else {
            if (this.eagle.getCurrentValue()) {
               mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
            }

            if (this.sneak.getCurrentValue()) {
               this.lastSneakTicks++;
               if (this.lastSneakTicks == 18) {
                  if (mc.player.isSprinting()) {
                     mc.options.keySprint.setDown(false);
                     mc.player.setSprinting(false);
                  }

                  mc.options.keyShift.setDown(true);
               } else if (this.lastSneakTicks >= 21) {
                  mc.options.keyShift.setDown(false);
                  this.lastSneakTicks = 0;
               }
            }
         }
      }

      this.lastRots.set(this.rots.getX(), this.rots.getY());
   }

   @EventTarget
   public void onClick(EventClick e) {
      e.setCancelled(true);
      if (mc.screen == null && mc.player != null && this.pos != null) {
         if (!this.mode.isCurrentMode("Telly Bridge") || this.offGroundTicks >= 1) {
            if (this.checkPlace(this.pos)) {
               this.placeBlock();
            }
         }
      }
   }

   private void updatePlacement() {
      if (mc.player == null || mc.level == null) {
         return;
      }

      Vec3 eye = mc.player.getEyePosition();
      if (!this.canBuildNow) {
         eye = mc.player.getEyePosition().add(mc.player.getDeltaMovement().multiply(2.0, 2.0, 2.0));
      }

      if (this.clutch.getCurrentValue() && mc.player.getDeltaMovement().y < 0.01) {
         FallingPlayer sim = new FallingPlayer(mc.player);
         sim.calculate(2);
         eye = new Vec3(eye.x, Math.max(sim.getEyePos().y, eye.y), eye.z);
      }

      BlockPos belowFeet = BlockPos.containing(eye.x, (float)this.baseY + 0.1F, eye.z);
      int feetX = belowFeet.getX();
      int feetZ = belowFeet.getZ();
      if (mc.level.getBlockState(belowFeet).entityCanStandOn(mc.level, belowFeet, mc.player)) {
         return;
      }

      if (this.checkBlock(eye, belowFeet)) {
         return;
      }

      for (int radius = 1; radius <= 6; radius++) {
         if (this.checkBlock(eye, new BlockPos(feetX, this.baseY - radius, feetZ))) {
            return;
         }

         for (int x = 1; x <= radius; x++) {
            for (int z = 0; z <= radius - x; z++) {
               int yOff = radius - x - z;
               for (int signX = 0; signX <= 1; signX++) {
                  for (int signZ = 0; signZ <= 1; signZ++) {
                     BlockPos test = new BlockPos(
                        feetX + (signX == 0 ? x : -x),
                        this.baseY - yOff,
                        feetZ + (signZ == 0 ? z : -z)
                     );
                     if (this.checkBlock(eye, test)) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private void findClutchTarget() {
      if (mc.player == null || mc.level == null) {
         return;
      }

      BlockPos feet = mc.player.blockPosition();
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      for (int dx = -5; dx <= 5; dx++) {
         for (int dy = -5; dy <= 0; dy++) {
            for (int dz = -5; dz <= 5; dz++) {
               BlockPos candidate = feet.offset(dx, dy, dz);
               if (candidate.getY() >= feet.getY()) {
                  continue;
               }

               BlockState state = mc.level.getBlockState(candidate);
               if (state.isAir() || !state.getFluidState().isEmpty()) {
                  continue;
               }

               double distance = candidate.distSqr(feet);
               if (distance < bestDistance) {
                  bestDistance = distance;
                  best = candidate;
               }
            }
         }
      }

      if (best == null) {
         return;
      }

      for (Direction direction : Direction.values()) {
         BlockPos airPos = best.relative(direction);
         BlockState airState = mc.level.getBlockState(airPos);
         if (!airState.isAir() && !airState.canBeReplaced()) {
            continue;
         }

         if (mc.level.getBlockState(best).entityCanStandOnFace(mc.level, best, mc.player, direction)) {
            this.pos = new BlockPosWithFacing(best, direction.getOpposite());
            return;
         }
      }
   }

   private boolean checkBlock(Vec3 from, BlockPos bp) {
      if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
         return false;
      }

      Vec3 center = new Vec3(bp.getX() + 0.5, bp.getY() + 0.5F, bp.getZ() + 0.5);
      for (Direction direction : Direction.values()) {
         Vec3 hit = center.add(
            new Vec3(direction.getUnitVec3i().getX() * 0.5, direction.getUnitVec3i().getY() * 0.5, direction.getUnitVec3i().getZ() * 0.5)
         );
         Vec3i baseBlock = bp.offset(direction.getUnitVec3i());
         BlockPos offset = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
         if (mc.level.getBlockState(offset).entityCanStandOnFace(mc.level, offset, mc.player, direction)) {
            Vec3 relevant = hit.subtract(from);
            if (relevant.lengthSqr() <= 20.25
               && relevant.normalize().dot(Vec3.atLowerCornerOf(direction.getUnitVec3i()).normalize()) >= 0.0) {
               this.pos = new BlockPosWithFacing(offset, direction.getOpposite());
               return true;
            }
         }
      }

      return false;
   }

   private boolean isRotationValidForPlacement(Vector2f rotation) {
      if (this.pos == null || rotation == null) {
         return false;
      }

      HitResult result = RayTraceUtils.rayCast(1.0F, rotation);
      if (!(result instanceof BlockHitResult hit)) {
         return false;
      }

      return hit.getBlockPos().equals(this.pos.position()) && hit.getDirection() == this.pos.facing();
   }

   private boolean isPlacementReachable() {
      if (this.pos == null || mc.player == null) {
         return false;
      }

      Vec3 blockCenter = new Vec3(this.pos.position().getX() + 0.5, this.pos.position().getY() + 0.5, this.pos.position().getZ() + 0.5);
      Vec3 hitPoint = blockCenter.add(
         new Vec3(
            this.pos.facing().getUnitVec3i().getX() * 0.5,
            this.pos.facing().getUnitVec3i().getY() * 0.5,
            this.pos.facing().getUnitVec3i().getZ() * 0.5
         )
      );
      Vec3 eye = mc.player.getEyePosition();
      Vec3 toBlock = hitPoint.subtract(eye);
      return toBlock.lengthSqr() <= 20.25
         && toBlock.normalize().dot(Vec3.atLowerCornerOf(this.pos.facing().getUnitVec3i().multiply(-1)).normalize()) >= 0.0;
   }

   private void calculateTargetRotation() {
      if (this.pos == null || mc.player == null) {
         return;
      }

      float realYaw = mc.player.getYRot();
      if (mc.options.keyDown.isDown()) {
         realYaw += 180.0F;
         if (mc.options.keyLeft.isDown()) {
            realYaw += 45.0F;
         } else if (mc.options.keyRight.isDown()) {
            realYaw -= 45.0F;
         }
      } else if (mc.options.keyUp.isDown()) {
         if (mc.options.keyLeft.isDown()) {
            realYaw -= 45.0F;
         } else if (mc.options.keyRight.isDown()) {
            realYaw += 45.0F;
         }
      } else if (mc.options.keyRight.isDown()) {
         realYaw += 90.0F;
      } else if (mc.options.keyLeft.isDown()) {
         realYaw -= 90.0F;
      }

      float yaw = realYaw - 180.0F;
      float idealPitch = 75.5F;
      float currentPitch = this.rots.getY();
      this.correctRotation.set(yaw, idealPitch);

      this.aimBranch = "ideal-75.5";
      if (this.isRotationValidForPlacement(new Vector2f(yaw, idealPitch))) {
         this.applyRotationWithSpeed(yaw, idealPitch);
         return;
      }

      this.aimBranch = "keep-current";
      if (this.isRotationValidForPlacement(new Vector2f(yaw, currentPitch))) {
         this.applyRotationWithSpeed(yaw, currentPitch);
         return;
      }

      this.aimBranch = "nearest-from-current";
      Float nearest = this.findValidPitchNear(yaw, currentPitch);
      if (nearest != null) {
         this.applyRotationWithSpeed(yaw, nearest);
         return;
      }

      this.aimBranch = "nearest-to-ideal";
      Float towardIdeal = this.findValidPitchNear(yaw, idealPitch);
      if (towardIdeal != null) {
         this.applyRotationWithSpeed(yaw, towardIdeal);
         return;
      }

      this.aimBranch = "optimal-search";
      Vector2f optimal = this.findOptimalRotation(yaw);
      if (optimal != null) {
         this.applyRotationWithSpeed(optimal.getX(), optimal.getY());
         return;
      }

      this.aimBranch = "hold";
      this.applyRotationWithSpeed(yaw, currentPitch);
   }

   private Float findValidPitchNear(float yaw, float center) {
      if (this.pos == null) {
         return null;
      }

      for (float offset = 0.0F; offset <= 30.0F; offset += 0.3F) {
         float down = Mth.clamp(center - offset, -90.0F, 90.0F);
         if (this.isRotationValidForPlacement(new Vector2f(yaw, down))) {
            return down;
         }

         float up = center + offset;
         if (up <= 90.0F && this.isRotationValidForPlacement(new Vector2f(yaw, up))) {
            return up;
         }
      }

      return null;
   }

   private Vector2f findOptimalRotation(float baseYaw) {
      if (this.pos == null) {
         return null;
      }

      for (float yawOffset = 0.0F; yawOffset < 180.0F; yawOffset++) {
         float currentPitch = this.rots.getY();
         for (float pitchOffset = 0.0F; pitchOffset < 25.0F; pitchOffset++) {
            for (int i = 0; i < 2; i++) {
               float testPitch = Mth.clamp(currentPitch - pitchOffset * (i == 0 ? 1.0F : -1.0F), -90.0F, 90.0F);
               if (this.isRotationValidForPlacement(new Vector2f(baseYaw + yawOffset, testPitch))) {
                  return new Vector2f(baseYaw + yawOffset, testPitch);
               }

               if (this.isRotationValidForPlacement(new Vector2f(baseYaw - yawOffset, testPitch))) {
                  return new Vector2f(baseYaw - yawOffset, testPitch);
               }
            }
         }
      }

      return null;
   }

   private void applyRotationWithSpeed(float yaw, float pitch) {
      if (this.pos == null || mc.player == null) {
         return;
      }

      float speed;
      if (this.syncRotSpeed.getCurrentValue()) {
         float realYaw = mc.player.getYRot();
         float currentDiffToReal = Math.abs(Mth.wrapDegrees(this.rots.getX() - realYaw));
         float newDiffToReal = Math.abs(Mth.wrapDegrees(yaw - realYaw));
         speed = newDiffToReal < currentDiffToReal ? this.returnSpeed.getCurrentValue() : this.turnSpeed.getCurrentValue();
      } else {
         speed = this.rotationSpeed.getCurrentValue();
      }

      float targetYaw = yaw;
      float targetPitch = pitch;
      if (speed < 360.0F) {
         Vector2f smoothed = this.rotSmoother.tick(this.rots, new Vector2f(targetYaw, targetPitch), speed);
         targetYaw = smoothed.getX();
         targetPitch = smoothed.getY();
      }

      Vector2f quantized = RotationSmoother.patchConstantRotation(new Vector2f(targetYaw, targetPitch), this.lastSentRot);
      targetYaw = quantized.getX();
      targetPitch = quantized.getY();
      float jitterScale = this.jitter.getCurrentValue();
      if (jitterScale > 0.0F) {
         targetYaw += rotationJitter() * jitterScale;
         targetPitch = Mth.clamp(targetPitch + rotationJitter() * jitterScale, -90.0F, 90.0F);
      }

      Vector2f result = new Vector2f(targetYaw, targetPitch);
      this.logLargeDelta(result);
      this.rots.set(result.getX(), result.getY());
   }

   private void logLargeDelta(Vector2f rotation) {
   }

   private boolean hasPlaceableInHand() {
      if (mc.player == null) {
         return false;
      }

      return isValidStack(mc.player.getMainHandItem()) || isValidStack(mc.player.getOffhandItem());
   }

   private InteractionHand placementHand() {
      if (mc.player == null || isValidStack(mc.player.getMainHandItem())) {
         return InteractionHand.MAIN_HAND;
      }

      return isValidStack(mc.player.getOffhandItem()) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   private boolean shouldBuild() {
      if (mc.player == null || mc.level == null) {
         return false;
      }

      BlockPos below = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
      return mc.level.isEmptyBlock(below) && this.hasPlaceableInHand();
   }

   private float rotationJitter() {
      float offset = (float)(Math.random() * 0.04 + 0.03);
      return Math.random() > 0.5 ? offset : -offset;
   }

   private void clearSkipTicks() {
      if (this.usedSkipTicks) {
         Naven.skipTicks = 0;
         this.usedSkipTicks = false;
      }
   }

   private void c06Place() {
      if (this.pos == null || mc.player == null || mc.gameMode == null) {
         return;
      }

      Direction facing = this.pos.facing();
      if (facing == null) {
         return;
      }

      if (facing == Direction.UP
         && !mc.player.onGround()
         && PlayerUtils.movementInput()
         && !mc.options.keyJump.isDown()
         && !this.mode.isCurrentMode("Normal")) {
         return;
      }

      if (!this.hasPlaceableInHand()) {
         return;
      }

      InteractionHand hand = this.placementHand();
      Vec3 faceCenter = getFaceCenter(this.pos.position(), facing);
      Rotation targetRot = new Rotation(mc.player.getEyePosition(), faceCenter);
      if (!this.isRotationValidForPlacement(new Vector2f(targetRot.getYaw(), targetRot.getPitch()))) {
         return;
      }

      float origYaw = mc.player.getYRot();
      float origPitch = mc.player.getXRot();
      NetworkUtils.sendPacketNoEvent(
         new ServerboundMovePlayerPacket.PosRot(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            targetRot.getYaw() + rotationJitter(),
            targetRot.getPitch() + rotationJitter(),
            mc.player.onGround(),
            mc.player.horizontalCollision
         )
      );
      BlockHitResult hit = new BlockHitResult(getHitVec(this.pos.position(), facing), facing, this.pos.position(), false);
      InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hit);
      if (result == InteractionResult.SUCCESS) {
         mc.player.swing(hand);
      }

      NetworkUtils.sendPacketNoEvent(
         new ServerboundMovePlayerPacket.PosRot(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            origYaw + rotationJitter(),
            origPitch + rotationJitter(),
            mc.player.onGround(),
            mc.player.horizontalCollision
         )
      );
   }

   private void doSnap() {
      if (this.pos == null || mc.player == null || mc.gameMode == null) {
         return;
      }

      if (!this.hasPlaceableInHand()) {
         return;
      }

      InteractionHand hand = this.placementHand();
      if (!this.canBuildNow && this.clutch.getCurrentValue() && !this.pos.position().equals(this.lastC05Position)) {
         this.lastC05Position = this.pos.position();
         this.aimBranch = "c05-snap";
         Vec3 c05Target = getFaceCenter(this.pos.position(), this.pos.facing());
         Vec3 c05Eye = new Vec3(
            mc.player.getX() + mc.player.getDeltaMovement().x,
            mc.player.getY() + mc.player.getEyeHeight() + mc.player.getDeltaMovement().y,
            mc.player.getZ() + mc.player.getDeltaMovement().z
         );
         Rotation c05Rotation = new Rotation(c05Eye, c05Target);
         this.logLargeDelta(new Vector2f(c05Rotation.getYaw(), c05Rotation.getPitch()));
         float yaw = c05Rotation.getYaw();
         if (yaw > -360.0F && yaw < 360.0F) {
            yaw += 720.0F;
         }

         this.lastSentRot.set(c05Rotation.getYaw(), c05Rotation.getPitch());
         NetworkUtils.sendPacketNoEvent(
            new ServerboundMovePlayerPacket.Rot(yaw, c05Rotation.getPitch(), mc.player.onGround(), mc.player.horizontalCollision)
         );
      }

      Direction facing = this.pos.facing();
      if (facing == null) {
         return;
      }

      boolean jumpHeld = InputConstants.isKeyDown(mc.getWindow(), mc.options.keyJump.getKey().getValue());
      if (facing == Direction.UP
         && !mc.player.onGround()
         && PlayerUtils.movementInput()
         && !jumpHeld
         && !this.mode.isCurrentMode("Normal")) {
         return;
      }

      if (!this.shouldBuild()) {
         return;
      }

      if (!this.isRotationValidForPlacement(this.rots)) {
         return;
      }

      BlockHitResult hit = new BlockHitResult(getHitVec(this.pos.position(), facing), facing, this.pos.position(), false);
      InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hit);
      if (result == InteractionResult.SUCCESS) {
         mc.player.swing(hand);
      }
   }

   private boolean checkPlace(BlockPosWithFacing data) {
      Vec3 center = new Vec3(data.position().getX() + 0.5, data.position().getY() + 0.5F, data.position().getZ() + 0.5);
      Vec3 hit = center.add(
         new Vec3(
            data.facing().getUnitVec3i().getX() * 0.5,
            data.facing().getUnitVec3i().getY() * 0.5,
            data.facing().getUnitVec3i().getZ() * 0.5
         )
      );
      Vec3 relevant = hit.subtract(mc.player.getEyePosition());
      return relevant.lengthSqr() <= 20.25
         && relevant.normalize().dot(Vec3.atLowerCornerOf(data.facing().getUnitVec3i().multiply(-1)).normalize()) >= 0.0;
   }

   private void placeBlock() {
      if (this.pos == null || !isValidStack(mc.player.getMainHandItem())) {
         return;
      }

      Direction sbFace = this.pos.facing();
      boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow(), mc.options.keyJump.getKey().getValue());
      if (sbFace != null
         && (sbFace != Direction.UP
            || mc.player.onGround()
            || !PlayerUtils.movementInput()
            || isHoldingJump
            || this.mode.isCurrentMode("Normal"))
         && this.shouldBuild()) {
         InteractionResult result = mc.gameMode
            .useItemOn(
               mc.player,
               InteractionHand.MAIN_HAND,
               new BlockHitResult(getVec3(this.pos.position(), sbFace), sbFace, this.pos.position(), false)
            );
         if (result == InteractionResult.SUCCESS) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.pos = null;
         }
      }
   }

   public static Vec3 getVec3(BlockPos pos, Direction face) {
      double x = pos.getX() + 0.5;
      double y = pos.getY() + 0.5;
      double z = pos.getZ() + 0.5;
      if (face != Direction.UP && face != Direction.DOWN) {
         y += 0.08;
      } else {
         x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
         z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == Direction.WEST || face == Direction.EAST) {
         z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
      }

      if (face == Direction.SOUTH || face == Direction.NORTH) {
         x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
      }

      return new Vec3(x, y, z);
   }

   public static Vec3 getFaceCenter(BlockPos pos, Direction direction) {
      return new Vec3(
         pos.getX() + 0.5 + direction.getUnitVec3i().getX() * 0.5,
         pos.getY() + 0.5 + direction.getUnitVec3i().getY() * 0.5,
         pos.getZ() + 0.5 + direction.getUnitVec3i().getZ() * 0.5
      );
   }

   public static Vec3 getHitVec(BlockPos pos, Direction direction) {
      return getFaceCenter(pos, direction);
   }

   public static record BlockPosWithFacing(BlockPos position, Direction facing) {
   }

   private static final class RotationSmoother {
      private static final double TURN_RATIO = 0.5;
      private static final double RESP_UP = 0.8;
      private static final double RESP_DOWN = 0.4;
      private static final double WOBBLE = 0.2;
      private final Random random;
      private double velYaw;
      private double velPitch;

      private RotationSmoother(Random random) {
         this.random = random;
      }

      private Vector2f tick(Vector2f from, Vector2f to, double speed) {
         float yaw = from.getX() + (float)this.smoothAxisDelta(Mth.wrapDegrees(to.getX() - from.getX()), speed, true);
         float pitch = Mth.clamp(
            from.getY() + (float)this.smoothAxisDelta(to.getY() - from.getY(), speed, false), -90.0F, 90.0F
         );
         return new Vector2f(yaw, pitch);
      }

      private double smoothAxisDelta(double delta, double speed, boolean yawAxis) {
         double dist = Math.abs(delta);
         double targetVel = Math.max(speed, dist * 0.5);
         double prevVel = yawAxis ? this.velYaw : this.velPitch;
         double resp = prevVel < targetVel ? 0.8 : 0.4;
         double vel = prevVel + (targetVel - prevVel) * resp;
         vel *= 1.0 + (this.random.nextDouble() * 2.0 - 1.0) * 0.2;
         vel = Math.min(vel, dist);
         if (yawAxis) {
            this.velYaw = vel;
         } else {
            this.velPitch = vel;
         }

         return delta < 0.0 ? -vel : vel;
      }

      private void reset() {
         this.velYaw = 0.0;
         this.velPitch = 0.0;
      }

      private static double sensitivityStep() {
         double sensitivity = mc.options.sensitivity().get().floatValue() * 0.6 + 0.2;
         return sensitivity * sensitivity * sensitivity * 8.0 * 0.15;
      }

      private static Vector2f patchConstantRotation(Vector2f rotation, Vector2f anchor) {
         double step = sensitivityStep();
         float yaw = anchor.getX() + (float)(Math.round((rotation.getX() - anchor.getX()) / step) * step);
         float pitch = Mth.clamp(
            anchor.getY() + (float)(Math.round((rotation.getY() - anchor.getY()) / step) * step), -90.0F, 90.0F
         );
         return new Vector2f(yaw, pitch);
      }
   }
}
