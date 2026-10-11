package client.nilore.gui.material3.setting;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import client.nilore.gui.material3.MD3Theme;
import client.nilore.render.FontRenderer;
import client.nilore.render.GlHelper;
import client.nilore.settings.Setting;
import client.nilore.settings.impl.NumberSetting;
import client.nilore.utils.math.LerpUtil;
import client.nilore.utils.render.RenderUtil;

public class MD3NumberRenderer implements MD3SettingRenderer {

    private final Map<NumberSetting, Float> hoverAnim = new HashMap<>();
    private final Map<NumberSetting, Boolean> dragging = new HashMap<>();
    private NumberSetting dragTarget;
    private float dragTrackX, dragTrackW;

    @Override
    public int render(GuiGraphics gg, Setting<?> s, int x, int y, int w, int mx, int my, float alpha, float accent) {
        if (!(s instanceof NumberSetting ns)) return 0;
        int h = getHeight(s);
        boolean rowHov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean active = dragging.getOrDefault(ns, false);

        float hc = hoverAnim.getOrDefault(ns, 0f);
        hoverAnim.put(ns, LerpUtil.smoothLerp(hc, rowHov || active ? 1f : 0f, 0.2f));
        float hv = hoverAnim.get(ns);

        RenderUtil.drawRoundedRect(gg.pose(), x + 1f, y + 3f, w - 2f, h - 6f, 9f,
                MD3Theme.withAlpha(MD3Theme.lerpColor(MD3Theme.SURFACE_DIM, MD3Theme.SURFACE_CONTAINER, hv), alpha * (0.68f + 0.18f * hv)));

        FontRenderer lf = MD3Theme.fontBody(1f);
        MD3Theme.text(ns.getName(), x + 10f, y + 10f, lf, MD3Theme.TEXT_MED, alpha);

        FontRenderer vf = MD3Theme.fontLabel(1f);
        String val = formatVal(ns.getValue().doubleValue());
        float vw = GlHelper.getStringWidth(val, vf);
        float valX = x + w - vw - 9f, valY = y + 9f;
        MD3Theme.text(val, valX, valY,
                vf, active ? MD3Theme.ON_PRIMARY : (int)accent, alpha * 0.9f);

        float trackY = y + h - 10f;
        float trackH = 3.5f;
        float trackX = x + 10f;
        float trackW = w - 20f;
        RenderUtil.drawRoundedRect(gg.pose(), trackX, trackY, trackW, trackH, trackH / 2f,
                MD3Theme.withAlpha(MD3Theme.SURFACE_HIGHEST, alpha * 0.58f));

        double min = ns.getMin().doubleValue(), max = ns.getMax().doubleValue();
        double valD = ns.getValue().doubleValue();
        float fill = (float)((valD - min) / (max - min));
        fill = Math.max(0f, Math.min(1f, fill));
        if (fill > 0.01f) {
            RenderUtil.drawRoundedRect(gg.pose(), trackX, trackY, trackW * fill, trackH, trackH / 2f,
                    MD3Theme.withAlpha((int)accent, alpha));
        }

        float thumbR = 3.6f + 0.6f * hv;
        float thumbX = trackX + trackW * fill;
        float thumbY = trackY + trackH / 2f;
        if (active || hv > 0.01f) {
            float halo = thumbR + 3f;
            RenderUtil.drawRoundedRect(gg.pose(), thumbX - halo, thumbY - halo,
                    halo * 2f, halo * 2f, halo,
                    MD3Theme.withAlpha((int)accent, alpha * (active ? 0.16f : 0.08f * hv)));
        }
        RenderUtil.drawRoundedRect(gg.pose(), thumbX - thumbR, thumbY - thumbR,
                thumbR * 2f, thumbR * 2f, thumbR,
                MD3Theme.withAlpha((int)accent, alpha * 0.92f));

        return h;
    }

    @Override
    public boolean onClick(Setting<?> s, int x, int y, int w, int mx, int my, int btn, float scale) {
        if (!(s instanceof NumberSetting ns) || btn != 0) return false;
        int h = getHeight(s);
        float trackX = x + 10f, trackW = w - 20f;
        float trackY = y + h - 15f;
        if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
            float ratio = Math.max(0f, Math.min(1f, (mx - trackX) / trackW));
            double min = ns.getMin().doubleValue(), max = ns.getMax().doubleValue();
            double step = ns.getStep().doubleValue();
            double raw = min + (max - min) * ratio;
            double snapped = Math.round(raw / step) * step;
            snapped = Math.max(min, Math.min(max, snapped));
            applyValue(ns, snapped);
            dragging.put(ns, true);
            dragTarget = ns;
            dragTrackX = trackX;
            dragTrackW = trackW;
            return true;
        }
        return false;
    }

    @Override
    public void onMouseRelease(double mx, double my, int btn) {
        dragging.clear();
        dragTarget = null;
    }

    @Override
    public void onMouseDrag(Setting<?> s, double mx, double my, int btn) {
        if (dragTarget == null || btn != 0) return;
        float ratio = Math.max(0f, Math.min(1f, (float)(mx - dragTrackX) / dragTrackW));
        double min = dragTarget.getMin().doubleValue(), max = dragTarget.getMax().doubleValue();
        double step = dragTarget.getStep().doubleValue();
        double raw = min + (max - min) * ratio;
        double snapped = Math.round(raw / step) * step;
        snapped = Math.max(min, Math.min(max, snapped));
        applyValue(dragTarget, snapped);
    }

    private void applyValue(NumberSetting ns, double v) {
        if (ns.getValue() instanceof Integer) ns.setValue((int)Math.round(v));
        else if (ns.getValue() instanceof Long) ns.setValue(Math.round(v));
        else if (ns.getValue() instanceof Float) ns.setValue((float)v);
        else ns.setValue(v);
    }

    private String formatVal(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((int)v);
        return String.format(Locale.US, "%.1f", v);
    }

    @Override public boolean supports(Setting<?> s) { return s instanceof NumberSetting; }
    @Override public int getHeight(Setting<?> s) { return 34; }
}
