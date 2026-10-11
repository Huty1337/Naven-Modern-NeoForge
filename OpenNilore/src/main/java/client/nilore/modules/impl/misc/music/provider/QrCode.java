package client.nilore.modules.impl.misc.music.provider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 用 zxing 把一段文本渲成二维码纹理。
 *
 * <p>白色背景、黑色像素、白边 4 像素 —— 手机端识别比纯黑白块更稳。
 *
 * <p>返回的 {@link RenderableQr} 持有随机 {@link ResourceLocation}，调用方负责
 * {@code TextureManager.register} 和 {@code release} —— DynamicTexture 的 GL 资源
 * 由 TextureManager 管理，不显式释放会泄漏。
 */
public final class QrCode {

    private QrCode() {
    }

    public static RenderableQr render(String content, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                    BarcodeFormat.QR_CODE, size, size);
            int w = matrix.getWidth();
            int h = matrix.getHeight();
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    // 前景黑、背景白。RGBA：黑 0xFF000000，白 0xFFFFFFFF
                    image.setPixelRGBA(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            // 随机 location 避免和游戏内其它纹理撞名
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    "nilore", "qr_" + UUID.randomUUID().toString().replace("-", ""));
            DynamicTexture texture = new DynamicTexture(image);
            texture.upload();
            return new RenderableQr(loc, texture, size, size);
        } catch (WriterException e) {
            // 内容太长才会走到这里；size=240 容纳 200+ 字符绰绰有余
            throw new IllegalStateException("二维码生成失败", e);
        }
    }

    public record RenderableQr(ResourceLocation location, DynamicTexture texture, int width, int height) {
    }
}