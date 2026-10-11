package client.nilore.modules.impl.misc.music.provider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 当前激活的播放渠道。
 *
 * <p>用静态门面而不是注入，是因为现有 9 处调用都直接写在
 * {@code NeteaseApi.xxx} 这种静态形式，不引入一层接口它们全都得改。
 * 所以保留 {@code NeteaseApi} 静态入口的形（迁到门面内），按当前 source 转发。
 */
public final class MusicSources {

    private static final List<MusicSource> ALL = List.of(
            new GdstudioSource(),
            new NeteaseSource());

    /** "GDStudio" / "NetEase Official"。 */
    private static volatile MusicSource current = ALL.get(0);

    private MusicSources() {
    }

    public static List<MusicSource> all() {
        return ALL;
    }

    public static MusicSource current() {
        return current;
    }

    /**
     * 切换当前 source。
     *
     * @param displayName {@link MusicSource#displayName()} 之一
     */
    public static void setCurrent(String displayName) {
        for (MusicSource source : ALL) {
            if (source.displayName().equals(displayName)) {
                current = source;
                return;
            }
        }
    }

    /** 启动时从磁盘恢复上次选的 source。失败时保持默认。 */
    public static void load(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return;
            }
            setCurrent(Files.readString(file).trim());
        } catch (Exception ignored) {
        }
    }

    /** 当前 source 名落盘。 */
    public static void save(Path file) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, current.displayName());
        } catch (Exception ignored) {
        }
    }
}