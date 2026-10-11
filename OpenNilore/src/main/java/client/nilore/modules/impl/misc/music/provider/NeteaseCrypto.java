package client.nilore.modules.impl.misc.music.provider;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 网易云 weapi 请求的加密。
 *
 * <p>这是网易 web 端一直在用的公开协议：明文先用固定密钥做一次 AES-CBC，再用一个随机密钥做第二次
 * AES-CBC，随机密钥本身用 RSA 单独加密。服务端拿 {@code encSecKey} 解出随机密钥，再解两层 AES。
 *
 * <p>都是协议常量，不是谁的私有实现。
 */
final class NeteaseCrypto {

    /** 第一层 AES 用的固定密钥，web 端硬编码的。 */
    private static final String PRESET_KEY = "0CoJUm6Qyw8W8jud";
    /** 两层 AES 共用的 IV。 */
    private static final byte[] IV = "0102030405060708".getBytes(StandardCharsets.UTF_8);
    /** RSA 公钥指数，固定 65537。 */
    private static final BigInteger RSA_EXPONENT = new BigInteger("010001", 16);
    /** RSA 模数，web 端硬编码的固定值。 */
    private static final BigInteger RSA_MODULUS = new BigInteger(
            "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e41"
                    + "7629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee25593"
                    + "2575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7", 16);
    private static final char[] BASE62 =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private NeteaseCrypto() {
    }

    /**
     * 加密出一组 weapi 表单参数。
     *
     * @param json 请求体的 JSON 文本
     * @return {@code params=...&encSecKey=...}，已经 URL 编码
     */
    static String encrypt(String json) {
        String secret = randomBase62(16);
        String params = aesCbcBase64(aesCbcBase64(json, PRESET_KEY), secret);
        String encSecKey = rsaHex(secret);
        return "params=" + urlEncode(params) + "&encSecKey=" + urlEncode(encSecKey);
    }

    private static String aesCbcBase64(String plain, String key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
                    new IvParameterSpec(IV));
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("weapi AES 加密失败", e);
        }
    }

    /**
     * 把随机密钥倒序后当成大整数做 RSA，输出固定 256 字符的十六进制。
     *
     * <p>倒序和 256 位补零都是协议规定，不能省。
     */
    private static String rsaHex(String secret) {
        String reversed = new StringBuilder(secret).reverse().toString();
        BigInteger value = new BigInteger(1, reversed.getBytes(StandardCharsets.UTF_8));
        String hex = value.modPow(RSA_EXPONENT, RSA_MODULUS).toString(16);
        if (hex.length() >= 256) {
            return hex;
        }
        StringBuilder sb = new StringBuilder(256);
        for (int i = hex.length(); i < 256; i++) {
            sb.append('0');
        }
        return sb.append(hex).toString();
    }

    private static String randomBase62(int length) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = BASE62[RANDOM.nextInt(BASE62.length)];
        }
        return new String(out);
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** 小写十六进制，eapi 用得上。 */
    static String md5Hex(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit(b >> 4 & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 失败", e);
        }
    }
}
