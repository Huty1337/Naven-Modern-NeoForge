package client.nilore.modules.impl.movement.speed.impl;

import client.nilore.event.EventTarget;
import client.nilore.event.impl.MotionEvent;
import client.nilore.modules.impl.movement.SpeedModule;
import client.nilore.modules.impl.movement.speed.SpeedMode;
import client.nilore.settings.impl.NumberSetting;
import client.nilore.utils.game.MovementUtil;

public class SpeedMotion extends SpeedMode {
    private final NumberSetting speed = new NumberSetting("Speed", 1.0, 1.0, 5.0, 0.1, () -> SpeedModule.is(this));

    public SpeedMotion() {
        super("Motion");
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;
        if (mc.player == null || mc.level == null) return;
        if (MovementUtil.isMoving() && mc.player.onGround()) {
            MovementUtil.strafeForward(speed.getValue().doubleValue() / 12);
            mc.player.jumpFromGround();
        }
    }
}
