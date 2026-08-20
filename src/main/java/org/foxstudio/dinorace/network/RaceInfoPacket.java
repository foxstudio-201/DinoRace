package org.foxstudio.dinorace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.foxstudio.dinorace.client.RaceClientData;
import org.foxstudio.dinorace.race.RaceInfo;
import org.foxstudio.dinorace.race.RacePower;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: gửi toàn bộ nội dung 5 chủng tộc (tên, mô tả, quyền năng) từ config
 * races.json cho client hiển thị.
 */
public class RaceInfoPacket {

    public final List<RaceInfo> races;

    public RaceInfoPacket(List<RaceInfo> races) {
        this.races = races;
    }

    public static void encode(RaceInfoPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.races.size());
        for (RaceInfo ri : msg.races) {
            buf.writeUtf(ri.key == null ? "" : ri.key, 64);
            buf.writeUtf(ri.name);
            buf.writeUtf(ri.color == null ? "" : ri.color, 16);
            buf.writeUtf(ri.description == null ? "" : ri.description);
            List<RacePower> powers = ri.powers == null ? List.of() : ri.powers;
            buf.writeInt(powers.size());
            for (RacePower p : powers) {
                buf.writeUtf(p.name == null ? "" : p.name, 128);
                buf.writeUtf(p.description == null ? "" : p.description, 2048);
                buf.writeBoolean(p.active);
                buf.writeInt(p.requiredLevel);
            }
        }
    }

    public static RaceInfoPacket decode(FriendlyByteBuf buf) {
        int n = buf.readInt();
        List<RaceInfo> races = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            RaceInfo ri = new RaceInfo();
            ri.key = buf.readUtf(64);
            ri.name = buf.readUtf(128);
            ri.color = buf.readUtf(16);
            ri.description = buf.readUtf(4096);
            int m = buf.readInt();
            ri.powers = new ArrayList<>();
            for (int j = 0; j < m; j++) {
                RacePower rp = new RacePower();
                rp.name = buf.readUtf(128);
                rp.description = buf.readUtf(2048);
                rp.active = buf.readBoolean();
                rp.requiredLevel = buf.readInt();
                ri.powers.add(rp);
            }
            races.add(ri);
        }
        return new RaceInfoPacket(races);
    }

    public static void handle(RaceInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                handleClient(msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(RaceInfoPacket msg) {
        RaceClientData.setRaces(msg.races);
    }
}