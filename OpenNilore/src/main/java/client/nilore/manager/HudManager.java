package client.nilore.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import client.nilore.ClientBase;
import client.nilore.NiloreClient;
import client.nilore.event.impl.GlRenderEvent;
import client.nilore.event.impl.Render2DEvent;
import client.nilore.event.impl.TickEvent;
import client.nilore.gui.IntroAnimation;
import client.nilore.hud.HudElement;
import client.nilore.hud.KeyBindsHud;
import client.nilore.hud.LieDetector;
import client.nilore.hud.MusicPlayerHud;
import client.nilore.hud.ModuleListHud;
import client.nilore.hud.NotificationHud;
import client.nilore.hud.PlayerListHud;
import client.nilore.hud.PotionEffectsHud;
import client.nilore.hud.ScoreboardHud;
import client.nilore.hud.TargetHud;
import client.nilore.modules.impl.render.Armor;
import client.nilore.event.EventTarget;

public class HudManager {
    private final Map<String, HudElement> hudElements = new HashMap<>();

    public HudManager() {
        this.init();
    }

    public void init() {
        this.registerHudElement(new TargetHud());
        this.registerHudElement(new KeyBindsHud());
        this.registerHudElement(new ModuleListHud());
        this.registerHudElement(new PlayerListHud());
        this.registerHudElement(new PotionEffectsHud());
        this.registerHudElement(new LieDetector());
        this.registerHudElement(new MusicPlayerHud());
        this.registerHudElement(new NotificationHud());
        this.registerHudElement(new Armor());
        this.registerHudElement(new ScoreboardHud());
    }

    private void registerHudElement(HudElement hudElement) {
        NiloreClient.getInstance().getModuleManager().register(hudElement);
        this.hudElements.put(hudElement.getClass().getSimpleName(), hudElement);
    }

    public <T extends HudElement> T getHudElement(Class<T> clazz) {
        return clazz.cast(this.hudElements.get(clazz.getSimpleName()));
    }

    public HudElement getHudElementByName(String string) {
        return this.hudElements.values().stream().filter(hudElement -> hudElement.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
    }

    public Collection<HudElement> getHudElements() {
        return this.hudElements.values();
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (ClientBase.mc.screen == null) {
            try {
                for (HudElement hudElement : NiloreClient.getInstance().getHudManager().getHudElements()) {
                    hudElement.stopDragging();
                }
            } catch (Exception exception) {
                ClientBase.logger.error(exception);
                ClientBase.logger.error(exception.getMessage());
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {
        if (IntroAnimation.isRunning()) {
            return;
        }
        for (HudElement hudElement : this.getHudElements()) {
            if (!hudElement.isEnabled()) continue;
            hudElement.onRender2D(render2DEvent, hudElement.getX(), hudElement.getY());
        }
    }

    @EventTarget
    public void onGlRender(GlRenderEvent glRenderEvent) {
        if (IntroAnimation.isRunning()) {
            return;
        }
        for (HudElement hudElement : this.getHudElements()) {
            if (!hudElement.isEnabled()) continue;
            hudElement.onGlRender(glRenderEvent, hudElement.getX(), hudElement.getY());
        }
    }
}