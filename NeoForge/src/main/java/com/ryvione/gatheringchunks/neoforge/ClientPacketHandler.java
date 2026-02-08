package com.ryvione.gatheringchunks.neoforge;

import com.ryvione.gatheringchunks.client.screens.GatheringChunksConfigScreen;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.common.network.S2COpenConfigPacket;
import com.ryvione.gatheringchunks.common.network.S2CSyncConfigPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPacketHandler {

    public static void handleOpenConfig(final S2COpenConfigPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new GatheringChunksConfigScreen(null));
        });
    }

    public static void handleSyncConfig(final S2CSyncConfigPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            ChunkByChunkConfig.get().applySyncJson(payload.configJson());
        });
    }
}
