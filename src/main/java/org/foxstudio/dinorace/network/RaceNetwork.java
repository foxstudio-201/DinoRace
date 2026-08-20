package org.foxstudio.dinorace.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.foxstudio.dinorace.DinoRace;

public final class RaceNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DinoRace.MODID, "race"),
            () -> VERSION,
            s -> true,
            s -> true
    );

    private RaceNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(0, RaceInfoPacket.class,
                RaceInfoPacket::encode, RaceInfoPacket::decode, RaceInfoPacket::handle);
        CHANNEL.registerMessage(1, OpenRaceFlowPacket.class,
                OpenRaceFlowPacket::encode, OpenRaceFlowPacket::decode, OpenRaceFlowPacket::handle);
        CHANNEL.registerMessage(2, RaceSelectPacket.class,
                RaceSelectPacket::encode, RaceSelectPacket::decode, RaceSelectPacket::handle);
        CHANNEL.registerMessage(3, RaceChosenPacket.class,
                RaceChosenPacket::encode, RaceChosenPacket::decode, RaceChosenPacket::handle);
        CHANNEL.registerMessage(4, PlayerLevelPacket.class,
                PlayerLevelPacket::encode, PlayerLevelPacket::decode, PlayerLevelPacket::handle);
        CHANNEL.registerMessage(5, RaceRequestPacket.class,
                RaceRequestPacket::encode, RaceRequestPacket::decode, RaceRequestPacket::handle);
        CHANNEL.registerMessage(6, PlayerTagPacket.class,
                PlayerTagPacket::encode, PlayerTagPacket::decode, PlayerTagPacket::handle);
    }
}