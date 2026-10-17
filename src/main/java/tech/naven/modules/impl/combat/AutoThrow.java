package tech.naven.modules.impl.combat;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.impl.EventUpdate;
import tech.naven.events.impl.EventUpdateHeldItem;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.misc.AutoHelper;
import tech.naven.modules.impl.misc.PearlPrediction;
import tech.naven.modules.impl.misc.Teams;
import tech.naven.modules.impl.move.AutoMLG;
import tech.naven.modules.impl.move.Blink;
import tech.naven.modules.impl.move.LongJump;
import tech.naven.modules.impl.move.Scaffold;
import tech.naven.modules.impl.move.Stuck;
import tech.naven.utils.BlinkingPlayer;
import tech.naven.utils.FriendManager;
import tech.naven.utils.InventoryUtils;
import tech.naven.utils.TimeHelper;
import tech.naven.utils.Vector2f;
import tech.naven.utils.rotation.RayTraceUtil;
import tech.naven.utils.rotation.Rotation;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.FloatValue;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;

@ModuleInfo(
   name = "AutoThrow",
   description = "Automatically throw snowballs and eggs.",
   category = Category.COMBAT
)
public class AutoThrow extends Module {
   private final FloatValue minDistance = ValueBuilder.create(this, "Min Distance")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(10.0F)
      .build()
      .getFloatValue();
   private final FloatValue maxDistance = ValueBuilder.create(this, "Max Distance")
      .setDefaultFloatValue(8.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(3.0F)
      .setMaxFloatValue(30.0F)
      .build()
      .getFloatValue();
   private final FloatValue delay = ValueBuilder.create(this, "Delay")
      .setDefaultFloatValue(500.0F)
      .setFloatStep(50.0F)
      .setMinFloatValue(50.0F)
      .setMaxFloatValue(2000.0F)
      .build()
      .getFloatValue();
   private final FloatValue predict = ValueBuilder.create(this, "Predict")
      .setDefaultFloatValue(0.8F)
      .setFloatStep(0.1F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(2.0F)
      .build()
      .getFloatValue();

   private final TimeHelper timer = new TimeHelper();
   public Vector2f rotation;
   private int originalSlot;
   private boolean hasThrown = false;
   private InteractionHand pendingThrowHand;

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (!this.isEnabled() || mc.player == null || mc.level == null) {
         return;
      }

      if (mc.screen != null) {
         this.resetState();
         return;
      }

      if (this.isMiningBlock()) {
         this.cancelThrow(false);
         return;
      }

      if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
         this.cancelThrow(false);
         return;
      }

      ItemStack heldItem = mc.player.getMainHandItem();
      if (heldItem.is(Items.CROSSBOW)) {
         this.resetState();
         return;
      }

      Scaffold scaffold = (Scaffold)Naven.getInstance().getModuleManager().getModule(Scaffold.class);
      Stuck stuck = (Stuck)Naven.getInstance().getModuleManager().getModule(Stuck.class);
      Blink blink = (Blink)Naven.getInstance().getModuleManager().getModule(Blink.class);
      AutoMLG autoMLG = (AutoMLG)Naven.getInstance().getModuleManager().getModule(AutoMLG.class);
      LongJump longJump = (LongJump)Naven.getInstance().getModuleManager().getModule(LongJump.class);
      AttackCrystal attackCrystal = (AttackCrystal)Naven.getInstance().getModuleManager().getModule(AttackCrystal.class);
      Aura aura = (Aura)Naven.getInstance().getModuleManager().getModule(Aura.class);
      AutoHelper autoHelper = (AutoHelper)Naven.getInstance().getModuleManager().getModule(AutoHelper.class);
      PearlPrediction pearlPrediction = (PearlPrediction)Naven.getInstance().getModuleManager().getModule(PearlPrediction.class);
      if (scaffold != null && scaffold.isEnabled() || stuck != null && stuck.isEnabled() || blink != null && blink.isEnabled()) {
         this.cancelThrow(false);
         return;
      }

      if (autoMLG != null && autoMLG.isEnabled() && autoMLG.rotation
         || longJump != null && longJump.isEnabled() && LongJump.rotation != null
         || attackCrystal != null && attackCrystal.isEnabled() && AttackCrystal.rotations != null
         || aura != null && aura.isEnabled() && Aura.target != null && Aura.rotation != null
         || autoHelper != null && autoHelper.isEnabled() && autoHelper.rotation != null
         || pearlPrediction != null && pearlPrediction.isEnabled() && PearlPrediction.isThrowing && PearlPrediction.rotations != null) {
         this.cancelThrow(false);
         return;
      }

      ItemStack offhandItem = mc.player.getOffhandItem();
      if (offhandItem.is(Items.ENCHANTED_GOLDEN_APPLE) || offhandItem.is(Items.GOLDEN_APPLE)) {
         if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            this.cancelThrow(false);
            return;
         }
      }

      if (this.pendingThrowHand != null) {
         if (!this.timer.delay(this.delay.getCurrentValue())) {
            this.cancelThrow(false);
            return;
         }

         Optional<AbstractClientPlayer> targetOpt = this.getTarget();
         if (targetOpt.isEmpty() || this.rotation == null) {
            this.cancelThrow(false);
            return;
         }

         Entity target = targetOpt.get();
         double targetDistance = mc.player.distanceTo(target);
         if (this.isThrowBlocked(this.rotation, Math.min(3.0, targetDistance))) {
            this.cancelThrow(false);
            return;
         }

         if (this.pendingThrowHand == InteractionHand.MAIN_HAND && !isThrowableItem(mc.player.getMainHandItem())) {
            this.cancelThrow(false);
            return;
         }

         if (this.pendingThrowHand == InteractionHand.OFF_HAND && !isThrowableItem(offhandItem)) {
            this.cancelThrow(false);
            return;
         }

         mc.gameMode.useItem(mc.player, this.pendingThrowHand);
         this.sendServerSwing(this.pendingThrowHand);
         this.timer.reset();
         this.rotation = null;
         this.pendingThrowHand = null;
         return;
      }

      boolean mainHandThrowable = isThrowableItem(heldItem);
      boolean offHandThrowable = isThrowableItem(offhandItem);
      boolean useOffHand = !mainHandThrowable && offHandThrowable;
      int throwableSlot = -1;
      if (!useOffHand) {
         throwableSlot = mainHandThrowable ? mc.player.getInventory().getSelectedSlot() : this.getThrowableSlot();
         if (throwableSlot == -1) {
            this.cancelThrow(false);
            return;
         }
      } else if (this.hasThrown) {
         mc.player.getInventory().setSelectedSlot(this.originalSlot);
         this.hasThrown = false;
      }

      if (!this.timer.delay(this.delay.getCurrentValue())) {
         this.cancelThrow(false);
         return;
      }

      Optional<AbstractClientPlayer> targetOpt = this.getTarget();
      if (targetOpt.isEmpty()) {
         this.cancelThrow(false);
         return;
      }

      if (!useOffHand && !canUseMainHand(heldItem)) {
         this.cancelThrow(false);
         return;
      }

      Entity target = targetOpt.get();
      Vector2f calcRotations = this.getRotationToEntity(target);
      if (calcRotations == null) {
         this.cancelThrow(false);
         return;
      }

      calcRotations.y = Math.max(-90.0F, Math.min(90.0F, calcRotations.y));
      double targetDistance = mc.player.distanceTo(target);
      if (this.isThrowBlocked(calcRotations, Math.min(3.0, targetDistance))) {
         this.cancelThrow(false);
         return;
      }

      this.rotation = calcRotations;
      if (useOffHand) {
         this.pendingThrowHand = InteractionHand.OFF_HAND;
         return;
      }

      if (!this.hasThrown) {
         if (mc.player.getInventory().getSelectedSlot() == throwableSlot) {
            this.pendingThrowHand = InteractionHand.MAIN_HAND;
            return;
         }

         this.originalSlot = mc.player.getInventory().getSelectedSlot();
         mc.player.getInventory().setSelectedSlot(throwableSlot);
         this.hasThrown = true;
         return;
      }

      if (mc.player.getInventory().getSelectedSlot() != throwableSlot) {
         this.cancelThrow(false);
         return;
      }

      if (isThrowableItem(mc.player.getMainHandItem())) {
         mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         this.sendServerSwing(InteractionHand.MAIN_HAND);
         this.timer.reset();
      }

      mc.player.getInventory().setSelectedSlot(this.originalSlot);
      this.hasThrown = false;
   }

   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      if (this.hasThrown && e.getHand() == InteractionHand.MAIN_HAND && mc.player != null) {
         e.setItem(mc.player.getInventory().getItem(this.originalSlot));
      }
   }

   private Vector2f getRotationToEntity(Entity target) {
      if (this.isFakePlayer(target)) {
         return null;
      }

      double distanceToEnt = mc.player.distanceTo(target);
      double predictTicks = distanceToEnt * this.predict.getCurrentValue();
      double predictX = target.getX() + (target.getX() - target.xOld) * predictTicks;
      double predictY = target.getY() + (target.getY() - target.yOld) * predictTicks;
      double predictZ = target.getZ() + (target.getZ() - target.zOld) * predictTicks;
      double x = predictX - mc.player.getX();
      double h = predictY + 1.2 - (mc.player.getY() + mc.player.getEyeHeight());
      double z = predictZ - mc.player.getZ();
      double h1 = Math.sqrt(x * x + z * z);
      float yaw = (float)(Math.atan2(z, x) * 180.0 / Math.PI) - 90.0F;
      float pitch = -this.getTrajectoryAngle((float)h1, (float)h, 0.6F, 0.006F);
      return new Vector2f(yaw, pitch);
   }

   private float getTrajectoryAngle(float dist, float height, float velocity, float gravity) {
      float sqrt = velocity * velocity * velocity * velocity
         - gravity * (gravity * dist * dist + 2.0F * height * velocity * velocity);
      if (sqrt < 0.0F) {
         return 0.0F;
      }

      return (float)Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(sqrt)) / (gravity * dist)));
   }

   public static boolean isThrowableItem(ItemStack stack) {
      return stack.is(Items.EGG) || stack.is(Items.SNOWBALL);
   }

   private static boolean canUseMainHand(ItemStack heldItem) {
      return heldItem.isEmpty()
         || !heldItem.is(Items.ENDER_PEARL) && !heldItem.is(Items.BOW) && !heldItem.has(DataComponents.FOOD) && !isPotionItem(heldItem);
   }

   private static boolean isPotionItem(ItemStack stack) {
      return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
   }

   private int getThrowableSlot() {
      int eggSlot = InventoryUtils.getItemSlot(Items.EGG);
      if (eggSlot != -1 && eggSlot < 9) {
         return eggSlot;
      }

      int snowballSlot = InventoryUtils.getItemSlot(Items.SNOWBALL);
      return snowballSlot != -1 && snowballSlot < 9 ? snowballSlot : -1;
   }

   private boolean isThrowBlocked(Vector2f rotations, double range) {
      HitResult hitResult = RayTraceUtil.rayCast(new Rotation(rotations.getX(), rotations.getY()), range, 0.0F, mc.player, null, false);
      return hitResult != null && hitResult.getType() == HitResult.Type.BLOCK;
   }

   private boolean isMiningBlock() {
      if (mc.gameMode == null) {
         return false;
      }

      return mc.gameMode.isDestroying()
         || mc.options.keyAttack.isDown() && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
   }

   private Optional<AbstractClientPlayer> getTarget() {
      return mc.level
         .players()
         .stream()
         .filter(e -> e != mc.player)
         .filter(e -> !Teams.isSameTeam(e))
         .filter(e -> !FriendManager.isFriend(e))
         .filter(e -> !this.isBot(e))
         .filter(e -> !this.isFakePlayer(e))
         .filter(e -> {
            double distance = mc.player.distanceTo(e);
            return distance <= this.maxDistance.getCurrentValue() && distance >= this.minDistance.getCurrentValue();
         })
         .filter(mc.player::hasLineOfSight)
         .filter(e -> !e.isInvisible())
         .min(Comparator.comparingDouble(e1 -> mc.player.distanceTo(e1)));
   }

   private boolean isBot(Player player) {
      return AntiBots.isBot(player);
   }

   private boolean isFakePlayer(Entity entity) {
      if (entity instanceof BlinkingPlayer) {
         return true;
      }

      return entity instanceof RemotePlayer ? entity.getId() < 0 : false;
   }

   private void sendServerSwing(InteractionHand hand) {
      if (mc.getConnection() != null) {
         mc.getConnection().send(new ServerboundSwingPacket(hand));
      }
   }

   private void resetState() {
      this.rotation = null;
      if (this.hasThrown && mc.player != null) {
         mc.player.getInventory().setSelectedSlot(this.originalSlot);
         this.hasThrown = false;
      }

      this.pendingThrowHand = null;
      this.timer.reset();
   }

   private void cancelThrow(boolean resetTimer) {
      this.rotation = null;
      if (this.hasThrown && mc.player != null) {
         mc.player.getInventory().setSelectedSlot(this.originalSlot);
         this.hasThrown = false;
      }

      this.pendingThrowHand = null;
      if (resetTimer) {
         this.timer.reset();
      }
   }

   @Override
   public void onEnable() {
      this.rotation = null;
      this.hasThrown = false;
      this.pendingThrowHand = null;
      this.timer.reset();
   }

   @Override
   public void onDisable() {
      this.resetState();
   }
}
