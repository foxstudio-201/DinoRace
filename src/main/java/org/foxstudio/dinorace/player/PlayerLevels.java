package org.foxstudio.dinorace.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.foxstudio.dinorace.DinoRace;
import org.foxstudio.dinorace.network.PlayerLevelPacket;
import org.foxstudio.dinorace.network.RaceNetwork;

@Mod.EventBusSubscriber(modid = DinoRace.MODID)
public final class PlayerLevels {

    public static final String KEY_LEVEL = "dinorace_level";
    public static final String KEY_XP = "dinorace_xp";
    public static final String KEY_POINTS = "dinorace_skill_points";

    private PlayerLevels() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        PlayerLevelConfig.load(event.getServer());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim instanceof net.minecraft.world.entity.player.Player) {
            return;
        }
        if (!(victim instanceof net.minecraft.world.entity.Mob mob)
                || mob.getClassification(false) != net.minecraft.world.entity.MobCategory.MONSTER) {
            return;
        }
        int xp = victim.getExperienceReward();
        if (xp <= 0) {
            xp = 5;
        }
        grantXp(killer, xp);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            send(player);
            PlayerTags.broadcast(player);
            PlayerTags.syncAllTo(player);
            org.foxstudio.dinorace.SkillGateEnforcer.schedule(player);
        }
    }

    public static void grantXp(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        var data = player.getPersistentData();
        int level = data.getInt(KEY_LEVEL);
        int xp = data.getInt(KEY_XP) + amount;
        int points = data.getInt(KEY_POINTS);
        int perLevel = PlayerLevelConfig.skillPointsPerLevel();
        int gained = 0;
        while (xp >= xpToNext(level) && gained < 1) {
            xp -= xpToNext(level);
            level++;
            points += perLevel;
            gained++;
        }
        data.putInt(KEY_LEVEL, level);
        data.putInt(KEY_XP, xp);
        data.putInt(KEY_POINTS, points);
        send(player);
        PlayerTags.broadcast(player);
        if (gained > 0) {
            org.foxstudio.dinorace.SkillGateEnforcer.schedule(player);
        }
    }

    public static int xpToNext(int level) {
        int base;
        if (level >= 30) {
            base = 9 * level - 158;
        } else if (level >= 15) {
            base = 5 * level - 38;
        } else {
            base = 2 * level + 7;
        }
        return (int) Math.round(base * PlayerLevelConfig.xpCurveMultiplier());
    }

    public static void send(ServerPlayer player) {
        var data = player.getPersistentData();
        int level = data.getInt(KEY_LEVEL);
        RaceNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PlayerLevelPacket(level, data.getInt(KEY_XP), xpToNext(level), data.getInt(KEY_POINTS)));
    }
}