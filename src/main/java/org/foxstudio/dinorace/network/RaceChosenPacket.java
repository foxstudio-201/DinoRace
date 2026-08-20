package org.foxstudio.dinorace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.foxstudio.dinorace.client.RaceDetailScreen;

import java.util.function.Supplier;

/**
 * S2C: server báo cho client biết người chơi đang thuộc chủng tộc nào
 * (đã lưu trong player data) — để phím tắt (R) vẫn mở được GUI thông tin
 * tộc sau khi thoát world / vào lại.
 */
public class RaceChosenPacket {

    public final int raceIndex;

    public RaceChosenPacket(int raceIndex) {
        this.raceIndex = raceIndex;
    }

    public static void encode(RaceChosenPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.raceIndex);
    }

    public static RaceChosenPacket decode(FriendlyByteBuf buf) {
        return new RaceChosenPacket(buf.readInt());
    }

    public static void handle(RaceChosenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                handleClient(msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(RaceChosenPacket msg) {
        RaceDetailScreen.markChosen(msg.raceIndex);
    }
}