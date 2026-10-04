package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.ClickGUI;

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
