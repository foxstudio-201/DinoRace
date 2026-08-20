package org.foxstudio.dinorace.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.foxstudio.dinorace.DinoRace;

/**
 * Bấm phím đăng ký (mặc định R, đổi được trong Controls) để mở GUI thông tin tộc.
 */
@Mod.EventBusSubscriber(modid = DinoRace.MODID, value = Dist.CLIENT)
public final class KeyBindingsClient {

    private KeyBindingsClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (KeyBindings.OPEN_RACE_DETAIL.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }
            int idx = RaceDetailScreen.lastRaceIndex();
            if (idx < 0) {
                RaceDetailScreen.requestOpen();
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "Đang đồng bộ tộc của bạn — bấm phím tắt lần nữa sau khi đã chọn tộc..."), true);
                return;
            }
            mc.setScreen(new RaceDetailScreen(idx));
        }
    }
}