package org.foxstudio.dinorace.player;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.foxstudio.dinorace.DinoRace;
import org.foxstudio.dinorace.network.PlayerTagPacket;
import org.foxstudio.dinorace.network.RaceNetwork;

/**
 * Broadcast cấp độ + tộc của player cho tất cả client (nametag [lv: X]),
 * đồng thời chèn [lv: X] vào tên hiển thị trong chat và tab list.
 */
@Mod.EventBusSubscriber(modid = DinoRace.MODID)
public final class PlayerTags {

    private PlayerTags() {
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            int level = sp.getPersistentData().getInt(PlayerLevels.KEY_LEVEL);
            event.setDisplayname(withLevel(event.getDisplayname(), level));
        }
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            int level = sp.getPersistentData().getInt(PlayerLevels.KEY_LEVEL);
            Component base = event.getDisplayName();
            if (base == null) {
                base = sp.getDisplayName();
            }
            event.setDisplayName(withLevel(base, level));
        }
    }

    private static Component withLevel(Component name, int level) {
        Component base = name == null ? Component.literal("") : name;
        String txt = base.getString();
        if (txt.startsWith("[lv:")) {
            return base;
        }
        return Component.literal("[lv: " + level + "] ").withStyle(s -> s.withColor(TextColor.fromRgb(0xFFFFCC44)))
                .copy().append(base);
    }

    public static void broadcast(ServerPlayer player) {
        try {
            RaceNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), build(player));
        } catch (Throwable t) {
            // bỏ qua — chưa gửi được
        }
    }

    public static void syncAllTo(ServerPlayer target) {
        try {
            for (ServerPlayer p : target.server.getPlayerList().getPlayers()) {
                RaceNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), build(p));
            }
        } catch (Throwable t) {
            // bỏ qua
        }
    }

    private static PlayerTagPacket build(ServerPlayer player) {
        var data = player.getPersistentData();
        int level = data.getInt(PlayerLevels.KEY_LEVEL);
        int idx = -1;
        if (data.contains("dinocore_race_index")) {
            idx = data.getInt("dinocore_race_index");
        }
        return new PlayerTagPacket(player.getUUID(), level, idx);
    }
}