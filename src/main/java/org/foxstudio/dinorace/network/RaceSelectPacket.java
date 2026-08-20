package org.foxstudio.dinorace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * C2S: player chọn 1 trong 5 chủng tộc → server chạy /origin set <origin_id>.
 */
public class RaceSelectPacket {

    private static final Logger LOGGER = LoggerFactory.getLogger("dinorace");

    public final String originId;

    public RaceSelectPacket(String originId) {
        this.originId = originId;
    }

    public static void encode(RaceSelectPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.originId, 64);
    }

    public static RaceSelectPacket decode(FriendlyByteBuf buf) {
        return new RaceSelectPacket(buf.readUtf(64));
    }

    public static void handle(RaceSelectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            player.getPersistentData().putBoolean("dinocore_race_chosen", true);
            int idx = -1;
            String[] o = org.foxstudio.dinorace.race.RaceData.ORIGINS;
            for (int i = 0; i < o.length; i++) {
                if (o[i].equals(msg.originId)) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                player.getPersistentData().putInt("dinocore_race_index", idx);
            }
            org.foxstudio.dinorace.player.PlayerTags.broadcast(player);
            try {
                String cmd = "origin set " + player.getGameProfile().getName()
                        + " origins:origin " + msg.originId;
                player.server.getCommands().getDispatcher()
                        .execute(cmd, player.createCommandSourceStack().withPermission(4));
                LOGGER.info("[dinorace] {} chọn tộc -> {}", player.getGameProfile().getName(), msg.originId);
            } catch (Throwable t) {
                LOGGER.error("[dinorace] Lỗi set origin {}: {}", msg.originId, t.getMessage());
            }

            // Các mod khác (vd dinocore) lắng nghe để xử lý tiếp (MCA destiny...)
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                    new org.foxstudio.dinorace.race.RaceChosenEvent(player, msg.originId, idx));
            org.foxstudio.dinorace.SkillGateEnforcer.schedule(player);
        });
        ctx.get().setPacketHandled(true);
    }
}