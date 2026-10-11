package client.nilore.gui.material3.setting;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import client.nilore.gui.material3.MD3Theme;
import client.nilore.render.FontRenderer;
import client.nilore.render.GlHelper;
import client.nilore.settings.Setting;
import client.nilore.settings.impl.MultiSelectSetting;
import client.nilore.utils.math.LerpUtil;
import client.nilore.utils.render.RenderUtil;

public class MD3MultiSelectRenderer implements MD3SettingRenderer {

    private static final int ROW = 26;
    private final Map<String, Float> checkAnim = new HashMap<>();
    private final Map<String, Float> hoverAnim = new HashMap<>();

    @Override
    public int render(GuiGraphics gg, Setting<?> s, int x, int y, int w, int mx, int my, float alpha, float accent) {
        if (!(s instanceof MultiSelectSetting ms)) return 0;

        FontRenderer lf = MD3Theme.fontBody(0.92f);
        MD3Theme.text(ms.getName(), x + 10f, y + 7f, lf, MD3Theme.TEXT_MED, alpha);

        int ry = y + ROW;
        float boxSize = 14f;

        for (String opt : ms.getOptions()) {
            boolean sel = ms.isSelected(opt);
            boolean hov = mx >= x && mx <= x + w && my >= ry && my <= ry + ROW;

            float cc = checkAnim.getOrDefault(opt, 0f);
            checkAnim.put(opt, LerpUtil.smoothLerp(cc, sel ? 1f : 0f, 0.2f));
            float cv = checkAnim.get(opt);

            float hc = hoverAnim.getOrDefault(opt, 0f);
            hoverAnim.put(opt, LerpUtil.smoothLerp(hc, hov ? 1f : 0f, 0.18f));
            float hv = hoverAnim.get(opt);

            int row = MD3Theme.lerpColor(MD3Theme.SURFACE_DIM, MD3Theme.SURFACE_CONTAINER, hv);
            row = MD3Theme.lerpColor(row, (int)accent, cv * 0.1f);
            RenderUtil.drawRoundedRect(gg.pose(), x + 1f, ry + 3f, w - 2f, ROW - 6f, 8f,
                    MD3Theme.withAlpha(row, alpha * (0.58f + 0.14f * hv + 0.08f * cv)));

            FontRenderer of2 = MD3Theme.fontBody(1f);
            int tc = sel ? MD3Theme.TEXT_HIGH : MD3Theme.lerpColor(MD3Theme.TEXT_LOW, MD3Theme.TEXT_MED, hv);
            float oly = ry + (ROW - of2.getMetrics().capHeight()) / 2f;
            MD3Theme.text(opt, x + 12f, oly, of2, tc, alpha);

            float bx = x + w - boxSize - 10f;
            float by = ry + (ROW - boxSize) / 2f;
            if (hv > 0.01f) {
                float r = boxSize + 5f * hv;
                RenderUtil.drawRoundedRect(gg.pose(), bx - (r - boxSize) / 2f, by - (r - boxSize) / 2f,
                        r, r, r / 2f, MD3Theme.withAlpha((int)accent, alpha * hv * 0.16f));
            }
            int box = MD3Theme.lerpColor(MD3Theme.SURFACE_HIGHEST, (int)accent, cv);
            RenderUtil.drawRoundedRect(gg.pose(), bx, by, boxSize, boxSize, 4f,
                    MD3Theme.withAlpha(box, alpha * 0.9f));
            RenderUtil.drawRoundedRect(gg.pose(), bx + 1f, by + 1f, boxSize - 2f, boxSize - 2f, 3f,
                    MD3Theme.withAlpha(sel ? MD3Theme.argb(24, 255, 255, 255) : MD3Theme.OUTLINE_VARIANT, alpha * (sel ? cv : 0.7f)));
            if (cv > 0.35f) {
                FontRenderer iconF = MD3Theme.fontMaterial(11f);
                GlHelper.drawText("", bx + 3f, by + (boxSize - iconF.getMetrics().capHeight()) / 2f + 1.5f,
                        iconF, MD3Theme.withAlpha(MD3Theme.ON_PRIMARY, alpha * cv));
            }

            ry += ROW;
        }

        return getHeight(ms);
    }

    @Override
    public boolean onClick(Setting<?> s, int x, int y, int w, int mx, int my, int btn, float scale) {
        if (!(s instanceof MultiSelectSetting ms) || btn != 0) return false;
        int ry = y + ROW;
        for (String opt : ms.getOptions()) {
            if (mx >= x && mx <= x + w && my >= ry && my <= ry + ROW) {
                if (ms.isSelected(opt)) ms.getValue().remove(opt);
                else ms.getValue().add(opt);
                return true;
            }
            ry += ROW;
        }
        return false;
    }

    @Override
    public int getHeight(Setting<?> s) {
        if (!(s instanceof MultiSelectSetting ms)) return 0;
        return ROW + ms.getOptions().size() * ROW;
    }

    @Override public boolean supports(Setting<?> s) { return s instanceof MultiSelectSetting; }
    @Override public void onMouseRelease(double mx, double my, int btn) {}
    @Override public void onMouseDrag(Setting<?> s, double mx, double my, int btn) {}
}
