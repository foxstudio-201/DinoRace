package org.foxstudio.dinorace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.foxstudio.dinorace.race.RaceData;
import org.foxstudio.dinorace.player.PlayerLevels;

import java.util.function.Supplier;

public class RaceRequestPacket {

    public RaceRequestPacket() {
    }

    public static void encode(RaceRequestPacket msg, FriendlyByteBuf buf) {
    }

    public static RaceRequestPacket decode(FriendlyByteBuf buf) {
        return new RaceRequestPacket();
    }

    public static void handle(RaceRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            var data = player.getPersistentData();
            int idx = -1;
            if (data.contains("dinocore_race_index")) {
                idx = data.getInt("dinocore_race_index");
            }
            if (idx < 0 || idx >= RaceData.NAMES.length) {
                idx = RaceData.indexFromOrigin(player);
                if (idx >= 0) {
                    data.putInt("dinocore_race_index", idx);
                }
            }
            if (idx >= 0) {
                RaceNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new RaceChosenPacket(idx));
            }
            PlayerLevels.send(player);
        });
        ctx.get().setPacketHandled(true);
    }
}