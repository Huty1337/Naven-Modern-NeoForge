package client.nilore.hud.target;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SwordItem;
import client.nilore.event.impl.Render2DEvent;
import client.nilore.modules.impl.render.NameProtect;
import client.nilore.render.FontPresets;
import client.nilore.render.FontRenderer;
import client.nilore.render.GlHelper;
import client.nilore.render.Paint;
import client.nilore.render.Renderer;
import client.nilore.utils.animation.SmoothAnimationTimer;
import client.nilore.utils.math.Easings;
import client.nilore.utils.render.RenderUtil;

public class NavenTargetStyle
        extends TargetStyle {
    private static final Color COLOR_PANEL_BG = new Color(23, 22, 38, 200);
    private static final Color COLOR_HEALTH_BG = new Color(0, 0, 0, 90);
    private static final Color COLOR_HEALTH_BAR = new Color(148, 42, 43);
    private final FontRenderer nameFont;
    private final FontRenderer subFont;
    private final SmoothAnimationTimer scaleAnim = new SmoothAnimationTimer();
    private final SmoothAnimationTimer fadeAnim;
    private final SmoothAnimationTimer slideAnim;
    private final SmoothAnimationTimer contentAnim;
    private boolean visible = false;
    private LivingEntity currentTarget;
    private long lastActiveTime = 0L;
    private final Paint panelPaint = new Paint();
    private final Paint healthBgPaint = new Paint();
    private final Paint healthBarPaint = new Paint();

    public float avatarOffsetY=-3.5f;
    public float textOffsetY=2f;
    public float nameOffsetY=3.5f;

    public NavenTargetStyle() {
        super("Naven");
        this.nameFont = FontPresets.pingfang(15.0f);
        this.subFont = FontPresets.astaSans(14.0f);
        this.fadeAnim = new SmoothAnimationTimer();
        this.fadeAnim.setCurrentValue(0.0);
        this.slideAnim = new SmoothAnimationTimer();
        this.slideAnim.setCurrentValue(5.0);
        this.contentAnim = new SmoothAnimationTimer();
        this.contentAnim.setCurrentValue(0.0);
    }

    @Override
    public void render(Render2DEvent render2DEvent, LivingEntity livingEntity, SmoothAnimationTimer healthAnim, SmoothAnimationTimer healthLagAnim, float healthPct, float x, float y) {
        float fade;
        boolean hasTarget = livingEntity != null;
        long now = System.currentTimeMillis();
        boolean targetChanged = false;
        if (hasTarget) {
            this.lastActiveTime = now;
            if (this.currentTarget != livingEntity) {
                this.currentTarget = livingEntity;
                targetChanged = true;
            }
        }
        boolean shouldShow = hasTarget || now - this.lastActiveTime < 300L;
        if (shouldShow != this.visible || (shouldShow && this.fadeAnim.getValueF() <= 0.01f)) {
            this.visible = shouldShow;
            if (this.visible) {
                this.fadeAnim.animate(1.0, 0.2, Easings.EASE_OUT_POW3);
                this.slideAnim.setCurrentValue(5.0);
                this.slideAnim.setStartTime(0L);
                this.contentAnim.setCurrentValue(0.0);
                this.contentAnim.setStartTime(0L);
            } else {
                this.fadeAnim.animate(0.0, 0.15, Easings.EASE_IN_POW3);
                this.slideAnim.animate(5.0, 0.15, Easings.EASE_IN_POW3);
                this.contentAnim.animate(0.0, 0.15, Easings.EASE_IN_POW3);
            }
        } else if (targetChanged && this.visible) {
            this.fadeAnim.animate(1.0, 0.2, Easings.EASE_OUT_POW3);
            this.slideAnim.setCurrentValue(5.0);
            this.slideAnim.setStartTime(0L);
            this.contentAnim.setCurrentValue(0.0);
            this.contentAnim.setStartTime(0L);
        }
        this.fadeAnim.tick();
        if (this.fadeAnim.isAnimating() && this.visible) {
            if (this.fadeAnim.getProgress() >= 0.08 && this.slideAnim.getStartTime() == 0L) {
                this.slideAnim.animate(0.0, 0.3, Easings.EASE_OUT_POW3);
            }
            if (this.fadeAnim.getProgress() >= 0.15 && this.contentAnim.getStartTime() == 0L) {
                this.contentAnim.animate(1.0, 0.4, Easings.EASE_OUT_POW3);
            }
        }
        if (this.slideAnim.getStartTime() != 0L) {
            this.slideAnim.tick();
        }
        if (this.contentAnim.getStartTime() != 0L) {
            this.contentAnim.tick();
        }
        if ((fade = this.fadeAnim.getValueF()) <= 0.01f) {
            return;
        }
        LivingEntity target = this.currentTarget;
        if (target == null) {
            return;
        }

        String displayName = target == mc.player ? NameProtect.getProtectedName() : target.getName().getString();
        float nameWidth = GlHelper.getStringWidth(displayName, this.nameFont);
        float panelWidth = Math.max(120.0f, nameWidth + 53.0f);
        float panelHeight = 56.0f;

        float avYOff = this.avatarOffsetY;
        float txtYOff = this.textOffsetY;
        float nmYOff = this.nameOffsetY;

        PoseStack poseStack = render2DEvent.guiGraphics().pose();
        poseStack.pushPose();
        RenderUtil.drawBlurredRect(poseStack, x, y, panelWidth, panelHeight, 6.0f, 15.0f, 0.95f * fade, 0);
        poseStack.popPose();

        Renderer.renderConsumer((drawContext -> {
            this.panelPaint.setColor(new Color(COLOR_PANEL_BG.getRed(), COLOR_PANEL_BG.getGreen(), COLOR_PANEL_BG.getBlue(), (int)((float)COLOR_PANEL_BG.getAlpha() * fade)).getRGB());
            GlHelper.drawRoundedRect(x, y, panelWidth, panelHeight, 6.0f, this.panelPaint);

            if (target instanceof AbstractClientPlayer abstractClientPlayer) {
                GlHelper.drawPlayerHeadRounded(abstractClientPlayer, x + 9.0f, y + 12.0f + avYOff, 32.0f, 32.0f, fade, 2.0f);
            }

            float slideOff = this.slideAnim.getValueF();

            String healthStr = "Health: " + Math.round(target.getHealth() * 10) / 10f;
            float dist = mc.player != null ? Math.round(mc.player.distanceTo(target) * 10) / 10f : 0;
            String distStr = "Distance: " + dist;
            String usingStr;
            if (target.getMainHandItem().getItem() instanceof SwordItem) {
                usingStr = target.isBlocking() ? "Blocking" : "Not Blocking";
            } else {
                usingStr = target.isUsingItem() ? "Using" : "Not Using";
            }

            int nameColor = new Color(COLOR_HEALTH_BAR.getRed(), COLOR_HEALTH_BAR.getGreen(), COLOR_HEALTH_BAR.getBlue(), (int)(255 * fade)).getRGB();
            int textARGB = new Color(255, 255, 255, (int)(255 * fade)).getRGB();

            GlHelper.drawTextShadowLegacy(displayName, x + 46.0f, y + 7.0f + nmYOff, this.nameFont, nameColor);
            GlHelper.drawTextShadowLegacy(healthStr, x + 46.0f, y + 19.0f + slideOff + txtYOff, this.subFont, textARGB);
            GlHelper.drawTextShadowLegacy(distStr, x + 46.0f, y + 27.0f + slideOff + txtYOff, this.subFont, textARGB);
            GlHelper.drawTextShadowLegacy(usingStr, x + 46.0f, y + 35.0f + slideOff + txtYOff, this.subFont, textARGB);

            float barX = x + 8.0f;
            float barY = y + 47.0f;
            float barW = (panelWidth - 16.0f);
            float barH = 4.0f;
            this.healthBgPaint.setColor(new Color(COLOR_HEALTH_BG.getRed(), COLOR_HEALTH_BG.getGreen(), COLOR_HEALTH_BG.getBlue(), (int)((float)COLOR_HEALTH_BG.getAlpha() * fade)).getRGB());
            GlHelper.drawRoundedRect(barX, barY, barW, barH, 2.0f, this.healthBgPaint);
            float contentVal = this.contentAnim.getValueF();
            float barFill = healthAnim.getValueF() * barW * contentVal;
            this.healthBarPaint.setColor(new Color(COLOR_HEALTH_BAR.getRed(), COLOR_HEALTH_BAR.getGreen(), COLOR_HEALTH_BAR.getBlue(), (int)(255.0f * fade)).getRGB());
            GlHelper.drawRoundedRect(barX, barY, barFill, barH, 2.0f, this.healthBarPaint);
        }));
    }
}
