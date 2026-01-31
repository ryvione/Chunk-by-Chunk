package com.ryvione.gatheringchunks.server;

import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

public class HardMode {

    public static void init(MinecraftServer server) {
        if (ChunkByChunkConfig.get().getGatheringChunksConfig().isHardMode()) {
            server.overworld().getServer().setDifficulty(Difficulty.HARD, true);
        }
    }

    public static boolean isHardModeEnabled() {
        return ChunkByChunkConfig.get().getGatheringChunksConfig().isHardMode();
    }
}