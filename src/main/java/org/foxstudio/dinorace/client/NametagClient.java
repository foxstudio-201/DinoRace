package org.foxstudio.dinorace.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.foxstudio.dinorace.DinoRace;

/**
 * Nametag player: tên giữ màu trắng, thêm [lv: X] màu theo cấp độ.
 */
@Mod.EventBusSubscriber(modid = DinoRace.MODID, value = Dist.CLIENT)
public final class NametagClient {

    private static final int[][] TIERS = {
            {10, 0xFFB0B0B0},   // 0-9
            {20, 0xFF55FF55},   // 10-19
            {30, 0xFF55AAFF},   // 20-29
            {40, 0xFFAA55FF},   // 30-39
            {50, 0xFFFFAA00},   // 40-49
            {60, 0xFFFF5555},   // 50-59
            {Integer.MAX_VALUE, 0xFFFF6B81} // 60+
    };

    private NametagClient() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int level = RaceClientData.tagLevel(player.getUUID());
        if (level < 0) {
            return;
        }
        int color = 0xFFFFCC44;
        for (int[] t : TIERS) {
            if (level < t[0]) {
                color = t[1];
                break;
            }
        }
        Component name = event.getContent().copy().withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
        Component tag = Component.literal(" [lv: " + level + "]")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
        event.setContent(name.copy().append(tag));
    }
}