package org.foxstudio.dinorace.race;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Đọc config/dinorace/races.json (server) — nội dung đầy đủ của 5 chủng tộc,
 * có thể chỉnh sửa sau. Nếu chưa có file, tự copy bản mặc định từ mod.
 */
public final class RaceConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("dinorace");
    private static final Gson GSON = new Gson();
    private static List<RaceInfo> RACES = List.of();

    private RaceConfig() {
    }

    public static void load(MinecraftServer server) {
        try {
            Path dir = server.getServerDirectory().toPath().resolve("config/dinorace");
            Path file = dir.resolve("races.json");
            if (!Files.exists(file)) {
                Files.createDirectories(dir);
                Optional<Resource> opt = server.getResourceManager().getResource(
                        new ResourceLocation("dinorace", "race_defaults/races.json"));
                if (opt.isPresent()) {
                    try (InputStream in = opt.get().open()) {
                        Files.writeString(file, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
            RACES = parse(file);
            LOGGER.info("[dinorace] Đã nạp {} chủng tộc từ {}", RACES.size(), file);
        } catch (Exception e) {
            LOGGER.error("[dinorace] Lỗi đọc races.json: {}", e.getMessage());
        }
    }

    public static List<RaceInfo> races() {
        return RACES;
    }

    private static List<RaceInfo> parse(Path file) {
        List<RaceInfo> out = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root != null && root.has("races")) {
                for (var e : root.getAsJsonArray("races")) {
                    RaceInfo ri = GSON.fromJson(e, RaceInfo.class);
                    if (ri != null && ri.name != null) {
                        out.add(ri);
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.error("[dinorace] Lỗi parse races.json: {}", ex.getMessage());
        }
        return out;
    }
}