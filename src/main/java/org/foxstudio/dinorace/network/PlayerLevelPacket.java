package org.foxstudio.dinorace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.foxstudio.dinorace.client.RaceClientData;

import java.util.function.Supplier;

public class PlayerLevelPacket {

    public final int level;
    public final int xp;
    public final int xpToNext;
    public final int skillPoints;

    public PlayerLevelPacket(int level, int xp, int xpToNext, int skillPoints) {
        this.level = level;
        this.xp = xp;
        this.xpToNext = xpToNext;
        this.skillPoints = skillPoints;
    }

    public static void encode(PlayerLevelPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.level);
        buf.writeInt(msg.xp);
        buf.writeInt(msg.xpToNext);
        buf.writeInt(msg.skillPoints);
    }

    public static PlayerLevelPacket decode(FriendlyByteBuf buf) {
        return new PlayerLevelPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(PlayerLevelPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                handleClient(msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PlayerLevelPacket msg) {
        RaceClientData.setPlayerLevel(msg.level, msg.xp, msg.xpToNext, msg.skillPoints);
    }
}