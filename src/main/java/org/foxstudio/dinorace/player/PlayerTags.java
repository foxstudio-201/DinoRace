package org.foxstudio.dinorace.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.foxstudio.dinorace.network.PlayerTagPacket;
import org.foxstudio.dinorace.network.RaceNetwork;

/**
 * Broadcast cấp độ + tộc của player cho tất cả client (nametag [lv: X]).
 */
public final class PlayerTags {

    private PlayerTags() {
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