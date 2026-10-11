package client.nilore.modules.impl.movement;

import client.nilore.event.impl.SneakEvent;
import client.nilore.modules.Category;
import client.nilore.modules.Module;
import client.nilore.event.EventTarget;

public class NoPush
extends Module {
    public NoPush() {
        super("NoPush", Category.MOVEMENT);
    }

    @EventTarget
    public void onSneak(SneakEvent sneakEvent) {
        if (!FireballBlink.INSTANCE.isEnabled()) {
            sneakEvent.setCancelled(true);
        }
    }
}