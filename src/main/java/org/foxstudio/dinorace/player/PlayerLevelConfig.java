package org.foxstudio.dinorace.player;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class PlayerLevelConfig {

    private static final Gson GSON = new Gson();
    private static double xpCurveMultiplier = 3.0;
    private static int skillPointsPerLevel = 1;

    private PlayerLevelConfig() {
    }

    public static void load(MinecraftServer server) {
        try {
            Path dir = server.getServerDirectory().toPath().resolve("config/dinorace");
            Path file = dir.resolve("player-levels.json");
            if (!Files.exists(file)) {
                Files.createDirectories(dir);
                Optional<Resource> opt = server.getResourceManager().getResource(
                        new ResourceLocation("dinorace", "race_defaults/player-levels.json"));
                if (opt.isPresent()) {
                    try (InputStream in = opt.get().open()) {
                        Files.writeString(file, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
            JsonObject root = GSON.fromJson(new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8), JsonObject.class);
            if (root != null) {
                if (root.has("xpCurveMultiplier")) {
                    xpCurveMultiplier = root.get("xpCurveMultiplier").getAsDouble();
                }
                if (root.has("skillPointsPerLevel")) {
                    skillPointsPerLevel = root.get("skillPointsPerLevel").getAsInt();
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static double xpCurveMultiplier() {
        return xpCurveMultiplier;
    }

    public static int skillPointsPerLevel() {
        return skillPointsPerLevel;
    }
}