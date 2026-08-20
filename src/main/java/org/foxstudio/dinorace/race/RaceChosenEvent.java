package org.foxstudio.dinorace.race;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Sự kiện Forge phát ra khi player vừa chọn xong chủng tộc (server side).
 * Các mod khác (vd dinocore chạy MCA destiny) lắng nghe để xử lý tiếp.
 */
public class RaceChosenEvent extends Event {

    public final ServerPlayer player;
    public final String originId;
    public final int raceIndex;

    public RaceChosenEvent(ServerPlayer player, String originId, int raceIndex) {
        this.player = player;
        this.originId = originId;
        this.raceIndex = raceIndex;
    }
}