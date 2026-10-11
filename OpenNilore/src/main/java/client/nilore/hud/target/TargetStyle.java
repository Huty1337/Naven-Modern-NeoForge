package client.nilore.hud.target;

import java.util.HashMap;
import net.minecraft.world.entity.LivingEntity;
import client.nilore.ClientBase;
import client.nilore.event.impl.Render2DEvent;
import client.nilore.utils.animation.SmoothAnimationTimer;

public abstract class TargetStyle
extends ClientBase {
    private final String name;
    private static final HashMap<Class<? extends TargetStyle>, TargetStyle> registry = new HashMap<>();

    protected TargetStyle(String string) {
        this.name = string;
    }

    public String getName() {
        return this.name;
    }

    public abstract void render(Render2DEvent var1, LivingEntity var2, SmoothAnimationTimer var3, SmoothAnimationTimer var4, float var5, float var6, float var7);

    public static void initStyles() {
        if (!registry.isEmpty()) {
            return;
        }
        registry.put(RoundTargetStyle.class, new RoundTargetStyle());
        registry.put(MoonTargetStyle.class, new MoonTargetStyle());
        registry.put(RiseTargetStyle.class, new RiseTargetStyle());
        registry.put(SimpleTargetStyle.class, new SimpleTargetStyle());
        registry.put(NavenTargetStyle.class, new NavenTargetStyle());
        registry.put(ExhibitionTargetStyle.class, new ExhibitionTargetStyle());
    }

    public static TargetStyle getByName(String string) {
        return registry.values().stream().filter(targetStyle -> targetStyle.name.equals(string)).findFirst().orElse(null);
    }
}