package org.foxstudio.dinorace.client;

import org.foxstudio.dinorace.race.RaceInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lưu tạm nội dung 5 chủng tộc do server gửi (RaceInfoPacket) để GUI hiển thị.
 */
public final class RaceClientData {

    private static final List<RaceInfo> RACES = new ArrayList<>();
    private static final Map<UUID, int[]> TAGS = new HashMap<>();
    private static int cachedLevel = 0;
    private static int cachedXp = 0;
    private static int cachedXpToNext = 1;
    private static int cachedSkillPoints = 0;
    private static boolean synced = false;

    private RaceClientData() {
    }

    public static void setRaces(List<RaceInfo> races) {
        RACES.clear();
        RACES.addAll(races);
        org.foxstudio.dinorace.race.RaceDetailConfig.clearCache();
    }

    public static List<RaceInfo> racesList() {
        if (RACES.isEmpty()) {
            return defaultRacesFromJar();
        }
        return RACES;
    }

    /** Fallback: đọc races.json mặc định trong jar khi chưa nhận data từ server. */
    private static List<RaceInfo> defaultRacesFromJar() {
        try {
            net.minecraft.resources.ResourceLocation loc =
                    new net.minecraft.resources.ResourceLocation("dinorace", "race_defaults/races.json");
            java.util.Optional<net.minecraft.server.packs.resources.Resource> opt =
                    net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(loc);
            if (opt.isPresent()) {
                try (java.io.InputStream in = opt.get().open()) {
                    String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    var wrap = new com.google.gson.Gson().fromJson(json, RaceListWrapper.class);
                    if (wrap != null && wrap.races != null) {
                        return wrap.races;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return RACES;
    }

    private static final class RaceListWrapper {
        List<RaceInfo> races;
    }

    public static RaceInfo get(int index) {
        if (index >= 0 && index < RACES.size()) {
            return RACES.get(index);
        }
        return null;
    }

    public static void setPlayerTag(UUID uuid, int level, int raceIndex) {
        TAGS.put(uuid, new int[]{level, raceIndex});
    }

    public static int tagLevel(UUID uuid) {
        int[] t = TAGS.get(uuid);
        return t == null ? -1 : t[0];
    }

    public static int tagRace(UUID uuid) {
        int[] t = TAGS.get(uuid);
        return t == null ? -1 : t[1];
    }

    public static void setPlayerLevel(int level, int xp, int xpToNext, int skillPoints) {
        cachedLevel = level;
        cachedXp = xp;
        cachedXpToNext = xpToNext;
        cachedSkillPoints = skillPoints;
        synced = true;
    }

    public static boolean hasPlayerLevel() {
        return synced;
    }

    public static int level() {
        return cachedLevel;
    }

    public static int xp() {
        return cachedXp;
    }

    public static int xpToNext() {
        return cachedXpToNext;
    }

    public static int skillPoints() {
        return cachedSkillPoints;
    }
}