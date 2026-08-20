package org.foxstudio.dinorace.race;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cấu hình GUI thông tin tộc (chỉ số / kỹ năng / cây tiến hóa), 1 file riêng cho
 * từng tộc để dễ dàng chỉnh sửa:
 * - File phục vụ:  config/dinorace/race_skills/&lt;tộc&gt;.json
 * - Bản mặc định:   data/dinorace/race_skills_defaults/&lt;tộc&gt;.json (tự copy ra
 *   config lần đầu nếu chưa có, giống cách races.json của RaceConfig).
 */
public final class RaceDetailConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("dinorace");

    public static final class Stat {
        public String name = "";
        public double value = 0;
    }

    public static final class Skill {
        public String name = "";
        public int level = 1;
        public String description = "";
        public boolean active = false;
        public int requiredLevel = 0;
    }

    public static final class EvoNode {
        public String id = "";
        public String name = "";
        public String icon = "";
        public String desc = "";
        public double x = 0;
        public double y = 0;
    }

    public static final class Evo {
        public double progress = 0;
        public List<Double> milestones = new ArrayList<>();
        public List<EvoNode> nodes = new ArrayList<>();
        public List<List<String>> links = new ArrayList<>();
    }

    public static final class Data {
        public String raceName = "";
        public List<Stat> stats = new ArrayList<>();
        public List<Skill> skills = new ArrayList<>();
        public Evo evolution = new Evo();
    }

    private static final Gson GSON = new Gson();
    private static final Map<String, Data> CACHE = new HashMap<>();

    private RaceDetailConfig() {
    }

    public static Data get(String raceKey) {
        if (raceKey == null) {
            raceKey = "";
        }
        Data cached = CACHE.get(raceKey);
        if (cached != null) {
            return cached;
        }
        Data data = new Data();
        boolean hasActive = false;
        String fileUsed = "none";
        try {
            Minecraft mc = Minecraft.getInstance();
            Path dir = mc.gameDirectory.toPath().resolve("config/dinorace/race_skills");
            Path file = dir.resolve(raceKey.toLowerCase(Locale.ROOT) + ".json");
            if (!Files.exists(file)) {
                Files.createDirectories(dir);
                Optional<Resource> opt = mc.getResourceManager().getResource(
                        new ResourceLocation("dinorace", "race_skills_defaults/" + raceKey.toLowerCase(Locale.ROOT) + ".json"));
                if (opt.isPresent()) {
                    try (InputStream in = opt.get().open()) {
                        Files.writeString(file, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
            if (Files.exists(file)) {
                fileUsed = file.toString();
                data = GSON.fromJson(Files.readString(file), Data.class);
            }
        } catch (IOException ignored) {
        }
        if (data == null) {
            data = new Data();
        }
        if (data.evolution == null) {
            data.evolution = new Evo();
        }
        if (data.evolution.milestones == null) {
            data.evolution.milestones = new ArrayList<>();
        }
        if (data.evolution.nodes == null) {
            data.evolution.nodes = new ArrayList<>();
        }
        if (data.evolution.links == null) {
            data.evolution.links = new ArrayList<>();
        }
        if (data.stats == null) {
            data.stats = new ArrayList<>();
        }
        if (data.skills == null) {
            data.skills = new ArrayList<>();
        }
        for (Skill s : data.skills) {
            if (s.active) {
                hasActive = true;
                break;
            }
        }
        if (!hasActive) {
            List<Skill> serverActive = new ArrayList<>();
            for (RaceInfo ri : org.foxstudio.dinorace.client.RaceClientData.racesList()) {
                if (ri.key != null && ri.key.equalsIgnoreCase(raceKey)) {
                    for (RacePower p : ri.powers) {
                        if (p.active) {
                            Skill sk = new Skill();
                            sk.name = p.name;
                            sk.description = p.description;
                            sk.active = true;
                            sk.requiredLevel = p.requiredLevel;
                            serverActive.add(sk);
                        }
                    }
                    break;
                }
            }
            if (!serverActive.isEmpty()) {
                data.skills = serverActive;
            }
            LOGGER.info("[dinorace] cfg: key={} file={} fileSkills={} hasActive={} raceList={} matchAct={} -> skills={}",
                    raceKey, fileUsed, data.skills.size(), hasActive,
                    org.foxstudio.dinorace.client.RaceClientData.racesList().size(), serverActive.size(),
                    data.skills.stream().map(s -> s.name + (s.active ? "(A)" : "")).toList());
        }
        CACHE.put(raceKey, data);
        return data;
    }

    public static void clearCache() {
        CACHE.clear();
    }
}