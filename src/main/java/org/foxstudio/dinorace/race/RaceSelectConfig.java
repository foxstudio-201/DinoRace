package org.foxstudio.dinorace.race;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Config chọn tộc (client): config/dinorace/race_select.json
 * Danh sách "enabled" ghi các khóa tộc được phép hiện trong màn hình chọn.
 * Bỏ đi một khóa = tộc đó sẽ KHÔNG xuất hiện. Mặc định hiện tất cả.
 */
public final class RaceSelectConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("dinorace");
    private static final Gson GSON = new Gson();

    private static final String FILE = "config/dinorace/race_select.json";
    private static final String DEFAULT_RL = "dinorace:race_select_defaults/race_select.json";

    private static boolean[] enabled = new boolean[RaceData.KEYS.length];
    private static boolean loaded;

    private RaceSelectConfig() {
    }

    public static boolean isEnabled(int index) {
        ensureLoaded();
        return index >= 0 && index < enabled.length && enabled[index];
    }

    /** Số tộc đang được bật (dùng cho layout màn hình chọn). */
    public static int enabledCount() {
        ensureLoaded();
        int n = 0;
        for (boolean b : enabled) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Arrays.fill(enabled, true);
        try {
            Minecraft mc = Minecraft.getInstance();
            Path file = mc.gameDirectory.toPath().resolve(FILE);
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                try {
                    InputStream in = mc.getResourceManager().getResource(new ResourceLocation(DEFAULT_RL)).get().open();
                    Files.writeString(file, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                } catch (Exception e) {
                    LOGGER.warn("[dinorace] Không copy được race_select.json mặc định: {}", e.getMessage());
                }
            }
            if (Files.exists(file)) {
                JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
                Set<String> ok = new HashSet<>(Arrays.asList(RaceData.KEYS));
                if (root != null && root.has("enabled")) {
                    Arrays.fill(enabled, false);
                    for (var e : root.getAsJsonArray("enabled")) {
                        String key = e.getAsString();
                        int idx = java.util.Arrays.asList(RaceData.KEYS).indexOf(key);
                        if (idx >= 0) {
                            enabled[idx] = true;
                        }
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("[dinorace] Lỗi đọc race_select.json: {}", ex.getMessage());
        }
    }
}