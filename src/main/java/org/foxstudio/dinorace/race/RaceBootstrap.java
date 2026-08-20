package org.foxstudio.dinorace.race;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.foxstudio.dinorace.DinoRace;

/**
 * Tự nạp liệu race config (data races.json) lúc server khởi động.
 */
@Mod.EventBusSubscriber(modid = DinoRace.MODID)
public final class RaceBootstrap {

    private RaceBootstrap() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        RaceConfig.load(event.getServer());
    }
}