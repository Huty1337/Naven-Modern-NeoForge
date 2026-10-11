package tech.naven.modules.impl.render;

import tech.naven.modules.Category;
import tech.naven.modules.Module;
import tech.naven.modules.ModuleInfo;
import tech.naven.values.ValueBuilder;
import tech.naven.values.impl.BooleanValue;

@ModuleInfo(
   name = "NoRender",
   description = "Disables rendering",
   category = Category.RENDER
)
public class NoRender extends Module {
   public BooleanValue disableEffects = ValueBuilder.create(this, "Disable Effects").setDefaultBooleanValue(true).build().getBooleanValue();
}
