package client.nilore.modules.impl.render;

import client.nilore.modules.Category;
import client.nilore.modules.Module;

public class ItemTags extends Module {
    public static ItemTags INSTANCE;

    public ItemTags() {
        super("ItemTags", Category.RENDER);
        INSTANCE = this;
    }
}
