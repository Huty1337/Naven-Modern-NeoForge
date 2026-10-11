package client.nilore.modules.impl.movement;

import client.nilore.modules.Category;
import client.nilore.modules.Module;
import client.nilore.settings.impl.BooleanSetting;

public class NoDelay
extends Module {
    public static NoDelay INSTANCE;
    public final BooleanSetting fastDig = new BooleanSetting("No Jump Delay", true);

    public NoDelay() {
        super("NoDelay", Category.MOVEMENT);
        INSTANCE = this;
    }
}