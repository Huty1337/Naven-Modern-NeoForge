package client.nilore.modules.impl.misc.music;

import java.util.List;

/**
 * 一行歌词。
 *
 * @param timeMs 行开始时间
 * @param text   整行文本
 * @param words  逐字时间；只有网易官方接口的 yrc 歌词才有，其它音源一律为 {@code null}
 */
public record LyricLine(long timeMs, String text, List<Word> words) {

    /**
     * 一个词/字的时间片。
     *
     * @param text       这一片的文本（中文歌通常是单字，英文歌可能是一个单词）
     * @param startMs    相对整首歌的开始时间
     * @param durationMs 持续时间
     */
    public record Word(String text, long startMs, long durationMs) {
    }

    /** 没有逐字时间时的构造，行级歌词走这个。 */
    public LyricLine(long timeMs, String text) {
        this(timeMs, text, null);
    }

    public boolean hasWordTiming() {
        return words != null && !words.isEmpty();
    }
}
