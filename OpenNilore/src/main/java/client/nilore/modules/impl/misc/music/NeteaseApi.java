package client.nilore.modules.impl.misc.music;

import client.nilore.modules.impl.misc.music.provider.MusicSource;
import client.nilore.modules.impl.misc.music.provider.MusicSources;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 静态门面：所有调用转发到当前激活的 {@link MusicSource}。
 *
 * <p>保留这一层的目的是不让 9 处既有调用方（{@code MusicPlayerScreen}、
 * {@code MusicPlayerHud}、{@code LyricsModule}）跟着改一遍。
 */
public class NeteaseApi {

    public static CompletableFuture<List<SongInfo>> search(String keywords, int limit) {
        return MusicSources.current().search(keywords, limit);
    }

    public static CompletableFuture<List<LyricLine>> getLyrics(long songId) {
        return MusicSources.current().lyrics(songId);
    }

    public static CompletableFuture<MusicSource.SongUrlResult> getSongUrl(long songId) {
        return MusicSources.current().songUrl(songId);
    }

    public static CompletableFuture<String> getAlbumPicUrl(SongInfo song) {
        // 完整 SongInfo 传给 source：gdstudio 用 albumPicUrl（picId）拼直链，
        // 网易官方用 id（songId）走 song/detail 拿 picUrl。两者需要的字段不同，
        // 只传一个字符串会顾此失彼（之前传 picId 导致网易官方 id=0 查不到封面）。
        return MusicSources.current().coverUrl(song);
    }
}
