package org.foxstudio.dinorace.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.foxstudio.dinorace.DinoRace;

import org.lwjgl.glfw.GLFW;

/**
 * Đăng ký phím tắt mở GUI thông tin tộc — có thể đổi phím trong
 * Options → Controls (mục "Mở thông tin tộc").
 */
@Mod.EventBusSubscriber(modid = DinoRace.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeyBindings {

    public static final KeyMapping OPEN_RACE_DETAIL = new KeyMapping(
            "key.dinorace.open_race_detail",
            GLFW.GLFW_KEY_R,
            KeyMapping.CATEGORY_GAMEPLAY);

    private KeyBindings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_RACE_DETAIL);
    }
}