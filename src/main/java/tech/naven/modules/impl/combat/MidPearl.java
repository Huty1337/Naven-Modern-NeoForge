package tech.naven.modules.impl.combat;

import tech.mixin.accessors.MultiPlayerGameModeAccessor;
import tech.naven.events.api.EventTarget;
import tech.naven.events.api.types.EventType;
import tech.naven.events.impl.EventRunTicks;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.utils.MathUtils;
import tech.naven.utils.TimeHelper;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.FloatValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(
   name = "MidPearl",
   description = "Middle-click to throw an ender pearl",
   category = Category.COMBAT
)
public class MidPearl extends Module {
   private final FloatValue minDelay = ValueBuilder.create(this, "Min Switch Delay")
      .setDefaultFloatValue(0.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(1000.0F)
      .setFloatStep(50.0F)
      .build()
      .getFloatValue();
   private final FloatValue maxDelay = ValueBuilder.create(this, "Max Switch Delay")
      .setDefaultFloatValue(5.0F)
      .setMinFloatValue(0.0F)
      .setMaxFloatValue(1000.0F)
      .setFloatStep(50.0F)
      .build()
      .getFloatValue();
   private final TimeHelper timer = new TimeHelper();
   private boolean doWork;
   private boolean switchBack;
   private int lastSlot = -1;

   @Override
   public void onEnable() {
      this.lastSlot = -1;
      this.doWork = false;
      this.switchBack = false;
      this.timer.reset();
   }

   @Override
   public void onDisable() {
      if (this.switchBack && mc.player != null && this.lastSlot >= 0) {
         mc.player.getInventory().setSelectedSlot(this.lastSlot);
      }

      this.switchBack = false;
      this.doWork = false;
      this.lastSlot = -1;
   }

   @EventTarget
   public void onTick(EventRunTicks event) {
      if (event.getType() != EventType.PRE) {
         return;
      }

      if (mc.player == null || mc.level == null || mc.gameMode == null || !mc.player.isAlive()) {
         return;
      }

      if (mc.options.keyPickItem.isDown()) {
         this.timer.reset();
         this.doWork = true;
         this.switchBack = false;
      }

      if (this.doWork && this.timer.delay(this.randomDelay())) {
         int pearlSlot = this.findPearlSlot();
         if (pearlSlot != -1) {
            this.lastSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(pearlSlot);
            ((MultiPlayerGameModeAccessor)mc.gameMode).invokeEnsureHasSentCarriedItem();
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            this.timer.reset();
            this.doWork = false;
            this.switchBack = true;
         }
      }

      if (this.switchBack && this.timer.delay(this.randomDelay())) {
         if (this.lastSlot >= 0) {
            mc.player.getInventory().setSelectedSlot(this.lastSlot);
         }

         this.switchBack = false;
      }
   }

   private double randomDelay() {
      float min = this.minDelay.getCurrentValue();
      float max = this.maxDelay.getCurrentValue();
      if (max < min) {
         max = min;
      }

      return MathUtils.getRandomDoubleInRange((double)min, (double)max);
   }

   private int findPearlSlot() {
      if (mc.player == null) {
         return -1;
      }

      for (int i = 0; i <= 8; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.getItem() == Items.ENDER_PEARL) {
            return i;
         }
      }

      return -1;
   }
}
