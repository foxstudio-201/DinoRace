package org.foxstudio.dinorace.race;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.foxstudio.dinorace.DinoRace;
import org.foxstudio.dinorace.network.RaceNetwork;
import org.foxstudio.dinorace.network.RaceChosenPacket;

@Mod.EventBusSubscriber(modid = DinoRace.MODID)
public final class RaceChosenSender {

    private RaceChosenSender() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
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
        if (idx < 0 || idx >= RaceData.NAMES.length) {
            return;
        }
        try {
            RaceNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new org.foxstudio.dinorace.network.RaceInfoPacket(RaceConfig.races()));
            RaceNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new RaceChosenPacket(idx));
            org.foxstudio.dinorace.player.PlayerTags.broadcast(player);
            org.foxstudio.dinorace.player.PlayerTags.syncAllTo(player);
        } catch (Throwable t) {
            // không gửi được thì bỏ qua — lần chọn tộc sau sẽ lưu lại
        }
    }
}