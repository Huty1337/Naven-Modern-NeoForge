package client.nilore.modules.impl.render;

import client.nilore.modules.Category;
import client.nilore.modules.Module;
import client.nilore.settings.impl.NumberSetting;

public class AspectRatio
extends Module {
    public static AspectRatio INSTANCE;
    public final NumberSetting ratioSetting = new NumberSetting("Ratio", 1.78f, 0.1f, 5.0f, 0.1f);

    public AspectRatio() {
        super("AspectRatio", Category.RENDER);
        INSTANCE = this;
    }
}