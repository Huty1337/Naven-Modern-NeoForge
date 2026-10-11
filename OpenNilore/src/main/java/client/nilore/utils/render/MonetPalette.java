package client.nilore.utils.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.FastColor;

/**
 * Material You (Monet) 动态取色。
 *
 * <p>从封面图里挑一个主色，再按 Material 3 的 tone 体系生成一整套深色主题配色。
 * 流程：采样 → 转 LCh → 按色相分桶投票选主色 → 生成 TonalPalette → 按 tone 取色（带色域裁剪）。
 *
 * <p>算法本身来自 Google 公开的 Material 3 设计规范（配色部分），这里是自己实现的，
 * 没有移植任何第三方代码。
 *
 * <p>注意色相插值走的是色相环短弧，否则换歌时从红色渐变到紫色会绕一整圈经过黄绿青蓝。
 */
public final class MonetPalette {

    /** 色相分桶数。多了容易被噪声带偏，少了主色不够准，30 是实测的平衡点。 */
    private static final int HUE_BUCKETS = 30;

    /** 色域裁剪的二分次数。20 次能把色度误差压到 1e-6 量级。 */
    private static final int GAMUT_STEPS = 20;

    /** 取色下限，避免从灰度图里取出一个几乎没颜色的主色。 */
    private static final float MIN_CHROMA = 20.0f;

    /**
     * 中性色色度。取 Material 3 规范值（neutral = 4、neutralVariant = 8）。
     *
     * <p>这两个数字决定背景「有多灰」。调大会让深色背景明显泛出主色的补色，
     * 观感发浑；调到 0 则整块背景纯灰，和主色完全脱钩。4/8 是规范里调好的平衡点。
     */
    private static final float NEUTRAL_CHROMA = 4.0f;
    private static final float NEUTRAL_VARIANT_CHROMA = 8.0f;

    private MonetPalette() {
    }

    /** LCh 颜色：亮度 0-100、色度、色相 0-360。 */
    public record Lch(float l, float c, float h) {
    }

    // ------------------------------------------------------------------
    // 色调板
    // ------------------------------------------------------------------

    /**
     * 一个色调板由「色相 + 色度」定义，按 tone（0=黑，100=白）取色。
     *
     * <p>取色时会二分查找当前亮度下不超过 sRGB 色域的最大色度。直接 clamp RGB 会偏色，
     * 二分出来的才是「能显示的最接近那个色」。
     */
    public static final class TonalPalette {
        private final float hue;
        private final float chroma;

        public TonalPalette(float hue, float chroma) {
            this.hue = normalizeHue(hue);
            this.chroma = Math.max(0.0f, chroma);
        }

        public float hue() {
            return this.hue;
        }

        public float chroma() {
            return this.chroma;
        }

        public int at(float tone) {
            float l = clamp(tone, 0.0f, 100.0f);
            float low = 0.0f;
            float high = this.chroma;
            for (int i = 0; i < GAMUT_STEPS; i++) {
                float mid = (low + high) * 0.5f;
                if (inGamut(l, mid, this.hue)) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            return lchToArgb(l, low, this.hue);
        }
    }

    /**
     * 一整套深色主题配色，字段语义对应 Material 3 的 color role。
     *
     * <p>tone 取值是 M3 的 dark scheme（浅色主题的 surface 是 99，深色是 6，差得很远，
     * 不能套用浅色那一套）。
     */
    public static final class Scheme {
        public final TonalPalette primary;
        public final TonalPalette secondary;
        public final TonalPalette tertiary;
        public final TonalPalette neutral;
        public final TonalPalette neutralVariant;

        public Scheme(TonalPalette primary, TonalPalette secondary, TonalPalette tertiary,
                      TonalPalette neutral, TonalPalette neutralVariant) {
            this.primary = primary;
            this.secondary = secondary;
            this.tertiary = tertiary;
            this.neutral = neutral;
            this.neutralVariant = neutralVariant;
        }

        // --- 主色 ---
        public int primary() { return this.primary.at(80); }
        public int primaryContainer() { return this.primary.at(30); }
        public int primaryBright() { return this.primary.at(90); }

        // --- 次级 ---
        public int secondaryContainer() { return this.secondary.at(30); }
        public int secondaryBright() { return this.secondary.at(90); }

        // --- 三级（对比色，用于徽章之类） ---
        public int tertiaryContainer() { return this.tertiary.at(30); }
        public int tertiaryBright() { return this.tertiary.at(90); }

        // --- 表面层级：数字越大越「浮起来」 ---
        public int surfaceLowest() { return this.neutral.at(5); }
        public int surfaceContainer() { return this.neutral.at(11); }
        public int surfaceHigh() { return this.neutral.at(16); }
        public int surfaceHighest() { return this.neutral.at(21); }

        // --- 文字 ---
        public int onSurface() { return this.neutral.at(92); }
        public int onSurfaceVariant() { return this.neutralVariant.at(80); }
        public int outline() { return this.neutralVariant.at(60); }
        public int outlineVariant() { return this.neutralVariant.at(30); }
    }

    // ------------------------------------------------------------------
    // 取色
    // ------------------------------------------------------------------

    /**
     * 从封面图提取配色。
     *
     * <p>会按 min(w,h)/48 的步长采样，不是逐像素扫，所以 1000×1000 的封面也只取一万来个点。
     * 该方法必须在解码出 {@link NativeImage} 之后、交给 DynamicTexture 之前调用。
     */
    public static Scheme fromImage(NativeImage image) {
        if (image == null) {
            return fallback();
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return fallback();
        }
        int step = Math.max(1, Math.min(width, height) / 48);

        float[] hueWeighted = new float[HUE_BUCKETS];
        float[] chromaWeighted = new float[HUE_BUCKETS];
        float[] totalWeight = new float[HUE_BUCKETS];

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int abgr = image.getPixelRGBA(x, y);
                // 半透明像素（PNG 封面常见）颜色不可信，跳过
                if (FastColor.ABGR32.alpha(abgr) < 128) {
                    continue;
                }
                Lch lch = srgbToLch(
                        FastColor.ABGR32.red(abgr),
                        FastColor.ABGR32.green(abgr),
                        FastColor.ABGR32.blue(abgr));
                // 太黑、太白、太灰的像素都不参与投票，否则会污染主色
                if (lch.l() < 15.0f || lch.l() > 95.0f || lch.c() < 5.0f) {
                    continue;
                }
                int bucket = (int) (lch.h() / 360.0f * HUE_BUCKETS) % HUE_BUCKETS;
                float weight = lch.c() * (lch.l() / 100.0f);
                hueWeighted[bucket] += lch.h() * weight;
                chromaWeighted[bucket] += lch.c() * weight;
                totalWeight[bucket] += weight;
            }
        }

        int best = -1;
        float bestScore = 0.0f;
        for (int i = 0; i < HUE_BUCKETS; i++) {
            if (totalWeight[i] <= 0.0f) {
                continue;
            }
            float averageChroma = chromaWeighted[i] / totalWeight[i];
            // 以「出现得多」为主，「饱和度高」有 20% 的加成
            float score = totalWeight[i] * (0.8f + 0.2f * Math.min(1.0f, averageChroma / 40.0f));
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best < 0 || bestScore < 0.1f) {
            return fallback();
        }

        float hue = hueWeighted[best] / totalWeight[best];
        float chroma = Math.max(MIN_CHROMA, chromaWeighted[best] / totalWeight[best]);
        return fromSource(hue, chroma);
    }

    /** 从一个已知的主色（色相 + 色度）派生整套配色。 */
    public static Scheme fromSource(float hue, float chroma) {
        return new Scheme(
                new TonalPalette(hue, chroma),
                // 次级偏 30°、色度降到 0.8，三级偏 60°、色度 0.6，这样既有关联又有层次
                new TonalPalette(hue + 30.0f, chroma * 0.8f),
                new TonalPalette(hue + 60.0f, chroma * 0.6f),
                // 中性色色度必须压得很低（M3 规范就是 neutral=4 / neutralVariant=8）。
                // 调大会在深色表面上泛出一层浑浊的补色 —— 色相 340 会变成脏酱紫。
                new TonalPalette(hue, NEUTRAL_CHROMA),
                new TonalPalette(hue, NEUTRAL_VARIANT_CHROMA));
    }

    /** 没有封面时的兜底配色：浅蓝（色相 250，强调色约 #82CFFF）。 */
    public static Scheme fallback() {
        return fromSource(250.0f, 45.0f);
    }

    // ------------------------------------------------------------------
    // 插值
    // ------------------------------------------------------------------

    /** 两套配色之间插值，用于换歌时的平滑过渡。 */
    public static Scheme lerp(Scheme a, Scheme b, float t) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        float p = clamp(t, 0.0f, 1.0f);
        return new Scheme(
                lerpPalette(a.primary, b.primary, p),
                lerpPalette(a.secondary, b.secondary, p),
                lerpPalette(a.tertiary, b.tertiary, p),
                lerpPalette(a.neutral, b.neutral, p),
                lerpPalette(a.neutralVariant, b.neutralVariant, p));
    }

    private static TonalPalette lerpPalette(TonalPalette a, TonalPalette b, float t) {
        return new TonalPalette(
                lerpHue(a.hue(), b.hue(), t),
                a.chroma() + (b.chroma() - a.chroma()) * t);
    }

    /** 沿色相环走最短弧，归一化到 [-180, 180] 再插值。 */
    private static float lerpHue(float a, float b, float t) {
        float delta = ((b - a) % 360.0f + 540.0f) % 360.0f - 180.0f;
        return normalizeHue(a + delta * t);
    }

    // ------------------------------------------------------------------
    // 色彩空间转换：sRGB ↔ linear ↔ XYZ ↔ Lab ↔ LCh
    // ------------------------------------------------------------------

    private static Lch srgbToLch(int r, int g, int b) {
        float lr = toLinear(r / 255.0f);
        float lg = toLinear(g / 255.0f);
        float lb = toLinear(b / 255.0f);

        // linear sRGB → XYZ (D65)
        float x = (0.4124f * lr + 0.3576f * lg + 0.1805f * lb) / 0.95047f;
        float y = (0.2126f * lr + 0.7152f * lg + 0.0722f * lb);
        float z = (0.0193f * lr + 0.1192f * lg + 0.9505f * lb) / 1.08883f;

        float fx = labF(x);
        float fy = labF(y);
        float fz = labF(z);

        float l = 116.0f * fy - 16.0f;
        float la = 500.0f * (fx - fy);
        float lbv = 200.0f * (fy - fz);

        float chroma = (float) Math.sqrt(la * la + lbv * lbv);
        float hue = (float) Math.toDegrees(Math.atan2(lbv, la));
        return new Lch(clamp(l, 0.0f, 100.0f), chroma, normalizeHue(hue));
    }

    private static int lchToArgb(float l, float chroma, float hue) {
        float radians = (float) Math.toRadians(hue);
        float la = (float) (chroma * Math.cos(radians));
        float lb = (float) (chroma * Math.sin(radians));

        float fy = (l + 16.0f) / 116.0f;
        float fx = fy + la / 500.0f;
        float fz = fy - lb / 200.0f;

        float x = 0.95047f * labFInv(fx);
        float y = labFInv(fy);
        float z = 1.08883f * labFInv(fz);

        float lr = 3.2406f * x - 1.5372f * y - 0.4986f * z;
        float lg = -0.9689f * x + 1.8758f * y + 0.0415f * z;
        float lb2 = 0.0557f * x - 0.2040f * y + 1.0570f * z;

        int r = toByte(toSrgb(lr));
        int g = toByte(toSrgb(lg));
        int b = toByte(toSrgb(lb2));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** 线性 RGB 是否落在 sRGB 色域内（留一点点容差抵消浮点误差）。 */
    private static boolean inGamut(float l, float chroma, float hue) {
        float radians = (float) Math.toRadians(hue);
        float la = (float) (chroma * Math.cos(radians));
        float lb = (float) (chroma * Math.sin(radians));

        float fy = (l + 16.0f) / 116.0f;
        float fx = fy + la / 500.0f;
        float fz = fy - lb / 200.0f;

        float x = 0.95047f * labFInv(fx);
        float y = labFInv(fy);
        float z = 1.08883f * labFInv(fz);

        float lr = 3.2406f * x - 1.5372f * y - 0.4986f * z;
        float lg = -0.9689f * x + 1.8758f * y + 0.0415f * z;
        float lb2 = 0.0557f * x - 0.2040f * y + 1.0570f * z;

        if (Float.isNaN(lr) || Float.isNaN(lg) || Float.isNaN(lb2)) {
            return false;
        }
        float eps = 1.0E-4f;
        return lr >= -eps && lr <= 1.0f + eps
                && lg >= -eps && lg <= 1.0f + eps
                && lb2 >= -eps && lb2 <= 1.0f + eps;
    }

    private static float toLinear(float c) {
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4);
    }

    private static float toSrgb(float c) {
        return c <= 0.0031308f ? c * 12.92f : (float) (1.055 * Math.pow(c, 1.0 / 2.4) - 0.055);
    }

    private static float labF(float t) {
        return t > 0.008856f ? (float) Math.cbrt(t) : 7.787f * t + 16.0f / 116.0f;
    }

    private static float labFInv(float t) {
        float cube = t * t * t;
        return cube > 0.008856f ? cube : (t - 16.0f / 116.0f) / 7.787f;
    }

    private static int toByte(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255.0f)));
    }

    private static float normalizeHue(float hue) {
        float h = hue % 360.0f;
        return h < 0.0f ? h + 360.0f : h;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    /** 只替换 RGB，保留原来的 alpha。用于把现有色板常量平滑换成动态配色。 */
    public static int withAlphaOf(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha & 0xFF) << 24;
    }
}
