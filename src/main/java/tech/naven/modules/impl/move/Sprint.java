package tech.naven.modules.impl.move;

import tech.naven.Naven;
import tech.naven.events.api.EventTarget;
import tech.naven.events.impl.EventSprint;
import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.modules.impl.combat.Aura;
import tech.naven.modules.impl.misc.InventoryCleaner;
import tech.naven.utils.InventoryUtils;

@ModuleInfo(
   name = "Sprint",
   description = "Automatically sprints",
   category = Category.MOVEMENT
)
public class Sprint extends Module {
   @EventTarget
   public void onSprint(EventSprint event) {
      if (mc.player == null || InventoryUtils.shouldDisableFeatures()) {
         return;
      }

      InventoryCleaner inventory = (InventoryCleaner)Naven.getInstance().getModuleManager().getModule(InventoryCleaner.class);
      if (inventory.isPerformingSilentAction()) {
         return;
      }

      Aura aura = (Aura)Naven.getInstance().getModuleManager().getModule(Aura.class);
      if (Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled() || aura.shouldStopSprint()) {
         mc.options.keySprint.setDown(false);
         mc.player.setSprinting(false);
         aura.markSprintReleased();
         return;
      }

      if (aura.isSprintReleased()) {
         mc.options.keySprint.setDown(false);
         mc.player.setSprinting(false);
         return;
      }

      mc.options.toggleSprint().set(false);
      mc.options.keySprint.setDown(true);
      if (mc.player.input.getMoveVector().y != 0.0F) {
         mc.player.setSprinting(true);
      }
   }

   @Override
   public void onDisable() {
      mc.options.keySprint.setDown(false);
   }
}
