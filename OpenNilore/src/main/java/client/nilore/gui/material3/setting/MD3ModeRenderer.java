package client.nilore.gui.material3.setting;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import client.nilore.gui.material3.MD3Theme;
import client.nilore.render.FontRenderer;
import client.nilore.render.GlHelper;
import client.nilore.settings.Setting;
import client.nilore.settings.impl.ModeSetting;
import client.nilore.utils.math.LerpUtil;
import client.nilore.utils.render.RenderUtil;

public class MD3ModeRenderer implements MD3SettingRenderer {

    private static final int HEADER_H = 30;
    private static final int ITEM_H = 23;
    private final Map<ModeSetting, Boolean> openState = new HashMap<>();
    private final Map<ModeSetting, Float> openAnim = new HashMap<>();
    private final Map<ModeSetting, Map<String, Float>> itemHover = new HashMap<>();

    @Override
    public int render(GuiGraphics gg, Setting<?> s, int x, int y, int w, int mx, int my, float alpha, float accent) {
        if (!(s instanceof ModeSetting ms)) return 0;

        boolean open = openState.getOrDefault(ms, false);
        float cur = openAnim.getOrDefault(ms, 0f);
        openAnim.put(ms, LerpUtil.smoothLerp(cur, open ? 1f : 0f, open ? 0.15f : 0.2f));
        float of = openAnim.getOrDefault(ms, 0f);

        String[] others = Arrays.stream((Object[])ms.getModes())
                .filter(m -> !Objects.equals(m, ms.getValue())).toArray(String[]::new);
        boolean headerHov = mx >= x && mx <= x + w && my >= y && my <= y + HEADER_H;
        float expandedH = others.length * ITEM_H * of;

        int headerBg = MD3Theme.lerpColor(MD3Theme.SURFACE_DIM, MD3Theme.SURFACE_CONTAINER, headerHov ? 1f : 0f);
        headerBg = MD3Theme.lerpColor(headerBg, (int)accent, of * 0.1f);
        RenderUtil.drawRoundedRect(gg.pose(), x + 1f, y + 3f, w - 2f, HEADER_H - 6f, 8f,
                MD3Theme.withAlpha(headerBg, alpha * (0.58f + 0.14f * of)));

        FontRenderer lf = MD3Theme.fontBody(0.92f);
        MD3Theme.text(ms.getName(), x + 10f, y + (HEADER_H - lf.getMetrics().capHeight()) / 2f + 2f,
                lf, MD3Theme.TEXT_HIGH, alpha);

        FontRenderer vf = MD3Theme.fontLabel(1f);
        String curVal = ms.getValue() != null ? ms.getValue() : "None";
        float vw = GlHelper.getStringWidth(curVal, vf);
        float chipW = vw + 22f, chipH = 16f;
        float chipX = x + w - chipW - 8f, chipY = y + (HEADER_H - chipH) / 2f;
        RenderUtil.drawRoundedRect(gg.pose(), chipX, chipY, chipW, chipH, 5f,
                MD3Theme.withAlpha(open ? (int)accent : MD3Theme.SURFACE_HIGHEST, alpha * (open ? 0.72f : 0.52f)));
        MD3Theme.text(curVal, chipX + 7f, chipY + (chipH - vf.getMetrics().capHeight()) / 2f,
                vf, open ? MD3Theme.ON_PRIMARY : (int)accent, alpha * 0.9f);
        FontRenderer iconF = MD3Theme.fontMaterial(12f);
        GlHelper.drawText(open ? "" : "", chipX + chipW - 10f,
                chipY + (chipH - iconF.getMetrics().capHeight()) / 2f + 2f,
                iconF, MD3Theme.withAlpha(open ? MD3Theme.ON_PRIMARY : MD3Theme.TEXT_LOW, alpha));

        if (of > 0.01f) {
            RenderUtil.drawRoundedRect(gg.pose(), x + 1f, y + HEADER_H, w - 2f, expandedH, 7f,
                    MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER, alpha * of * 0.72f));
            itemHover.putIfAbsent(ms, new HashMap<>());
            Map<String, Float> hMap = itemHover.get(ms);
            int itemY = y + HEADER_H;
            for (String mode : others) {
                boolean hov = mx >= x && mx <= x + w && my >= itemY && my <= itemY + ITEM_H;
                float hc = hMap.getOrDefault(mode, 0f);
                hMap.put(mode, LerpUtil.smoothLerp(hc, hov ? 1f : 0f, 0.2f));
                float hv = hMap.get(mode);

                int bg = MD3Theme.lerpColor(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_HIGH, hv);
                RenderUtil.drawRoundedRect(gg.pose(), x + 5f, itemY + 2f, w - 10f, ITEM_H - 4f, 6f,
                        MD3Theme.withAlpha(bg, alpha * of));

                FontRenderer mf = MD3Theme.fontBody(1f);
                int mc2 = MD3Theme.lerpColor(MD3Theme.TEXT_LOW, MD3Theme.TEXT_HIGH, hv);
                float mty = itemY + (ITEM_H - mf.getMetrics().capHeight()) / 2f;
                MD3Theme.text(mode, x + 12f, mty, mf, mc2, alpha * of);

                itemY += ITEM_H;
            }
        }

        return getHeight(ms);
    }

    @Override
    public boolean onClick(Setting<?> s, int x, int y, int w, int mx, int my, int btn, float scale) {
        if (!(s instanceof ModeSetting ms)) return false;
        boolean open = openState.getOrDefault(ms, false);

        // Click header to toggle
        if (my >= y && my < y + HEADER_H) {
            openState.put(ms, !open);
            return true;
        }

        // Click item
        if (open) {
            String[] others = Arrays.stream((Object[])ms.getModes())
                    .filter(m -> !Objects.equals(m, ms.getValue())).toArray(String[]::new);
            int itemY = y + HEADER_H;
            for (String mode : others) {
                if (mx >= x && mx <= x + w && my >= itemY && my <= itemY + ITEM_H && btn == 0) {
                    ms.setValue(mode);
                    openState.put(ms, false);
                    return true;
                }
                itemY += ITEM_H;
            }
        }
        return false;
    }

    @Override
    public int getHeight(Setting<?> s) {
        if (!(s instanceof ModeSetting ms)) return HEADER_H;
        String[] others = Arrays.stream((Object[])ms.getModes())
                .filter(m -> !Objects.equals(m, ms.getValue())).toArray(String[]::new);
        float of = openAnim.getOrDefault(ms, 0f);
        return HEADER_H + (int)(others.length * ITEM_H * of);
    }

    @Override public boolean supports(Setting<?> s) { return s instanceof ModeSetting; }
    @Override public void onMouseRelease(double mx, double my, int btn) {}
    @Override public void onMouseDrag(Setting<?> s, double mx, double my, int btn) {}
}
