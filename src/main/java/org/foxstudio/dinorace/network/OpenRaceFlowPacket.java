package org.foxstudio.dinorace.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import org.foxstudio.dinorace.client.SystemInstallingScreen;

import java.util.function.Supplier;

/**
 * S2C: yêu cầu client chạy luồng boot "System Installing" → GUI chọn chủng tộc.
 */
public class OpenRaceFlowPacket {

    public OpenRaceFlowPacket() {
    }

    public static void encode(OpenRaceFlowPacket msg, FriendlyByteBuf buf) {
    }

    public static OpenRaceFlowPacket decode(FriendlyByteBuf buf) {
        return new OpenRaceFlowPacket();
    }

    public static void handle(OpenRaceFlowPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                handleClient();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient() {
        Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().setScreen(new SystemInstallingScreen()));
    }
}