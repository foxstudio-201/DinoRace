package org.foxstudio.dinorace;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.foxstudio.dinorace.player.PlayerLevels;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side gate cho skill chủ động: quyền power bị khóa (chưa đủ cấp) sẽ bị
 * thu hồi, đủ cấp thì cấp lại. Chạy độc lập với datapack override nên hoạt động
 * chắc chắn trên server hosting.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = org.foxstudio.dinorace.DinoRace.MODID)
public final class SkillGateEnforcer {

    private SkillGateEnforcer() {
    }

    private static final ResourceLocation SOURCE = new ResourceLocation("dinorace", "skill_gate");

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("dinorace");

    public static final Map<String, Integer> REQUIRED = new HashMap<>();

    static {
        REQUIRED.put("medievalorigins:dwarf/potent_brew", 5);
        REQUIRED.put("medievalorigins:high_elf/ebonbreath", 10);
        REQUIRED.put("medievalorigins:high_elf/blazenbreath", 10);
        REQUIRED.put("medievalorigins:high_elf/blazenbreath_weak", 10);
        REQUIRED.put("medievalorigins:high_elf/void_warp", 15);
        REQUIRED.put("medievalorigins:pixie/flight", 3);
        REQUIRED.put("medievalorigins:pixie/mischief_maketh_man", 5);
        REQUIRED.put("medievalorigins:alfiq/meow", 3);
        REQUIRED.put("medievalorigins:revenant/hellraiser", 20);
        REQUIRED.put("medievalorigins:revenant/skeleton_summons", 10);
        REQUIRED.put("medievalorigins:revenant/zombie_summons", 10);
        REQUIRED.put("medievalorigins:revenant/command_summons", 15);
        REQUIRED.put("medievalorigins:incubus/unholy_deal", 10);
        REQUIRED.put("medievalorigins:incubus/demon_fire", 10);
        REQUIRED.put("medievalorigins:valkyrie/intervention", 10);
        REQUIRED.put("medievalorigins:yeti/frigid_pulse", 10);
        REQUIRED.put("medievalorigins:fae/levitation", 5);
        REQUIRED.put("medievalorigins:fae/natures_blessing", 10);
        REQUIRED.put("medievalorigins:fae/nourishment", 5);
        REQUIRED.put("medievalorigins:dragonkin/dragon_fury", 10);
        REQUIRED.put("medievalorigins:ogre/bloodlust", 10);
    }

    public static void schedule(ServerPlayer player) {
        player.server.execute(() -> sync(player));
    }

    private static int tickCount = 0;

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        if (++tickCount % 100 != 0) {
            return;
        }
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (var p : server.getPlayerList().getPlayers()) {
            sync(p);
        }
    }

    public static void sync(ServerPlayer player) {
        if (player == null || player.server == null || !player.connection.isAcceptingMessages()) {
            return;
        }
        try {
            int level = player.getPersistentData().getInt(PlayerLevels.KEY_LEVEL);
            OriginComponent oc = ModComponents.ORIGIN.get(player);
            PowerHolderComponent comp = PowerHolderComponent.KEY.get(player);
            comp.removeAllPowersFromSource(SOURCE);
            Map<String, PowerType<?>> originTypes = new HashMap<>();
            for (Origin origin : oc.getOrigins().values()) {
                if (origin == null) {
                    continue;
                }
                for (PowerType<?> pt : origin.getPowerTypes()) {
                    originTypes.putIfAbsent(pt.getIdentifier().toString(), pt);
                }
            }
            for (Map.Entry<String, Integer> e : REQUIRED.entrySet()) {
                PowerType<?> pt = originTypes.get(e.getKey());
                if (pt == null) {
                    continue;
                }
                if (level >= e.getValue()) {
                    if (!comp.hasPower(pt)) {
                        comp.addPower(pt, SOURCE);
                        LOGGER.info("[dinorace] gate: GRANT {} cho {} (lv {} >= {})",
                                e.getKey(), player.getGameProfile().getName(), level, e.getValue());
                    }
                } else {
                    for (ResourceLocation src : comp.getSources(pt)) {
                        comp.removePower(pt, src);
                        LOGGER.info("[dinorace] gate: REVOKE {} từ {} cho {} (lv {} < {})",
                                e.getKey(), src, player.getGameProfile().getName(), level, e.getValue());
                    }
                }
            }
            comp.sync();
        } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("dinorace").error("[dinorace] SkillGateEnforcer sync lỗi: {}", t.toString());
        }
    }
}
