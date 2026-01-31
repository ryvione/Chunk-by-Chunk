/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.common.util;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.config.system.ConfigSystem;

import java.nio.file.Paths;
public final class ConfigUtil {
    private ConfigUtil() {
    }
    private static final ConfigSystem system = new ConfigSystem();
    public static void loadDefaultConfig() {
        synchronized (system) {
            system.synchConfig(Paths.get(GatheringChunksConstants.DEFAULT_CONFIG_PATH).resolve(GatheringChunksConstants.CONFIG_FILE), ChunkByChunkConfig.get());
        }
    }
    public static void saveDefaultConfig() {
        synchronized (system) {
            system.write(Paths.get(GatheringChunksConstants.DEFAULT_CONFIG_PATH).resolve(GatheringChunksConstants.CONFIG_FILE), ChunkByChunkConfig.get());
        }
    }
}
