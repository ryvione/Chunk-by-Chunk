/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.common.util;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.config.system.ConfigSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class ConfigUtil {
    private static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    private ConfigUtil() {
    }

    private static final ConfigSystem system = new ConfigSystem();

    public static void loadDefaultConfig() {
        synchronized (system) {
            Path configPath = ConfigSystem.getCentralConfigPath(GatheringChunksConstants.CONFIG_FILE);
            LOGGER.info("[ConfigUtil] Loading config from centralized location: {}", configPath);
            system.synchConfig(configPath, ChunkByChunkConfig.get());
        }
    }


    public static void reloadConfig() {
        synchronized (system) {
            Path configPath = ConfigSystem.getCentralConfigPath(GatheringChunksConstants.CONFIG_FILE);
            LOGGER.info("[ConfigUtil] Reloading config from: {}", configPath);
            system.reloadConfig(configPath, ChunkByChunkConfig.get());
        }
    }


    public static void saveDefaultConfig() {
        synchronized (system) {
            Path configPath = ConfigSystem.getCentralConfigPath(GatheringChunksConstants.CONFIG_FILE);
            LOGGER.info("[ConfigUtil] Saving config to: {}", configPath);
            system.write(configPath, ChunkByChunkConfig.get());
        }
    }
}