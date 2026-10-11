package client.nilore.hud;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import net.minecraft.client.renderer.texture.DynamicTexture;
import client.nilore.event.impl.GlRenderEvent;
import client.nilore.event.impl.Render2DEvent;
import client.nilore.modules.impl.misc.MusicPlayer;
import client.nilore.modules.impl.misc.music.AudioPlayer;
import client.nilore.modules.impl.misc.music.MusicHttp;
import client.nilore.modules.impl.misc.music.NeteaseApi;
import client.nilore.modules.impl.misc.music.SongInfo;
import client.nilore.render.DrawContext;
import client.nilore.render.FontPresets;
import client.nilore.render.FontRenderer;
import client.nilore.render.GlHelper;
import client.nilore.render.Paint;
import client.nilore.render.Rectangle;
import client.nilore.render.RoundedRectangle;
import client.nilore.render.RoundedRectShader;
import client.nilore.render.Texture;
import client.nilore.settings.impl.BooleanSetting;
import client.nilore.settings.impl.ModeSetting;
import client.nilore.settings.impl.NumberSetting;
import client.nilore.utils.animation.SmoothAnimationTimer;
import client.nilore.utils.math.Easings;
import client.nilore.utils.render.ColorUtil;
import client.nilore.utils.render.RenderUtil;

public class MusicPlayerHud extends HudElement {
    private static final float SCALE = 1.1f;
    private final SmoothAnimationTimer showAnim = new SmoothAnimationTimer();
    private float titleScrollOffset = 0;
    private long titleScrollTimestamp = 0;

    private final NumberSetting bgAlphaSetting = new NumberSetting("Bg Alpha", 120, 0, 255, 1);
    private final ModeSetting styleSetting = new ModeSetting("Style", "Simple", "Material3").withDefault("Simple");
    private final BooleanSetting glowSetting = new BooleanSetting("Glow", false);
    private final NumberSetting glowRadiusSetting = new NumberSetting("Glow Radius", 12, 4, 40, 1);
    private final NumberSetting glowAlphaSetting = new NumberSetting("Glow Alpha", 120, 0, 255, 1);

    private static final float SIMPLE_ART_SIZE = 32;
    private static final float SIMPLE_GAP = 8;
    private static final float SIMPLE_PAD = 8;
    private static final float SIMPLE_BAR_H = SIMPLE_ART_SIZE + SIMPLE_PAD * 2;
    private static final float SIMPLE_TEXT_W = 160;
    private static final float SIMPLE_BAR_W = SIMPLE_PAD + SIMPLE_ART_SIZE + SIMPLE_GAP + SIMPLE_TEXT_W + SIMPLE_PAD;
    private static final float SIMPLE_RADIUS = 10;
    private static final float SIMPLE_BAR_HEIGHT = 2;

    private final FontRenderer titleFont = FontPresets.pingfang(17);
    private final FontRenderer artistFont = FontPresets.pingfang(14);
    private final FontRenderer timeFont = FontPresets.pingfang(13);
    private final FontRenderer materialTitleFont = FontPresets.pingfang(16 * SCALE);
    private final FontRenderer materialArtistFont = FontPresets.pingfang(12 * SCALE);
    private final FontRenderer materialTimeFont = FontPresets.pingfang(11 * SCALE);
    private final FontRenderer coverTitleFont = FontPresets.pingfang(18.0f * SCALE);
    private final FontRenderer coverArtistFont = FontPresets.pingfang(12.0f * SCALE);

    // album art cache
    private volatile Texture albumTexture;
    private long albumSongId = -1;
    private volatile boolean albumLoading = false;
    private volatile byte[] albumBytes;
    private int albumRetryCount = 0;

    public MusicPlayerHud() {
        super("MusicPlayerHud");
        this.setWidth(SIMPLE_BAR_W);
        this.setHeight(SIMPLE_BAR_H);
    }

    @Override
    public void registerSettings() {
        this.registerSetting(styleSetting, bgAlphaSetting, glowSetting, glowRadiusSetting, glowAlphaSetting);
    }

    @Override
    public void onRender2D(Render2DEvent render2DEvent, float x, float y) {
    }

    @Override
    public void onGlRender(GlRenderEvent glRenderEvent, float x, float y) {
        AudioPlayer player = MusicPlayer.AUDIO_PLAYER;

        // Stop if not in a world (title screen, etc.)
        if (mc.level == null) {
            player.stop();
            showAnim.animate(0.0, 0.3, Easings.EASE_OUT_POW3);
            showAnim.tick();
            this.setWidth(0);
            this.setHeight(0);
            return;
        }
        boolean hasSong = player.getCurrentSong() != null && player.getState() != AudioPlayer.State.STOPPED;

        showAnim.animate(hasSong ? 1.0 : 0.0, 0.3, Easings.EASE_OUT_POW3);
        showAnim.tick();
        float a = showAnim.getValueF();
        if (a < 0.01f) {
            this.setWidth(0);
            this.setHeight(0);
            return;
        }

        SongInfo song = player.getCurrentSong();
        if (song == null) return;

        // load album art
        loadAlbumArt(song);
        if (albumBytes != null) {
            try {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(albumBytes));
                DynamicTexture dyn = new DynamicTexture(img);
                albumTexture = new Texture(dyn.getId(), img.getWidth(), img.getHeight());
            } catch (Exception e) {
                System.err.println("[MusicPlayerHud] Failed to create album texture: " + e.getMessage());
            }
            albumBytes = null;
        }

        DrawContext ctx = glRenderEvent.drawContext();
        if (ctx == null) return;

        float progress = Math.max(0.0f, Math.min(1.0f, player.getProgress()));

        if (styleSetting.is("Material3")) {
            renderMaterial3(ctx, x, y, song, player, progress, a);
            return;
        }

        // background
        // Glow behind background
        if (glowSetting.getValue()) {
            float gRadius = glowRadiusSetting.getValue().floatValue();
            int gAlpha = glowAlphaSetting.getValue().intValue();
            if (gAlpha > 0 && gRadius > 0.0f) {
                RenderUtil.drawShadow(ctx.getPoseStack(),
                        x, y, SIMPLE_BAR_W, SIMPLE_BAR_H, (int) gRadius, (int) ((gAlpha << 24) | 0x000000));
                RenderUtil.enableBlend();
            }
        }
        int bgAlpha = bgAlphaSetting.getValue().intValue();
        ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(x, y, SIMPLE_BAR_W, SIMPLE_BAR_H, SIMPLE_RADIUS),
                new Paint().setColor(ColorUtil.fromARGB(0, 0, 0, (int) (bgAlpha * a))));

        // album art (rounded, equal padding)
        float artX = x + SIMPLE_PAD;
        float artY = y + SIMPLE_PAD;
        float artRadius = 6;
        if (albumTexture != null) {
            int artColor = ColorUtil.fromARGB(255, 255, 255, (int)(255 * a));
            org.joml.Matrix4f pose = ctx.getPoseStack().last().pose();
            DrawContext.getRoundedRectShader().drawTextured(pose,
                    artX, artY, artX + SIMPLE_ART_SIZE, artY + SIMPLE_ART_SIZE,
                    artRadius, artRadius, artRadius, artRadius,
                    artColor, albumTexture.getGlId(), 0, 0, 1, 1);
        } else {
            ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(artX, artY, SIMPLE_ART_SIZE, SIMPLE_ART_SIZE, artRadius),
                    new Paint().setColor(ColorUtil.fromARGB(255, 255, 255, (int)(15 * a))));
        }

        // text area (to the right of album art)
        float textX = artX + SIMPLE_ART_SIZE + SIMPLE_GAP;
        float textMaxW = SIMPLE_TEXT_W;
        String title = song.name;
        float titleW = GlHelper.getStringWidth(title, titleFont);

        ctx.save();
        ctx.clipRect(Rectangle.ofXYWH(textX, y + 2, textMaxW, SIMPLE_BAR_H - 4), true);
        GlHelper.drawText(title, textX - titleScrollOffset, y + SIMPLE_PAD, titleFont,
                ColorUtil.fromARGB(255, 255, 255, (int)(255 * a)));
        ctx.restore();

        if (titleW > textMaxW) {
            if (System.currentTimeMillis() - titleScrollTimestamp > 2000) {
                titleScrollOffset += 0.3f;
                if (titleScrollOffset > titleW + 16) {
                    titleScrollOffset = -textMaxW;
                    titleScrollTimestamp = System.currentTimeMillis();
                }
            }
        } else {
            titleScrollOffset = 0;
            titleScrollTimestamp = System.currentTimeMillis();
        }

        GlHelper.drawText(song.artist, textX, y + SIMPLE_PAD + 12, artistFont,
                ColorUtil.fromARGB(255, 255, 255, (int)(120 * a)));

        float barY = y + SIMPLE_BAR_H - SIMPLE_PAD - SIMPLE_BAR_HEIGHT - 3;
        float barW = textMaxW;

        ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(textX, barY, barW, SIMPLE_BAR_HEIGHT, SIMPLE_BAR_HEIGHT / 2),
                new Paint().setColor(ColorUtil.fromARGB(255, 255, 255, (int)(30 * a))));

        if (progress > 0.001f) {
            ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(textX, barY, barW * progress, SIMPLE_BAR_HEIGHT, SIMPLE_BAR_HEIGHT / 2),
                    new Paint().setColor(ColorUtil.fromARGB(255, 255, 255, (int)(180 * a))));
        }

        String elapsed = formatMs(player.getCurrentPositionMs());
        String total = song.formatDuration();
        int timeAlpha = (int)(100 * a);
        GlHelper.drawText(elapsed, textX, barY + 5, timeFont,
                ColorUtil.fromARGB(255, 255, 255, timeAlpha));
        float totalW = GlHelper.getStringWidth(total, timeFont);
        GlHelper.drawText(total, textX + barW - totalW, barY + 5, timeFont,
                ColorUtil.fromARGB(255, 255, 255, timeAlpha));

        this.setWidth(SIMPLE_BAR_W);
        this.setHeight(SIMPLE_BAR_H);
    }

    private void renderMaterial3(DrawContext ctx, float x, float y, SongInfo song,
                                  AudioPlayer player, float progress, float alpha) {
        final float width = 142.5f;
        final float height = 43.0f;
        final float padding = 4.0f * SCALE;
        final float artSize = 32.0f * SCALE;
        final float artRadius = 4.0f * SCALE;
        final float buttonSize = 20.0f * SCALE;
        final float buttonRadius = buttonSize * 0.5f;
        final float artistWidth = GlHelper.getStringWidth(song.artist, coverArtistFont);
        final float artistRequiredWidth = 42.0f * SCALE + artistWidth + padding + buttonSize + 5.0f * SCALE;
        final float renderWidth = Math.max(width * SCALE, artistRequiredWidth);
        final float renderHeight = height * SCALE + 2.0f;
        final float renderY = y + (1.0f - alpha) * 1.2f * SCALE;
        final float textX = x + 42.0f * SCALE;
        final float textRight = x + renderWidth - padding - buttonSize - 5.0f * SCALE;
        final float textWidth = Math.max(0.0f, textRight - textX);
        final int background = ColorUtil.fromARGB(57, 43, 49, (int) (alpha * 220.0f));
        final int border = ColorUtil.fromARGB(86, 66, 73, (int) (alpha * 148.0f));
        final int buttonBackground = ColorUtil.fromARGB(104, 76, 86, (int) (alpha * 234.0f));
        final int primaryText = ColorUtil.fromARGB(245, 236, 235, (int) (alpha * 255.0f));
        final int secondaryText = ColorUtil.fromARGB(205, 185, 186, (int) (alpha * 224.0f));
        final int detailText = ColorUtil.fromARGB(176, 159, 160, (int) (alpha * 208.0f));
        final int track = ColorUtil.fromARGB(108, 96, 102, (int) (alpha * 200.0f));
        final int progressColor = ColorUtil.fromARGB(247, 181, 204, (int) (alpha * 255.0f));

        if (glowSetting.getValue()) {
            float glowRadius = glowRadiusSetting.getValue().floatValue();
            int glowAlpha = glowAlphaSetting.getValue().intValue();
            if (glowRadius > 0.0f && glowAlpha > 0) {
                RenderUtil.drawShadow(ctx.getPoseStack(), x, renderY, renderWidth, renderHeight, (int) glowRadius,
                        (int) ((int) (glowAlpha * alpha) << 24));
                RenderUtil.enableBlend();
            }
        }

        ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(x, renderY, renderWidth, renderHeight, 10.0f * SCALE),
                new Paint().setColor(background));
        ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(x, renderY, renderWidth, renderHeight, 10.0f * SCALE),
                new Paint().setColor(border));

        float artX = x + padding;
        float artY = renderY + 3.0f * SCALE;
        if (albumTexture != null) {
            int artColor = ColorUtil.fromARGB((int) (255 * alpha), 255, 255, 255);
            org.joml.Matrix4f pose = ctx.getPoseStack().last().pose();
            DrawContext.getRoundedRectShader().drawTextured(pose,
                    artX, artY, artX + artSize, artY + artSize,
                    artRadius, artRadius, artRadius, artRadius,
                    artColor, albumTexture.getGlId(), 0, 0, 1, 1);
        } else {
            ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(artX, artY, artSize, artSize, artRadius),
                    new Paint().setColor(ColorUtil.fromARGB(78, 60, 66, (int) (alpha * 230.0f))));
        }

        String title = song.name;
        float titleWidth = GlHelper.getStringWidth(title, coverTitleFont);
        ctx.save();
        ctx.clipRect(Rectangle.ofXYWH(textX, renderY + 6.5f * SCALE, textWidth, 15.0f * SCALE), true);
        GlHelper.drawText(title, textX - titleScrollOffset, renderY + 9.5f * SCALE, coverTitleFont, primaryText);
        ctx.restore();

        if (titleWidth > textWidth) {
            if (System.currentTimeMillis() - titleScrollTimestamp > 2000) {
                titleScrollOffset += 0.20f;
                if (titleScrollOffset > titleWidth + 10.0f) {
                    titleScrollOffset = -textWidth;
                    titleScrollTimestamp = System.currentTimeMillis();
                }
            }
        } else {
            titleScrollOffset = 0.0f;
            titleScrollTimestamp = System.currentTimeMillis();
        }

        GlHelper.drawText(song.artist, textX, renderY + 24.0f * SCALE, coverArtistFont, secondaryText);

        float buttonX = x + renderWidth - 4.0f * SCALE - buttonSize;
        float buttonY = renderY + (height - buttonSize) * 0.5f;
        ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(buttonX, buttonY, buttonSize, buttonSize, buttonRadius),
                new Paint().setColor(buttonBackground));
        float barW = 2.0f * SCALE;
        float barH = 8.0f * SCALE;
        float barGap = 2.4f * SCALE;
        float iconX = buttonX + (buttonSize - (barW * 2.0f + barGap)) * 0.5f;
        float iconY = buttonY + (buttonSize - barH) * 0.5f;
        ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(iconX, iconY, barW, barH, 2.0f), new Paint().setColor(primaryText));
        ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(iconX + barW + barGap, iconY, barW, barH, 2.0f), new Paint().setColor(primaryText));

        float progressX = artX;
        float progressY = renderY + artSize + 6.0f * SCALE;
        float progressW = renderWidth - padding * 2.0f - 3.0f;
        final float progressHeight = 1.5f * SCALE + 2.0f;
        final float progressGap = 3.0f;
        final float progressRadius = progressHeight * 0.5f;
        if (progress <= 0.001f) {
            ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(progressX, progressY, progressW, progressHeight, progressRadius),
                    new Paint().setColor(track));
        } else if (progress >= 0.999f) {
            ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(progressX, progressY, progressW, progressHeight, progressRadius),
                    new Paint().setColor(progressColor));
        } else {
            float splitX = progressX + progressW * progress;
            float halfGap = progressGap * 0.5f;
            float playedWidth = splitX - halfGap - progressX;
            float remainingX = splitX + halfGap;
            float remainingWidth = progressX + progressW - remainingX;

            if (playedWidth > 0.0f) {
                ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(progressX, progressY,
                                playedWidth, progressHeight, progressRadius),
                        new Paint().setColor(progressColor));
            }
            if (remainingWidth > 0.0f) {
                ctx.drawRoundedRect(RoundedRectangle.ofXYWHR(remainingX, progressY,
                                remainingWidth, progressHeight, progressRadius),
                        new Paint().setColor(track));
            }
        }

        this.setWidth(renderWidth);
        this.setHeight(renderHeight);
    }

    private void loadAlbumArt(SongInfo song) {
        if (albumLoading) return;
        if (song.id == albumSongId && albumTexture != null) return;
        if (song.id == albumSongId && albumRetryCount >= 2) return;
        if (song.id != albumSongId) {
            albumRetryCount = 0;
            titleScrollOffset = 0.0f;
            titleScrollTimestamp = System.currentTimeMillis();
        }
        albumSongId = song.id;
        albumTexture = null;
        albumBytes = null;
        albumLoading = true;
        albumRetryCount++;

        NeteaseApi.getAlbumPicUrl(song).thenAccept(picUrl -> {
            if (picUrl == null || picUrl.isEmpty()) { albumLoading = false; return; }
            try {
                byte[] bytes = MusicHttp.getBytes(URI.create(picUrl));
                if (bytes != null && bytes.length > 100) {
                    albumBytes = bytes;
                } else {
                    System.err.println("[MusicPlayerHud] Album art too small, retrying...");
                    albumSongId = -1;
                }
            } catch (Exception e) {
                System.err.println("[MusicPlayerHud] Failed to download album art: " + e.getMessage());
                albumSongId = -1;
            } finally {
                albumLoading = false;
            }
        }).exceptionally(e -> {
            albumLoading = false;
            albumSongId = -1;
            return null;
        });
    }

    private String formatMs(long ms) {
        long sec = ms / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    @Override
    public void onSettings() {
    }
}
