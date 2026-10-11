package client.nilore.hud;

import client.nilore.ClientBase;
import client.nilore.NiloreClient;
import client.nilore.event.impl.GlRenderEvent;
import client.nilore.render.DrawContext;
import client.nilore.render.FontPresets;
import client.nilore.render.FontRenderer;

public class LogoWatermark {
    private static final String LOGO = "N";
    private static final FontRenderer LOGO_FONT = FontPresets.niloreIcon(96.0f);

    public void onGlRender(GlRenderEvent event) {
        if (ClientBase.mc.player == null || NiloreClient.getInstance().getHudManager() == null) {
            return;
        }
        ModuleListHud moduleList = NiloreClient.getInstance().getHudManager().getHudElement(ModuleListHud.class);
        int topColor = moduleList == null ? 0xFFFFFFFF : moduleList.getThemeColor(0, 0.0f, 1);
        int bottomColor = moduleList == null ? 0xFFFFFFFF : moduleList.getThemeColor(1, 1.0f, 1);
        DrawContext drawContext = event.drawContext();
        drawContext.drawStringGradient(LOGO, -2.0f, 18.0f + LOGO_FONT.getMetrics().capHeight(), LOGO_FONT, topColor, bottomColor);
    }
}
