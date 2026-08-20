package org.foxstudio.dinorace;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.foxstudio.dinorace.network.OpenRaceFlowPacket;
import org.foxstudio.dinorace.network.RaceInfoPacket;
import org.foxstudio.dinorace.network.RaceNetwork;
import org.foxstudio.dinorace.race.RaceConfig;

/**
 * API công khai để các mod khác (vd dinocore) kích hoạt luồng chọn tộc:
 * gửi nội dung 5 chủng tộc (RaceInfoPacket) rồi mở màn hình chọn tộc
 * (OpenRaceFlowPacket).
 */
public final class RaceFlow {

    private RaceFlow() {
    }

    /** Gửi thông tin 5 chủng tộc + mở màn hình chọn tộc cho player. */
    public static void start(ServerPlayer player) {
        RaceNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new RaceInfoPacket(RaceConfig.races()));
        RaceNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenRaceFlowPacket());
    }
}