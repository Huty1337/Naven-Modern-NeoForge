package client.nilore.modules.impl.render;

import client.nilore.event.EventPriority;
import client.nilore.event.EventTarget;
import client.nilore.event.impl.CameraPitchEvent;
import client.nilore.modules.Category;
import client.nilore.modules.Module;
import client.nilore.settings.impl.BooleanSetting;
import client.nilore.settings.impl.ModeSetting;
import client.nilore.settings.impl.NumberSetting;

public class FakeAntiAim extends Module {

    public static FakeAntiAim INSTANCE;

    public static final String SPIN = "Spin";
    public static final String STATIC = "Static";
    public static final String NOT_MODIFIED = "NotModified";

    public static final float STATIC_LIMIT = 360.0f;

    public final BooleanSetting overrideRotation = new BooleanSetting("Override Rotation", false);

    public final ModeSetting entityYawMode = new ModeSetting("Entity Yaw", SPIN, STATIC, NOT_MODIFIED).withDefault(SPIN);
    public final NumberSetting entityYawValue = new NumberSetting("Entity Yaw Value", 10, -360, 360, 1,
            () -> !entityYawMode.is(NOT_MODIFIED));
    public final NumberSetting entityYawJitter = new NumberSetting("Entity Yaw Jitter", 0, -360, 360, 1);

    public final ModeSetting entityPitchMode = new ModeSetting("Entity Pitch", SPIN, STATIC, NOT_MODIFIED).withDefault(NOT_MODIFIED);
    public final NumberSetting entityPitchValue = new NumberSetting("Entity Pitch Value", 0, -360, 360, 1,
            () -> !entityPitchMode.is(NOT_MODIFIED));
    public final NumberSetting entityPitchJitter = new NumberSetting("Entity Pitch Jitter", 0, -360, 360, 1);

    public final ModeSetting headYawMode = new ModeSetting("Head Yaw", SPIN, STATIC, NOT_MODIFIED).withDefault(NOT_MODIFIED);
    public final NumberSetting headYawValue = new NumberSetting("Head Yaw Value", 0, -360, 360, 1,
            () -> !headYawMode.is(NOT_MODIFIED));
    public final NumberSetting headYawJitter = new NumberSetting("Head Yaw Jitter", 0, -360, 360, 1);

    public final ModeSetting headPitchMode = new ModeSetting("Head Pitch", SPIN, STATIC, NOT_MODIFIED).withDefault(NOT_MODIFIED);
    public final NumberSetting headPitchValue = new NumberSetting("Head Pitch Value", 0, -360, 360, 1,
            () -> !headPitchMode.is(NOT_MODIFIED));
    public final NumberSetting headPitchJitter = new NumberSetting("Head Pitch Jitter", 0, -360, 360, 1);

    public FakeAntiAim() {
        super("FakeAntiAim", Category.RENDER);
        INSTANCE = this;
    }

    public boolean isActive(ModeSetting mode) {
        return !mode.is(NOT_MODIFIED);
    }

    private boolean groupActive(ModeSetting mode, NumberSetting jitter) {
        return isActive(mode) || jitter.getValue().floatValue() != 0.0f;
    }

    private static int jitterSign(int tick) {
        return (tick % 2 == 0) ? 1 : -1;
    }

    private float groupAngle(ModeSetting mode, NumberSetting value, NumberSetting jitter, float time, int tick) {
        float base = isActive(mode) ? computeAngle(mode, value, time) : 0.0f;
        return base + jitterSign(tick) * jitter.getValue().floatValue();
    }

    public boolean entityYawActive()   { return groupActive(entityYawMode, entityYawJitter); }
    public boolean entityPitchActive() { return groupActive(entityPitchMode, entityPitchJitter); }
    public boolean headYawActive()     { return groupActive(headYawMode, headYawJitter); }
    public boolean headPitchActive()   { return groupActive(headPitchMode, headPitchJitter); }

    public float entityYawAngle(float time, int tick)   { return groupAngle(entityYawMode, entityYawValue, entityYawJitter, time, tick); }
    public float entityPitchAngle(float time, int tick) { return groupAngle(entityPitchMode, entityPitchValue, entityPitchJitter, time, tick); }

    public float computeAngle(ModeSetting mode, NumberSetting value, float time) {
        if (mode.is(SPIN)) return time * value.getValue().floatValue();
        if (mode.is(STATIC)) {
            float v = value.getValue().floatValue();
            return Math.max(-STATIC_LIMIT, Math.min(STATIC_LIMIT, v));
        }
        return 0.0f;
    }

    public float headYawOffset(float time, int tick) {
        return groupAngle(headYawMode, headYawValue, headYawJitter, time, tick);
    }

    public float headPitchOffset(float time, int tick) {
        return groupAngle(headPitchMode, headPitchValue, headPitchJitter, time, tick);
    }

    @EventTarget(value = EventPriority.LOWEST)
    public void onCameraPitch(CameraPitchEvent e) {
        if (!isEnabled() || mc.player == null || !headPitchActive()) return;
        int tick = mc.player.tickCount;
        e.setPitch(headPitchOffset(tick + mc.getFrameTime(), tick));
    }

    @Override
    protected void onDisable() {
        super.onDisable();
    }
}
