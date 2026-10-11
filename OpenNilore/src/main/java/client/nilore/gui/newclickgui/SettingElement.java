package client.nilore.gui.newclickgui;

import lombok.Getter;
import lombok.Generated;
import client.nilore.gui.newclickgui.CategoryPanel;
import client.nilore.gui.newclickgui.UIElement;
import client.nilore.settings.Setting;
import client.nilore.utils.animation.SmoothAnimationTimer;

public abstract class SettingElement<T extends Setting<?>>
extends UIElement {
    @Getter
    protected final CategoryPanel parentPanel;
    @Getter
    protected final T setting;
    @Getter
    protected final SmoothAnimationTimer visibilityTimer = new SmoothAnimationTimer();

    @Generated
    public SettingElement(CategoryPanel categoryPanel, T setting) {
        this.parentPanel = categoryPanel;
        this.setting = setting;
    }
}