package org.foxstudio.dinorace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.foxstudio.dinorace.DinoRace;
import org.joml.Matrix4f;

/**
 * Nametag player tích hợp chức năng SneakyNameTag:
 * - Chỉ hiện nametag khi crosshair đang ngắm trúng player (không crouching).
 * - Khi hiện: thêm [lv: X] màu theo cấp độ, tự vẽ để không bị mod khác chặn.
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
        if (!shouldShowNametag(player)) {
            return;
        }
        int level = RaceClientData.tagLevel(player.getUUID());
        if (level < 0) {
            level = 0;
        }
        int color = 0xFFFFCC44;
        for (int[] t : TIERS) {
            if (level < t[0]) {
                color = t[1];
                break;
            }
        }
        Component name = event.getContent().copy().withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
        Component tag = Component.literal("[lv: " + level + "] ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
        Component content = tag.copy().append(name);
        event.setContent(content);
        drawNameTag(event, player, content);
    }

    private static boolean shouldShowNametag(Player player) {
        Minecraft client = Minecraft.getInstance();
        HitResult hit = client.hitResult;
        boolean isTargeted = hit != null
                && hit.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) hit).getEntity() == player;
        return isTargeted
                && player.shouldShowName()
                && !player.isCrouching();
    }

    private static void drawNameTag(RenderNameTagEvent event, Player player, Component content) {
        try {
            Minecraft mc = Minecraft.getInstance();
            PoseStack pose = event.getPoseStack();
            MultiBufferSource buffer = event.getMultiBufferSource();
            int light = event.getPackedLight();
            double distSq = mc.getEntityRenderDispatcher().distanceToSqr(player);
            if (distSq > 4096.0 * 4096.0) {
                return;
            }
            boolean seeThrough = !player.isDiscrete();
            float offY = pehkuiAdjustedNameTagY(player, event.getPartialTick());
            int bg = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
            Font font = mc.font;
            pose.pushPose();
            pose.translate(0.0F, offY, 0.0F);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(-0.025F, -0.025F, 0.025F);
            Matrix4f m4 = pose.last().pose();
            float x = (float) (-font.width(content) / 2);
            font.drawInBatch(content, x, 0.0F, 553648127, false, m4, buffer,
                    seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, bg, light);
            if (seeThrough) {
                font.drawInBatch(content, x, 0.0F, -1, false, m4, buffer,
                        Font.DisplayMode.NORMAL, 0, light);
            }
            pose.popPose();
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        } catch (Throwable t) {
            // không vẽ được thì bỏ qua
        }
    }

    private static float pehkuiAdjustedNameTagY(Player player, float partialTick) {
        float base = player.getNameTagOffsetY();
        float bb = player.getBbHeight();
        float scale = 1.0F;
        try {
            Class<?> scaleUtils = Class.forName("virtuoel.pehkui.util.ScaleUtils");
            java.lang.reflect.Method m = scaleUtils.getMethod("getBoundingBoxHeightScale", net.minecraft.world.entity.Entity.class, float.class);
            Object v = m.invoke(null, player, partialTick);
            if (v instanceof Number n && n.floatValue() > 0.0001F) {
                scale = n.floatValue();
            }
        } catch (Throwable ignored) {
        }
        return base - bb + bb / scale;
    }
}
