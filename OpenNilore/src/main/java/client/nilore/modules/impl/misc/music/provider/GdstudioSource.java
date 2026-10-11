package client.nilore.modules.impl.misc.music.provider;

import client.nilore.modules.impl.misc.music.GdstudioApi;
import client.nilore.modules.impl.misc.music.LyricLine;
import client.nilore.modules.impl.misc.music.SongInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 走 gdstudio 第三方聚合 API（{@code music-api.gdstudio.xyz}）。
 *
 * <p>一直能用，但拿不到逐字歌词（只有行级 LRC）。
 *
 * <p>注意：这里直接调 {@link GdstudioApi}，不能再经过 {@code NeteaseApi} 门面 ——
 * 门面转发回 {@code MusicSources}，而当前 source 正是本类，会形成调用环导致 StackOverflow。
 */
final class GdstudioSource implements MusicSource {

    @Override
    public String displayName() {
        return "GDStudio";
    }

    @Override
    public CompletableFuture<List<SongInfo>> search(String keyword, int limit) {
        return GdstudioApi.search(keyword, limit);
    }

    @Override
    public CompletableFuture<List<LyricLine>> lyrics(long songId) {
        return GdstudioApi.getLyrics(songId);
    }

    @Override
    public CompletableFuture<SongUrlResult> songUrl(long songId) {
        return GdstudioApi.getSongUrl(songId);
    }

    @Override
    public CompletableFuture<String> coverUrl(SongInfo song) {
        return GdstudioApi.getAlbumPicUrl(song.albumPicUrl);
    }
}