package client.nilore.modules.impl.render.nametag;

import java.util.HashMap;
import client.nilore.ClientBase;
import client.nilore.event.impl.PacketEvent;
import client.nilore.event.impl.Render2DEvent;
import client.nilore.event.impl.RenderEvent;
import client.nilore.modules.impl.render.nametag.OpalNameTag;
import client.nilore.modules.impl.render.nametag.SimpleNameTag;

public abstract class NameTagStyle
extends ClientBase {
    private final String styleName;
    private static final HashMap<Class<? extends NameTagStyle>, NameTagStyle> styleRegistry = new HashMap<>();

    protected NameTagStyle(String string) {
        this.styleName = string;
    }

    public static void registerStyles() {
        styleRegistry.put(OpalNameTag.class, new OpalNameTag());
        styleRegistry.put(SimpleNameTag.class, new SimpleNameTag());
    }

    public static NameTagStyle getByName(String string) {
        return styleRegistry.values().stream().filter(nameTagStyle -> nameTagStyle.styleName.equals(string)).findFirst().orElse(null);
    }

    public abstract void onEnable();

    public abstract void onDisable();

    public abstract String getName();

    public abstract void onRender(RenderEvent var1);

    public abstract void onRender2D(Render2DEvent var1);

    public abstract void onPacket(PacketEvent var1);
}