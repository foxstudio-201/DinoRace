package org.foxstudio.dinorace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.foxstudio.dinorace.client.RaceClientData;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * S2C: broadcast cấp độ + tộc của một player cho TẤT CẢ client
 * (để nametag hiện [lv: X] với màu theo tộc).
 */
public class PlayerTagPacket {

    public final UUID uuid;
    public final int level;
    public final int raceIndex;

    public PlayerTagPacket(UUID uuid, int level, int raceIndex) {
        this.uuid = uuid;
        this.level = level;
        this.raceIndex = raceIndex;
    }

    public static void encode(PlayerTagPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.uuid);
        buf.writeInt(msg.level);
        buf.writeInt(msg.raceIndex);
    }

    public static PlayerTagPacket decode(FriendlyByteBuf buf) {
        return new PlayerTagPacket(buf.readUUID(), buf.readInt(), buf.readInt());
    }

    public static void handle(PlayerTagPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                handleClient(msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PlayerTagPacket msg) {
        RaceClientData.setPlayerTag(msg.uuid, msg.level, msg.raceIndex);
    }
}