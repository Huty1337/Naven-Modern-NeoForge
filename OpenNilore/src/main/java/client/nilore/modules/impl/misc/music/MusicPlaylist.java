package client.nilore.modules.impl.misc.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import client.nilore.manager.ConfigManager;

public class MusicPlaylist {
    private static final String FILE_NAME = "music-playlist.json";
    private final List<SongInfo> songs = new ArrayList<>();

    public MusicPlaylist() {
        load();
    }

    public synchronized List<SongInfo> getSongs() {
        return List.copyOf(songs);
    }

    public synchronized void add(SongInfo song) {
        songs.add(song);
        save();
    }

    public synchronized void remove(int index) {
        if (index < 0 || index >= songs.size()) return;
        songs.remove(index);
        save();
    }

    private void load() {
        if (!ConfigManager.CONFIG_DIR.exists()) return;
        java.io.File file = new java.io.File(ConfigManager.CONFIG_DIR, FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonArray()) return;
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject song = element.getAsJsonObject();
                songs.add(new SongInfo(
                        song.get("id").getAsLong(),
                        song.get("name").getAsString(),
                        song.get("artist").getAsString(),
                        song.get("albumName").getAsString(),
                        song.get("albumPicUrl").getAsString(),
                        song.get("duration").getAsLong()));
            }
        } catch (Exception e) {
            System.err.println("[MusicPlayer] Failed to load playlist: " + e.getMessage());
        }
    }

    private void save() {
        if (!ConfigManager.CONFIG_DIR.exists() && !ConfigManager.CONFIG_DIR.mkdirs()) {
            System.err.println("[MusicPlayer] Failed to create config directory");
            return;
        }
        java.io.File file = new java.io.File(ConfigManager.CONFIG_DIR, FILE_NAME);
        JsonArray root = new JsonArray();
        for (SongInfo song : songs) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", song.id);
            entry.addProperty("name", song.name);
            entry.addProperty("artist", song.artist);
            entry.addProperty("albumName", song.albumName);
            entry.addProperty("albumPicUrl", song.albumPicUrl);
            entry.addProperty("duration", song.duration);
            root.add(entry);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(root.toString());
        } catch (IOException e) {
            System.err.println("[MusicPlayer] Failed to save playlist: " + e.getMessage());
        }
    }
}