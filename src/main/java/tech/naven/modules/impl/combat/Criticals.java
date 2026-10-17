package tech.naven.modules.impl.combat;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.api.types.Priority;
import tech.naven.events.impl.EventAttack;
import tech.naven.events.impl.EventMoveInput;
import tech.naven.events.impl.EventMotion;
import tech.naven.events.impl.EventRespawn;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;
import tech.naven.values.impl.FloatValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.shapes.VoxelShape;

@ModuleInfo(
   name = "Criticals",
   description = "Critical Hit Target",
   category = Category.COMBAT
)
public class Criticals extends Module {
   FloatValue range = ValueBuilder.create(this, "Critical Range")
      .setDefaultFloatValue(3.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(3.2F)
      .build()
      .getFloatValue();

   private final BooleanValue skipTicksOption = ValueBuilder.create(this, "SkipTicks")
      .setDefaultBooleanValue(true)
      .build()
      .getBooleanValue();

   private boolean usedSkipTicks;

   @Override
   public void onDisable() {
      this.reset();
   }

   @EventTarget
   public void onRespawn(EventRespawn event) {
      this.reset();
   }

   @EventTarget(Priority.LOWEST)
   public void onMoveInputSprintCancel(EventMoveInput event) {
      if (!this.isEnabled() || mc.player == null || mc.level == null || mc.options == null) {
         return;
      }

      Entity target = Aura.target;
      if (target == null) {
         return;
      }

      if (this.hasHeadBlock()) {
         this.reset();
         return;
      }

      if (this.cantCrit(target)) {
         this.reset();
         return;
      }

      Aura aura = (Aura)Naven.getInstance().getModuleManager().getModule(Aura.class);
      if (aura == null || !aura.isEnabled()) {
         this.reset();
         return;
      }

      if (mc.player.getDeltaMovement().y < 0.0
         && !mc.player.onGround()
         && mc.player.distanceTo(target) <= this.range.getCurrentValue()) {
         this.forceCancelSprint();
      }
   }

   @EventTarget(Priority.HIGH)
   public void onMotion(EventMotion event) {
      if (event.getType() != EventType.PRE) {
         return;
      }

      if (Aura.target == null) {
         return;
      }

      if (this.hasHeadBlock()) {
         this.reset();
         return;
      }

      if (this.cantCrit(Aura.target)) {
         this.reset();
      } else {
         Aura aura = (Aura)Naven.getInstance().getModuleManager().getModule(Aura.class);
         if (Aura.target != null) {
            if (!this.isNull()
               && mc.player.getDeltaMovement().y < 0.0
               && !mc.player.onGround()
               && aura != null
               && aura.isEnabled()
               && mc.player.distanceTo(Aura.target) <= this.range.getCurrentValue()) {
               if (this.skipTicksOption.getCurrentValue() && Naven.skipTicks <= 0) {
                  Naven.skipTicks++;
                  this.usedSkipTicks = true;
               }
            } else if (!this.isNull() && (aura == null || !aura.isEnabled())) {
               this.reset();
            }
         }
      }
   }

   @EventTarget
   public void onAttack(EventAttack event) {
      if (!this.isEnabled() || mc.player == null || mc.level == null) {
         return;
      }

      if (Aura.target == null) {
         return;
      }

      if (event.getTarget() != Aura.target) {
         return;
      }

      if (this.hasHeadBlock()) {
         this.reset();
         return;
      }

      Aura aura = (Aura)Naven.getInstance().getModuleManager().getModule(Aura.class);
      if (aura != null
         && aura.isEnabled()
         && mc.player.getDeltaMovement().y < 0.0
         && !mc.player.onGround()
         && mc.player.distanceTo(Aura.target) <= this.range.getCurrentValue()) {
         this.forceCancelSprint();
      }

      if (this.cantCrit(Aura.target)) {
         this.reset();
      }
   }

   private boolean cantCrit(Entity target) {
      if (mc.player == null) {
         return true;
      }

      if (!(target instanceof LivingEntity living)) {
         return true;
      }

      return mc.player.onClimbable()
         || mc.player.isInWater()
         || mc.player.isInLava()
         || mc.player.isPassenger()
         || this.hasHeadBlock()
         || living.hurtTime > 10
         || living.getHealth() <= 0.0F;
   }

   private boolean hasHeadBlock() {
      if (mc.player == null || mc.level == null) {
         return false;
      }

      Iterable<VoxelShape> collisions = mc.level.getBlockCollisions(mc.player, mc.player.getBoundingBox().move(0.0, 0.5, 0.0));
      return collisions.iterator().hasNext();
   }

   private boolean isNull() {
      return mc.player == null || mc.level == null;
   }

   private void forceCancelSprint() {
      if (mc.player == null || mc.options == null) {
         return;
      }

      if (mc.player.isSprinting() || mc.options.keySprint.isDown()) {
         mc.options.keySprint.setDown(false);
         mc.player.setSprinting(false);
      }
   }

   private void reset() {
      if (this.usedSkipTicks) {
         Naven.skipTicks = 0;
         this.usedSkipTicks = false;
      }
   }
}
