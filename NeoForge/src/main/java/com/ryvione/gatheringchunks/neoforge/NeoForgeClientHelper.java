package com.ryvione.gatheringchunks.neoforge;

import com.ryvione.gatheringchunks.client.screens.StarterBookScreen;
import com.ryvione.gatheringchunks.common.network.C2SSaveConfigPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgeClientHelper {
    public static void openStarterBook() {
        Minecraft.getInstance().setScreen(new StarterBookScreen());
    }

    public static void sendConfigSavePacket(C2SSaveConfigPacket packet) {
        PacketDistributor.sendToServer(packet);
    }
}
