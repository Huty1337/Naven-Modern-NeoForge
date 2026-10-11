package client.nilore.gui.material3.setting;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import client.nilore.gui.material3.MD3Theme;
import client.nilore.render.FontRenderer;
import client.nilore.render.GlHelper;
import client.nilore.settings.Setting;
import client.nilore.settings.impl.BooleanSetting;
import client.nilore.utils.math.LerpUtil;
import client.nilore.utils.render.RenderUtil;

public class MD3BooleanRenderer implements MD3SettingRenderer {

    private final Map<BooleanSetting, Float> toggleAnim = new HashMap<>();
    private final Map<BooleanSetting, Float> hoverAnim = new HashMap<>();

    @Override
    public int render(GuiGraphics gg, Setting<?> s, int x, int y, int w, int mx, int my, float alpha, float accent) {
        if (!(s instanceof BooleanSetting bs)) return 0;
        int h = getHeight(s);
        boolean rowHov = mx >= x && mx <= x + w && my >= y && my <= y + h;

        float cur = toggleAnim.getOrDefault(bs, bs.getValue() ? 1f : 0f);
        float tgt = bs.getValue() ? 1f : 0f;
        toggleAnim.put(bs, Math.abs(tgt - cur) > 0.01f ? LerpUtil.smoothLerp(cur, tgt, 0.2f) : tgt);
        float tv = toggleAnim.get(bs);

        float hc = hoverAnim.getOrDefault(bs, 0f);
        hoverAnim.put(bs, LerpUtil.smoothLerp(hc, rowHov ? 1f : 0f, 0.2f));
        float hv = hoverAnim.get(bs);

        int row = MD3Theme.lerpColor(MD3Theme.SURFACE_DIM, MD3Theme.SURFACE_CONTAINER, hv * 0.85f);
        row = MD3Theme.lerpColor(row, (int)accent, tv * 0.12f);
        RenderUtil.drawRoundedRect(gg.pose(), x + 1f, y + 3f, w - 2f, h - 6f, 8f,
                MD3Theme.withAlpha(row, alpha * (0.58f + 0.16f * hv + 0.1f * tv)));

        RenderUtil.drawRoundedRect(gg.pose(), x + 9f, y + h / 2f - 2.5f, 5f, 5f, 2.5f,
                MD3Theme.withAlpha(bs.getValue() ? (int)accent : MD3Theme.TEXT_DISABLED, alpha * (bs.getValue() ? 0.86f : 0.5f + 0.22f * hv)));

        FontRenderer lf = MD3Theme.fontBody(0.92f);
        float ly = y + (h - lf.getMetrics().capHeight()) / 2f + 2f;
        MD3Theme.text(bs.getName(), x + 24f, ly, lf, bs.getValue() ? MD3Theme.TEXT_HIGH : MD3Theme.TEXT_MED, alpha);

        float tw = 29f, th = 15f;
        float tx = x + w - tw - 9f;
        float ty = y + (h - th) / 2f;
        int trackColor = MD3Theme.lerpColor(MD3Theme.SURFACE_HIGHEST, (int)accent, tv);
        if (hv > 0.01f) {
            RenderUtil.drawRoundedRect(gg.pose(), tx - 4f, ty - 4f, tw + 8f, th + 8f,
                    (th + 8f) / 2f, MD3Theme.withAlpha(bs.getValue() ? (int)accent : MD3Theme.SURFACE_HIGHEST, alpha * hv * 0.25f));
            trackColor = MD3Theme.brighten(trackColor, 1f + 0.08f * hv);
        }
        RenderUtil.drawRoundedRect(gg.pose(), tx, ty, tw, th, th / 2f,
                MD3Theme.withAlpha(trackColor, alpha));
        RenderUtil.drawRoundedRect(gg.pose(), tx + 1f, ty + 1f, tw - 2f, th - 2f, (th - 2f) / 2f,
                MD3Theme.withAlpha(MD3Theme.argb(bs.getValue() ? 24 : 14, 255, 255, 255), alpha));

        float ks = 10f + 1.5f * tv;
        float kx = tx + 3f + (tw - ks - 6f) * tv;
        float ky = ty + (th - ks) / 2f;
        RenderUtil.drawRoundedRect(gg.pose(), kx, ky, ks, ks, ks / 2f,
                MD3Theme.withAlpha(bs.getValue() ? MD3Theme.ON_PRIMARY : MD3Theme.TEXT_MED, alpha));

        return h;
    }

    @Override
    public boolean onClick(Setting<?> s, int x, int y, int w, int mx, int my, int btn, float scale) {
        if (!(s instanceof BooleanSetting bs) || btn != 0) return false;
        int h = getHeight(s);
        if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
            bs.setValue(!bs.getValue());
            return true;
        }
        return false;
    }

    @Override public boolean supports(Setting<?> s) { return s instanceof BooleanSetting; }
    @Override public int getHeight(Setting<?> s) { return 32; }
    @Override public void onMouseRelease(double mx, double my, int btn) {}
    @Override public void onMouseDrag(Setting<?> s, double mx, double my, int btn) {}
}
