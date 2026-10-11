package client.nilore.gui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import client.nilore.NiloreClient;
import client.nilore.gui.material3.MD3Theme;
import client.nilore.gui.material3.setting.MD3SettingRegistry;
import client.nilore.gui.panel.setting.NumberSettingRenderer;
import client.nilore.modules.Category;
import client.nilore.modules.Module;
import client.nilore.modules.impl.render.ClickGuiModule;
import client.nilore.render.DrawContext;
import client.nilore.render.FontRenderer;
import client.nilore.render.Paint;
import client.nilore.render.RoundedRectangle;
import client.nilore.render.GlHelper;
import client.nilore.render.Renderer;
import client.nilore.settings.Setting;
import client.nilore.utils.math.LerpUtil;
import client.nilore.utils.render.RenderUtil;

public class MaterialClickGui extends Screen {

    public static final MaterialClickGui instance = new MaterialClickGui();

    private enum State { CLOSED, OPENING, OPEN, CLOSING }
    private State state = State.CLOSED;
    private float openT = 0f;

    @Getter private Category selected = Category.COMBAT;
    private List<Module> modules;
    private final Map<Category, Float> catHover = new HashMap<>();
    private final Map<Category, Float> catSelect = new HashMap<>();
    private final Map<Module, Float> modHover = new HashMap<>();
    private final Map<Module, Float> modToggleAnim = new HashMap<>();
    private float focusedToggleHover = 0f;
    private Module focused;

    private float scrollY = 0f, scrollTarget = 0f, contentH = 0f;
    private float scrollAlpha = 0f;
    private long scrollTime = 0L;

    private float settingsAlpha = 0f;
    private float sScrollY = 0f, sScrollTarget = 0f, sContentH = 0f;
    private float sScrollAlpha = 0f;
    private long sScrollTime = 0L;

    private boolean searching = false, searchFocus = false;
    private String query = "";
    private long cursorTime = 0L;

    private static class Toast {
        final String msg;
        final long born;
        float y, targetY, alpha;
        Toast(String m) { msg = m; born = System.currentTimeMillis(); }
    }
    private final List<Toast> toasts = new CopyOnWriteArrayList<>();

    // ── Layout ──
    private static final float W = 650f, H = 390f, R = 16f;
    private static final float SIDEBAR_W = 174f, GAP = 0f;
    private static final float SETTINGS_W = 256f;
    private static final float CARD = 36f, CARD_GAP = 5f, CARD_R = 8f;
    private static final float HEAD = 44f;
    private static final float TOGGLE_W = 32f, TOGGLE_H = 18f;
    private static final float CAT_H = 32f, CAT_GAP = 5f, CAT_R = 9f;
    private static final float CAT_TOP = 74f;
    private static final float USER_H = 36f, USER_R = 9f;
    private static final float SEARCH_H = 28f, SEARCH_R = 8f;

    private MaterialClickGui() {
        super(Component.nullToEmpty("Nilore ClickGui"));
    }

    // ──────────────── Lifecycle ────────────────

    public void init() {
        super.init();
        LerpUtil.reset();
        if (state == State.CLOSED) openT = 0f;
        state = State.OPENING;
        loadModules();
    }

    public boolean isPauseScreen() { return false; }
    public void onClose() { if (state != State.CLOSING) state = State.CLOSING; }

    // ──────────────── Geometry ────────────────

    private float detailW() {
        float base = 318f;
        if (settingsAlpha > 0.05f) base += GAP + SETTINGS_W * settingsAlpha;
        return base;
    }
    private float panelW() { return SIDEBAR_W + GAP + detailW(); }
    private float ox() { return width / 2f - panelW() / 2f; }
    private float oy() { return height / 2f - H / 2f - 6f; }

    // ──────────────── Render ────────────────

    public void render(@Nonnull GuiGraphics gg, int mx, int my, float pt) {
        LerpUtil.update();
        syncTheme();
        tickState();
        if (state == State.CLOSED && openT <= 0f) return;

        float ease = ease(openT);
        float sc = 0.97f + 0.03f * ease;

        // Scrim
        gg.fill(0, 0, width, height, MD3Theme.withAlpha(MD3Theme.SCRIM, ease));

        // Settings alpha
        float st = focused != null ? 1f : 0f;
        settingsAlpha = lerp(settingsAlpha, st, 0.1f);

        // Smooth scroll
        scrollY = lerp(scrollY, scrollTarget, 0.25f);
        sScrollY = lerp(sScrollY, sScrollTarget, 0.25f);

        // Scale transform
        int cx = width / 2, cy = height / 2;
        gg.pose().pushPose();
        gg.pose().translate(cx, cy, 0);
        gg.pose().scale(sc, sc, 1);
        gg.pose().translate(-cx, -cy, 0);

        float a = ease;
        float px = ox(), py = oy();

        Renderer.renderConsumer(dc -> {
            float pw = panelW();

            // Shadow
            RenderUtil.drawBlurredRect(gg.pose(), px - 4, py - 4, pw + 8, H + 8, R, 28f, a * 0.45f, 0xFF000000);
            // Main surface (provides outer rounded boundary)
            RenderUtil.drawRoundedRect(gg.pose(), px, py, pw, H, R, MD3Theme.withAlpha(MD3Theme.SURFACE, a));

            // Clip to main panel so inner rects don't overflow the rounded corners
            gg.enableScissor((int)px, (int)py, (int)(px + pw), (int)(py + H));
            renderSidebar(dc, gg, px, py, mx, my, a);
            renderDetail(dc, gg, px, py, mx, my, a);
            gg.disableScissor();
            renderToasts(gg, px, py, a);
        });

        super.render(gg, mx, my, pt);
        gg.pose().popPose();
    }

    // ──────────────── Sidebar ────────────────

    private void renderSidebar(DrawContext dc, GuiGraphics gg, float px, float py, int mx, int my, float a) {
        dc.drawRoundedRect(RoundedRectangle.ofXYWHRadii(px, py, SIDEBAR_W, H, new float[]{R, 0, 0, R}),
                new Paint().setColor(MD3Theme.SIDEBAR));
        RenderUtil.drawFilledRect(gg.pose(), px + SIDEBAR_W - 1f, py + 16f, 0.5f, H - 32f,
                MD3Theme.withAlpha(MD3Theme.OUTLINE_VARIANT, a * 0.48f));

        FontRenderer brandIcon = MD3Theme.fontMaterial(22f);
        float iconX = px + 13f;
        float iconY = py + 13f;
        RenderUtil.drawRoundedRect(gg.pose(), iconX - 5f, iconY - 5f, 30f, 30f, 10f,
                MD3Theme.withAlpha(MD3Theme.PRIMARY_CONTAINER, a * 0.72f));
        GlHelper.drawText("", iconX+5f, iconY + 10f, brandIcon, MD3Theme.withAlpha(MD3Theme.PRIMARY, a));

        FontRenderer titleF = MD3Theme.fontTitle(1f);
        float tx = px + 51f;
        MD3Theme.text("Nilore", tx, py + 13f, titleF, MD3Theme.TEXT_HIGH, a);

        FontRenderer betaF = MD3Theme.fontLabel(1f);
        float betaY = py + 26f;
        RenderUtil.drawRoundedRect(gg.pose(), tx, betaY - 1f, 24f, 13f, 6.5f, // 34
                MD3Theme.withAlpha(MD3Theme.PRIMARY_CONTAINER, a * 0.52f));
        MD3Theme.text("beta", tx + 6f, betaY+5f, betaF, MD3Theme.PRIMARY, a * 0.92f);

        RenderUtil.drawFilledRect(gg.pose(), px + 14f, py + 61f, SIDEBAR_W - 28f, 0.5f,
                MD3Theme.withAlpha(MD3Theme.OUTLINE_VARIANT, a * 0.34f));

        Category[] cats = Category.values();
        float catBaseY = py + CAT_TOP;
        for (int i = 0; i < cats.length; i++) {
            Category c = cats[i];
            float cy = catBaseY + i * (CAT_H + CAT_GAP);
            boolean sel = c == selected;
            boolean hov = mx >= px + 8 && mx <= px + SIDEBAR_W - 8 && my >= cy && my <= cy + CAT_H;

            float hc = catHover.getOrDefault(c, 0f);
            catHover.put(c, lerp(hc, hov ? 1f : 0f, 0.12f));
            float sc2 = catSelect.getOrDefault(c, 0f);
            catSelect.put(c, lerp(sc2, sel ? 1f : 0f, 0.12f));
            float hv = catHover.get(c), sv = catSelect.get(c);

            float catX = px + 8f, catW = SIDEBAR_W - 16f;
            int accent = MD3Theme.primary(c);
            int fill = MD3Theme.lerpColor(MD3Theme.SURFACE_HIGHEST, MD3Theme.container(c), sv * 0.72f);
            float fillAlpha = a * (0.34f * hv + 0.74f * sv);
            if (fillAlpha > 0.01f) {
                RenderUtil.drawRoundedRect(gg.pose(), catX, cy, catW, CAT_H, CAT_R,
                        MD3Theme.withAlpha(fill, fillAlpha));
            }
            if (sel) {
                RenderUtil.drawRoundedRect(gg.pose(), catX + 4f, cy + 8f, 3f, CAT_H - 16f, 1.5f,
                        MD3Theme.withAlpha(accent, a * sv));
            }

            FontRenderer iconF = MD3Theme.fontMaterial(17f);
            float ix = catX + 14f + sv;
            float iy = cy + (CAT_H - iconF.getMetrics().capHeight()) / 2f + 2f;
            int ic = sel ? accent : MD3Theme.lerpColor(MD3Theme.TEXT_LOW, MD3Theme.TEXT_MED, hv);
            GlHelper.drawText(MD3Theme.icon(c), ix, iy, iconF, MD3Theme.withAlpha(ic, a));

            FontRenderer lf = MD3Theme.fontBody(1f);
            int tc = sel ? MD3Theme.TEXT_HIGH : MD3Theme.lerpColor(MD3Theme.TEXT_LOW, MD3Theme.TEXT_MED, hv);
            float lx = catX + 39f + sv;
            float ly = cy + (CAT_H - lf.getMetrics().capHeight()) / 2f;
            MD3Theme.text(MD3Theme.label(c), lx, ly, lf, tc, a);
        }

        float userY = py + H - USER_H - 12f;
        float userX = px + 8f, userW = SIDEBAR_W - 16f;
        RenderUtil.drawRoundedRect(gg.pose(), userX, userY, userW, USER_H, USER_R,
                MD3Theme.withAlpha(MD3Theme.SURFACE_HIGH, a * 0.72f));

        FontRenderer userIconF = MD3Theme.fontMaterial(15f);
        float uix = userX + 10f;
        float uiy = userY + (USER_H - userIconF.getMetrics().capHeight()) / 2f + 2f;
        GlHelper.drawText(MD3Theme.ICON_PERSON, uix, uiy, userIconF, MD3Theme.withAlpha(MD3Theme.TEXT_LOW, a));

        String username = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getGameProfile().getName() : "Player";
        FontRenderer uf = MD3Theme.fontPingfang(1f);
        float ux = userX + 31f;
        float uy = userY + (USER_H - uf.getMetrics().capHeight()) / 2f;
        MD3Theme.text(username, ux, uy, uf, MD3Theme.TEXT_MED, a);
    }

    // ──────────────── Detail Panel ────────────────

    private void renderDetail(DrawContext dc, GuiGraphics gg, float px, float py, int mx, int my, float a) {
        float dx = px + SIDEBAR_W + GAP;
        float dw = detailW();
        boolean settingsOpen = settingsAlpha > 0.05f;

        // Module list width
        float listW = settingsOpen ? (dw - GAP) * (1f - settingsAlpha * 0.45f) : dw;

        // ── 1) Module list bg: left straight, right rounded when settings closed ──
        float listR = settingsOpen ? 0 : R;
        dc.drawRoundedRect(RoundedRectangle.ofXYWHRadii(dx, py, listW, H, new float[]{0, listR, listR, 0}),
                new Paint().setColor(MD3Theme.SURFACE_DIM));

        // ── 2) Settings panel bg: left straight, right rounded ──
        if (settingsOpen) {
            float sw = SETTINGS_W * settingsAlpha;
            float sx = dx + dw - sw;
            dc.drawRoundedRect(RoundedRectangle.ofXYWHRadii(sx, py, sw, H, new float[]{0, R, R, 0}),
                    new Paint().setColor(MD3Theme.SURFACE_CONTAINER));
            RenderUtil.drawFilledRect(gg.pose(), sx, py + 12f, 0.5f, H - 24f,
                    MD3Theme.withAlpha(MD3Theme.OUTLINE_VARIANT, a * settingsAlpha));
        }

        int activeCount = modules != null ? (int)modules.stream().filter(Module::isEnabled).count() : 0;
        int totalCount = modules != null ? modules.size() : 0;

        FontRenderer iconF = MD3Theme.fontMaterial(17f);
        GlHelper.drawText(MD3Theme.icon(selected), dx + 14f, py + 17f, iconF,
                MD3Theme.withAlpha(MD3Theme.primary(selected), a));

        FontRenderer headF = MD3Theme.fontTitle(1f);
        MD3Theme.text(MD3Theme.label(selected), dx + 38f, py + 16f, headF, MD3Theme.TEXT_HIGH, a);

        FontRenderer countF = MD3Theme.fontLabel(1f);
        String countStr = activeCount + " active / " + totalCount;
        float countW = GlHelper.getStringWidth(countStr, countF) + 14f;
        RenderUtil.drawRoundedRect(gg.pose(), dx + listW - countW - 12f, py + 10f, countW, 18f, 9f,
                MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER, a * 0.82f));
        MD3Theme.text(countStr, dx + listW - countW - 5f, py + 17f, countF, MD3Theme.TEXT_LOW, a);

        RenderUtil.drawFilledRect(gg.pose(), dx + 12f, py + HEAD - 2f, listW - 24f, 0.5f,
                MD3Theme.withAlpha(MD3Theme.OUTLINE_VARIANT, a * 0.32f));

        float contentY = py + HEAD;
        float searchY = contentY + 12f;
        float cardTop = searchY + SEARCH_H + 10f;
        float cardAreaH = H - (cardTop - py) - 12f;

        renderSearch(gg, dx + 12f, searchY, listW - 24f, mx, my, a);

        RenderUtil.drawRoundedRect(gg.pose(), dx + 8f, cardTop - 6f, listW - 16f, cardAreaH + 12f, 12f,
                MD3Theme.withAlpha(MD3Theme.SURFACE, a * 0.22f));

        gg.enableScissor((int)dx, (int)cardTop, (int)(dx + listW), (int)(cardTop + cardAreaH));

        if (modules != null) {
            contentH = modules.size() * (CARD + CARD_GAP) - CARD_GAP;
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                float cardY = cardTop + i * (CARD + CARD_GAP) - scrollY;
                if (cardY + CARD < cardTop || cardY > cardTop + cardAreaH) continue;
                renderCard(gg, dx + 12f, cardY, listW - 24f, CARD, m, mx, my, a);
            }
        } else {
            contentH = 0f;
        }
        gg.disableScissor();

        // Store for scroll
        this.contentH = contentH;
        tickScrollAlpha();
        renderScrollbar(gg, dx + listW - 4f, cardTop, cardAreaH, scrollY, contentH, scrollAlpha, a);

        // Right: settings panel
        if (settingsAlpha > 0.02f) {
            renderSettings(gg, dx, py, dw, mx, my, a);
        }
    }

    private void renderCard(GuiGraphics gg, float x, float y, float w, float h,
                            Module m, int mx, int my, float a) {
        boolean on = m.isEnabled();
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        boolean rc = m == focused;

        float hc = modHover.getOrDefault(m, 0f);
        modHover.put(m, lerp(hc, hov ? 1f : 0f, 0.12f));
        float hv = modHover.get(m);

        // Toggle animation
        float tc = modToggleAnim.getOrDefault(m, 0f);
        modToggleAnim.put(m, lerp(tc, on ? 1f : 0f, 0.15f));
        float tv = modToggleAnim.get(m);

        int cp = MD3Theme.primary(selected);
        int cc = MD3Theme.container(selected);

        int rowBase = MD3Theme.lerpColor(MD3Theme.SURFACE_DIM, MD3Theme.SURFACE_CONTAINER, hv * 0.75f);
        int rowFill = MD3Theme.lerpColor(rowBase, cc, tv * 0.42f);
        float rowAlpha = a * (0.58f + hv * 0.18f + tv * 0.2f);
        RenderUtil.drawRoundedRect(gg.pose(), x, y, w, h, CARD_R,
                MD3Theme.withAlpha(rowFill, rowAlpha));

        if (hov || rc) {
            RenderUtil.drawRoundedRect(gg.pose(), x, y, w, h, CARD_R,
                    MD3Theme.withAlpha(rc ? cp : MD3Theme.SURFACE_HIGHEST, a * (rc ? 0.18f : 0.16f * hv)));
        }

        RenderUtil.drawRoundedRect(gg.pose(), x + 8f, y + h / 2f - 3f, 6f, 6f, 3f,
                MD3Theme.withAlpha(on ? cp : MD3Theme.TEXT_DISABLED, a * (on ? 0.95f : 0.55f + 0.25f * hv)));
        FontRenderer nf = MD3Theme.fontBodyLarge(1f);
        int nc = on ? MD3Theme.TEXT_HIGH : MD3Theme.lerpColor(MD3Theme.TEXT_LOW, MD3Theme.TEXT_MED, hv);
        MD3Theme.text(m.getName(), x + 24f, y + (h - nf.getMetrics().capHeight()) / 2f + 2f,
                nf, nc, a);

        String b = m.getBind().getName();
        float right = x + w - 9f;
        if (!b.equalsIgnoreCase("None")) {
            FontRenderer bf = MD3Theme.fontLabel(1f);
            float bw = GlHelper.getStringWidth(b, bf) + 10f;
            float bh = 15f, bx = right - bw, by = y + (h - bh) / 2f;
            RenderUtil.drawRoundedRect(gg.pose(), bx, by, bw, bh, 6f,
                    MD3Theme.withAlpha(MD3Theme.SURFACE_HIGHEST, a * (on ? 0.55f : 0.42f + 0.2f * hv)));
            MD3Theme.text(b, bx + 5f, by + (bh - bf.getMetrics().capHeight()) / 2f, bf,
                    on ? MD3Theme.TEXT_MED : MD3Theme.TEXT_DISABLED, a * 0.9f);
            right = bx - 8f;
        }

        FontRenderer stateF = MD3Theme.fontLabel(1f);
        String state = on ? "ON" : "OFF";
        int stateColor = on ? cp : MD3Theme.TEXT_DISABLED;
        float stateW = GlHelper.getStringWidth(state, stateF);
        MD3Theme.text(state, right - stateW, y + (h - stateF.getMetrics().capHeight()) / 2f,
                stateF, stateColor, a * (on ? 0.95f : 0.72f));

        if (rc) {
            FontRenderer dotF = MD3Theme.fontMaterial(13f);
            GlHelper.drawText("", right - stateW - 18f, y + (h - dotF.getMetrics().capHeight()) / 2f + 1f,
                    dotF, MD3Theme.withAlpha(cp, a));
        }
    }

    // ──────────────── Settings Panel ────────────────

    private void renderSettings(GuiGraphics gg, float dx, float py, float dw, int mx, int my, float a) {
        if (focused == null) return;
        float sx = dx + dw - SETTINGS_W * settingsAlpha;
        float sw = SETTINGS_W * settingsAlpha;
        float pa = a * settingsAlpha;
        int cp = MD3Theme.primary(selected);

        RenderUtil.drawRoundedRect(gg.pose(), sx + 10f, py + 10f, sw - 20f, 50f, 12f,
                MD3Theme.withAlpha(MD3Theme.SURFACE_HIGH, pa * 0.42f));

        FontRenderer iconF = MD3Theme.fontMaterial(18f);
        RenderUtil.drawRoundedRect(gg.pose(), sx + 18f, py + 20f, 28f, 28f, 9f,
                MD3Theme.withAlpha(focused.isEnabled() ? MD3Theme.container(selected) : MD3Theme.SURFACE_HIGHEST, pa * 0.82f));
        GlHelper.drawText(MD3Theme.ICON_MODULE, sx + 28f, py + 34f, iconF,
                MD3Theme.withAlpha(focused.isEnabled() ? cp : MD3Theme.TEXT_LOW, pa));

        FontRenderer tf = MD3Theme.fontTitleMedium(1f);
        MD3Theme.text(focused.getName(), sx + 56f, py + 26f, tf, MD3Theme.TEXT_HIGH, pa);

        FontRenderer sf = MD3Theme.fontLabel(1f);
        MD3Theme.text(focused.isEnabled() ? "Enabled" : "Disabled", sx + 56f, py + 39f, sf,
                focused.isEnabled() ? cp : MD3Theme.TEXT_LOW, pa * 0.92f);

        float ttx = sx + sw - TOGGLE_W - 18f, tty = py + 26f;
        focusedToggleHover = lerp(focusedToggleHover, isToggleHovered(dx, py, focused, mx, my) ? 1f : 0f, 0.16f);
        renderToggle(gg, ttx, tty, focused.isEnabled(), focusedToggleHover, pa, cp);

        float cY = py + 72f, cH = H - 84f;
        RenderUtil.drawRoundedRect(gg.pose(), sx + 8f, cY - 6f, sw - 16f, cH + 12f, 12f,
                MD3Theme.withAlpha(MD3Theme.SURFACE, pa * 0.2f));

        List<Setting<?>> settings = focused.getSettings();
        if (settings != null && !settings.isEmpty()) {
            sContentH = 0;
            for (Setting<?> s : settings) {
                if (s.getVisibility() != null && !s.getVisibility().displayable()) continue;
                sContentH += MD3SettingRegistry.get().getHeight(s);
            }

            gg.enableScissor((int)sx, (int)cY, (int)(sx + sw), (int)(cY + cH));
            int sy = (int)cY - (int)sScrollY;
            for (Setting<?> s : settings) {
                if (s.getVisibility() != null && !s.getVisibility().displayable()) continue;
                int dy = MD3SettingRegistry.get().render(gg, s,
                        (int)(sx + 12f), sy, (int)(sw - 24f), mx, my, pa, cp);
                sy += dy;
            }
            gg.disableScissor();

            tickSScrollAlpha();
            renderScrollbar(gg, sx + sw - 5f, cY, cH, sScrollY, sContentH, sScrollAlpha, pa);
        } else {
            FontRenderer emptyF = MD3Theme.fontBody(1f);
            MD3Theme.text("No settings", sx + 18f, cY + 12f, emptyF, MD3Theme.TEXT_DISABLED, pa);
        }
    }

    private void renderToggle(GuiGraphics gg, float x, float y, boolean on, float hov, float a, int accent) {
        int track = MD3Theme.lerpColor(MD3Theme.SURFACE_HIGHEST, accent, on ? 1f : 0f);
        int outline = on ? accent : MD3Theme.OUTLINE_VARIANT;
        if (hov > 0.01f) {
            RenderUtil.drawRoundedRect(gg.pose(), x - 4f, y - 4f, TOGGLE_W + 8f, TOGGLE_H + 8f,
                    (TOGGLE_H + 8f) / 2f, MD3Theme.withAlpha(on ? accent : MD3Theme.SURFACE_HIGHEST, a * hov * 0.28f));
            track = MD3Theme.brighten(track, 1f + 0.1f * hov);
        }
        RenderUtil.drawRoundedRect(gg.pose(), x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2f,
                MD3Theme.withAlpha(track, a));
        RenderUtil.drawRoundedRect(gg.pose(), x + 1f, y + 1f, TOGGLE_W - 2f, TOGGLE_H - 2f, (TOGGLE_H - 2f) / 2f,
                MD3Theme.withAlpha(MD3Theme.argb(on ? 24 : 16, 255, 255, 255), a));

        float ks = on ? 14f : 12f;
        float kx = on ? x + TOGGLE_W - ks - 2f : x + 3f;
        float ky = y + (TOGGLE_H - ks) / 2f;
        RenderUtil.drawRoundedRect(gg.pose(), x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2f,
                MD3Theme.withAlpha(outline, a * (on ? 0.2f : 0.55f)));
        RenderUtil.drawRoundedRect(gg.pose(), kx, ky, ks, ks, ks / 2f,
                MD3Theme.withAlpha(on ? MD3Theme.ON_PRIMARY : MD3Theme.TEXT_MED, a));
    }

    // ──────────────── Search ────────────────

    private void renderSearch(GuiGraphics gg, float x, float y, float w, int mx, int my, float a) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + SEARCH_H;
        RenderUtil.drawRoundedRect(gg.pose(), x, y, w, SEARCH_H, SEARCH_R,
                MD3Theme.withAlpha(MD3Theme.SURFACE_CONTAINER, a));
        if (hov || searchFocus) {
            RenderUtil.drawRoundedRect(gg.pose(), x - 0.5f, y - 0.5f, w + 1, SEARCH_H + 1,
                    SEARCH_R + 0.5f, MD3Theme.withAlpha(MD3Theme.PRIMARY, a * (searchFocus ? 0.25f : 0.12f)));
        }

        // Search icon
        FontRenderer iconF = MD3Theme.fontMaterial(16f);
        GlHelper.drawText("", x + 9f, y + (SEARCH_H - iconF.getMetrics().capHeight()) / 2f + 3f,
                iconF, MD3Theme.withAlpha(MD3Theme.TEXT_LOW, a));

        float tx = x + 30f;
        if (!searching && query.isEmpty()) {
            FontRenderer pf = MD3Theme.fontBody(1f);
            MD3Theme.text("Search modules...", tx,
                    y + (SEARCH_H - pf.getMetrics().capHeight()) / 2f, pf, MD3Theme.TEXT_DISABLED, a);
        } else {
            FontRenderer qf = MD3Theme.fontBodyLarge(1f);
            float qy = y + 2f + (SEARCH_H - qf.getMetrics().capHeight()) / 2f;
            MD3Theme.text(query, tx, qy, qf, MD3Theme.TEXT_HIGH, a);
            if (searchFocus) {
                float blink = (float)(Math.sin((System.currentTimeMillis() - cursorTime) / 200.0) * 0.5 + 0.5);
                float ccx = tx + GlHelper.getStringWidth(query, qf) + 1f;
                RenderUtil.drawFilledRect(gg.pose(), ccx, qy, 0.5f, qf.getMetrics().capHeight(),
                        ((int)(blink * a * 255) << 24) | 0xFFFFFF);
            }
        }
    }

    // ──────────────── Scrollbar / Toasts ────────────────

    private void renderScrollbar(GuiGraphics gg, float x, float y, float h,
                                 float off, float total, float sbA, float a) {
        if (sbA <= 0.01f || total <= h) return;
        float max = total - h;
        float thumb = Math.max(14f, h / total * h);
        float ty = y + (off / max) * (h - thumb);
        RenderUtil.drawRoundedRect(gg.pose(), x, y, 4f, h, 2f,
                MD3Theme.withAlpha(MD3Theme.SURFACE_HIGHEST, sbA * a * 0.18f));
        RenderUtil.drawRoundedRect(gg.pose(), x, ty, 4f, thumb, 2f,
                MD3Theme.withAlpha(MD3Theme.PRIMARY, sbA * a * 0.65f));
    }

    private void tickScrollAlpha() {
        float ch = H - HEAD - 32f;
        float t = 0f;
        if (contentH > ch) {
            long s = System.currentTimeMillis() - scrollTime;
            t = s < 500 ? 1f : s < 1000 ? 1f - (s - 500) / 500f : 0f;
        }
        scrollAlpha = lerp(scrollAlpha, t, 0.3f);
    }

    private void tickSScrollAlpha() {
        float ch = H - 84f;
        float t = 0f;
        if (sContentH > ch) {
            long s = System.currentTimeMillis() - sScrollTime;
            t = s < 500 ? 1f : s < 1000 ? 1f - (s - 500) / 500f : 0f;
        }
        sScrollAlpha = lerp(sScrollAlpha, t, 0.3f);
    }

    public void addToast(String msg) {
        for (Toast t : toasts) t.targetY -= 16f;
        Toast nt = new Toast(msg);
        toasts.add(nt);
    }

    private void renderToasts(GuiGraphics gg, float px, float py, float a) {
        if (toasts.isEmpty()) return;
        FontRenderer tf = MD3Theme.fontBodyLarge(1f);
        float baseY = py - 14f;
        for (Toast t : toasts) {
            long age = System.currentTimeMillis() - t.born;
            t.y = LerpUtil.smoothLerp(t.y, t.targetY, 0.18f);
            float ta = age < 2000 ? 1f : age < 2500 ? 1f - (age - 2000) / 500f : 0f;
            t.alpha = LerpUtil.smoothLerp(t.alpha, ta, 0.2f);
            if (t.alpha >= 0.01f || age <= 2000) {
                float tw = GlHelper.getStringWidth(t.msg, tf) + 16f, th = 14f;
                float tx = px + (panelW() - tw) / 2f, ty = baseY + t.y;
                RenderUtil.drawRoundedRect(gg.pose(), tx, ty, tw, th, th / 2f,
                        MD3Theme.withAlpha(MD3Theme.SURFACE_HIGHEST, a * t.alpha));
                MD3Theme.text(t.msg, tx + 8f, ty + (th - tf.getMetrics().capHeight()) / 2f,
                        tf, MD3Theme.TEXT_HIGH, a * t.alpha);
            }
        }
        toasts.removeIf(t -> t.alpha < 0.01f && System.currentTimeMillis() - t.born > 2000);
    }

    // ──────────────── Input ────────────────

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (openT < 1f) return true;
        int mx = (int)mouseX, my = (int)mouseY;
        float px = ox(), py = oy();
        float dw = detailW();
        float dx = px + SIDEBAR_W + GAP;
        float listW = settingsAlpha > 0.05f ? (dw - GAP) * (1f - settingsAlpha * 0.45f) : dw;
        float searchY = py + HEAD + 12f;
        float searchW = listW - 24f;
        float searchX = dx + 12f;

        // Search
        if (mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + SEARCH_H) {
            if (!searching) { searching = true; query = ""; doSearch(); }
            searchFocus = true; cursorTime = System.currentTimeMillis();
            return true;
        }
        searchFocus = false;

        if (clickNav(px, py, mx, my)) return true;
        if (clickCard(px, py, mx, my, button)) return true;
        if (button == 0 && clickToggle(px, py, mx, my)) return true;
        if (clickSettings(px, py, mx, my, button)) return true;

        NumberSettingRenderer.clearEditing();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickNav(float px, float py, int mx, int my) {
        Category[] cats = Category.values();
        float catBaseY = py + CAT_TOP;
        for (int i = 0; i < cats.length; i++) {
            float cy = catBaseY + i * (CAT_H + CAT_GAP);
            if (mx >= px + 10 && mx <= px + SIDEBAR_W - 10 && my >= cy && my <= cy + CAT_H) {
                selected = cats[i]; focused = null;
                scrollY = 0; scrollTarget = 0;
                loadModules();
                if (searching) { searching = false; searchFocus = false; query = ""; }
                return true;
            }
        }
        return false;
    }

    private boolean clickCard(float px, float py, int mx, int my, int btn) {
        if (modules == null) return false;
        float dw = detailW();
        float dx = px + SIDEBAR_W + GAP;
        float listW = settingsAlpha > 0.05f ? (dw - GAP) * (1f - settingsAlpha * 0.45f) : dw;
        float cardTop = py + HEAD + 12f + SEARCH_H + 10f;
        float cardAreaH = H - (cardTop - py) - 12f;
        if (mx < dx + 12 || mx > dx + listW - 12 || my < cardTop || my > cardTop + cardAreaH) return false;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            float cardY = cardTop + i * (CARD + CARD_GAP) - scrollY;
            if (my >= cardY && my <= cardY + CARD) {
                if (btn == 0) { m.toggle(); addToast(m.getName() + (m.isEnabled() ? " On" : " Off")); }
                else if (btn == 1) {
                    if (focused == m) { focused = null; }
                    else { focused = m; sScrollY = 0; sScrollTarget = 0; }
                }
                return true;
            }
        }
        return false;
    }

    private boolean clickToggle(float px, float py, int mx, int my) {
        if (focused == null || settingsAlpha < 0.5f) return false;
        float dw = detailW();
        float dx = px + SIDEBAR_W + GAP;
        float sx = dx + dw - SETTINGS_W * settingsAlpha;
        float sw = SETTINGS_W * settingsAlpha;
        float ttx = sx + sw - TOGGLE_W - 18f, tty = py + 26f;
        if (mx >= ttx && mx <= ttx + TOGGLE_W && my >= tty && my <= tty + TOGGLE_H) {
            focused.toggle();
            addToast(focused.getName() + (focused.isEnabled() ? " On" : " Off"));
            return true;
        }
        return false;
    }

    private boolean clickSettings(float px, float py, int mx, int my, int btn) {
        if (focused == null || settingsAlpha < 0.5f) return false;
        List<Setting<?>> ss = focused.getSettings();
        if (ss == null || ss.isEmpty()) return false;
        float dw = detailW();
        float dx = px + SIDEBAR_W + GAP;
        float sx = dx + dw - SETTINGS_W * settingsAlpha;
        float sw = SETTINGS_W * settingsAlpha;
        float cY = py + 72f, cH = H - 84f;
        if (mx < sx + 12 || mx > sx + sw - 12 || my < cY || my > cY + cH) return false;
        int sy = (int)cY - (int)sScrollY;
        for (Setting<?> s : ss) {
            if (s.getVisibility() != null && !s.getVisibility().displayable()) continue;
            int h = MD3SettingRegistry.get().getHeight(s);
            if (my >= sy && my <= sy + h) {
                if (MD3SettingRegistry.get().onClick(s,
                        (int)(sx + 12f), sy, (int)(sw - 24f), mx, my, btn, 1f))
                    return true;
            }
            sy += h;
        }
        return false;
    }

    public boolean mouseReleased(double mx, double my, int btn) {
        MD3SettingRegistry.get().onMouseRelease(mx, my, btn);
        return super.mouseReleased(mx, my, btn);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (focused == null || settingsAlpha < 0.5f) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        List<Setting<?>> ss = focused.getSettings();
        if (ss != null) {
            for (Setting<?> s : ss) {
                if (s.getVisibility() != null && !s.getVisibility().displayable()) continue;
                MD3SettingRegistry.get().onMouseDrag(s, mouseX, mouseY, button);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (openT < 1f) return true;
        int mxI = (int)mx, myI = (int)my;
        float px = ox(), py = oy();
        float dw = detailW();
        float dx = px + SIDEBAR_W + GAP;

        // Module list scroll
        float listW = settingsAlpha > 0.05f ? (dw - GAP) * (1f - settingsAlpha * 0.45f) : dw;
        float cardTop = py + HEAD + 12f + SEARCH_H + 10f;
        float cardAreaH = H - (cardTop - py) - 12f;
        if (mxI >= dx && mxI <= dx + listW && myI >= cardTop && myI <= cardTop + cardAreaH) {
            if (contentH > cardAreaH) {
                scrollTarget -= (float)delta * 20f;
                scrollTarget = Math.max(0, Math.min(scrollTarget, contentH - cardAreaH));
                scrollTime = System.currentTimeMillis();
            }
            return true;
        }

        // Settings scroll
        if (settingsAlpha > 0.05f && focused != null) {
            float sx = dx + dw - SETTINGS_W * settingsAlpha;
            float sw = SETTINGS_W * settingsAlpha;
            if (mxI >= sx && mxI <= sx + sw && myI >= py && myI <= py + H) {
                float sCH = H - 84f;
                if (sContentH > sCH) {
                    sScrollTarget -= (float)delta * 20f;
                    sScrollTarget = Math.max(0, Math.min(sScrollTarget, sContentH - sCH));
                    sScrollTime = System.currentTimeMillis();
                }
                return true;
            }
        }
        return super.mouseScrolled(mx, my, delta);
    }

    public boolean keyPressed(int key, int scan, int mod) {
        if (NumberSettingRenderer.onKeyPress(key, scan, mod)) return true;
        if (searching) {
            if (key == 256) { searching = false; searchFocus = false; query = ""; loadModules(); return true; }
            if (searchFocus) {
                cursorTime = System.currentTimeMillis();
                if (key == 259 && !query.isEmpty()) { query = query.substring(0, query.length() - 1); doSearch(); return true; }
            }
        }
        if (key == 256 && !searching) { onClose(); return true; }
        return super.keyPressed(key, scan, mod);
    }

    public boolean charTyped(char c, int mod) {
        if (searching && searchFocus) { cursorTime = System.currentTimeMillis(); query += c; doSearch(); return true; }
        if (NumberSettingRenderer.onCharTyped(c)) return true;
        return super.charTyped(c, mod);
    }

    // ──────────────── Helpers ────────────────

    private boolean isToggleHovered(float dx, float py, Module m, int mx, int my) {
        if (m == null) return false;
        float dw = detailW();
        float sx = dx + dw - SETTINGS_W * settingsAlpha;
        float sw = SETTINGS_W * settingsAlpha;
        float ttx = sx + sw - TOGGLE_W - 18f, tty = py + 26f;
        return mx >= ttx && mx <= ttx + TOGGLE_W && my >= tty && my <= tty + TOGGLE_H;
    }

    private int categoryCount(Category category) {
        return NiloreClient.instance.getModuleManager() != null
                ? (int) NiloreClient.instance.getModuleManager().getModules().stream()
                .filter(m -> m.getCategory() == category).count()
                : 0;
    }

    private void syncTheme() {
        try {
            ClickGuiModule clickGui = NiloreClient.instance.getModuleManager().getModule(ClickGuiModule.class);
            MD3Theme.useLight(clickGui.materialTheme.is("Light"));
        } catch (Exception ignored) {
            MD3Theme.useLight(false);
        }
    }

    private void doSearch() {
        if (query.isEmpty()) { loadModules(); return; }
        modules = NiloreClient.instance.getModuleManager().getModules().stream()
                .filter(m -> m.getName().toLowerCase().contains(query.toLowerCase()))
                .sorted(Comparator.comparing(Module::getName)).collect(Collectors.toList());
        scrollY = 0; scrollTarget = 0;
    }

    private void loadModules() {
        modules = NiloreClient.instance.getModuleManager().getModules().stream()
                .filter(m -> m.getCategory() == selected)
                .sorted(Comparator.comparing(Module::getName)).collect(Collectors.toList());
    }

    private void tickState() {
        switch (state) {
            case OPENING -> { openT = LerpUtil.lerp(openT, 1f, 0.08f); if (openT >= 1f) state = State.OPEN; }
            case CLOSING -> {
                openT = LerpUtil.lerp(openT, 0f, 0.1f);
                if (openT <= 0f) {
                    state = State.CLOSED;
                    if (NiloreClient.isReady()) NiloreClient.instance.getConfigManager().saveAll();
                    minecraft.setScreen(null);
                }
            }
        }
    }

    private static float ease(float t) { return (float)(1 - Math.pow(1 - t, 3)); }
    private static float lerp(float c, float t, float s) {
        return Math.abs(t - c) > 0.005f ? LerpUtil.smoothLerp(c, t, s) : t;
    }
}
