package client.nilore.modules.impl.render;

import net.minecraft.world.level.block.Block;
import client.nilore.modules.Category;
import client.nilore.modules.Module;

public class XRay extends Module {
    public static XRay INSTANCE;

    public XRay() {
        super("XRay", Category.RENDER);
        INSTANCE = this;
    }

    public boolean isXrayVisible(Block block) {
        // TODO: implement actual XRay visibility logic (ore detection, etc.)
        return true;
    }
}
