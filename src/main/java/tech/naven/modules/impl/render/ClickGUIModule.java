package tech.naven.modules.impl.render;

import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.ui.ClickGUI;

@ModuleInfo(
   name = "ClickGUI",
   category = Category.RENDER,
   description = "The ClickGUI"
)
public class ClickGUIModule extends Module {
   ClickGUI clickGUI = null;

   @Override
   protected void initModule() {
      super.initModule();
      this.setKey(344);
   }

   @Override
   public void onEnable() {
      if (this.clickGUI == null) {
         this.clickGUI = new ClickGUI();
      }

      mc.setScreen(this.clickGUI);
      this.toggle();
   }
}
