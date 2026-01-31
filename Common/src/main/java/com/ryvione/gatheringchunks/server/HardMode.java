package com.ryvione.gatheringchunks.server;

import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;

public class HardMode {

    public static void init() {
    }

    public static boolean isHardModeEnabled() {
        return ChunkByChunkConfig.get().getGatheringChunksConfig().isHardMode();
    }
}