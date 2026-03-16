package com.ryvione.gatheringchunks.fabric;

import com.ryvione.gatheringchunks.client.screens.StarterBookScreen;
import com.ryvione.gatheringchunks.common.network.C2SSaveConfigPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class FabricClientHelper {
    public static void openStarterBook() {
        Minecraft.getInstance().setScreen(new StarterBookScreen());
    }

    public static void sendConfigSavePacket(C2SSaveConfigPacket packet) {
        ClientPlayNetworking.send(packet);
    }
}
