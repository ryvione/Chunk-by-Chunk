/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.api;

import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.config.GameplayConfig;
import com.ryvione.gatheringchunks.config.GenerationConfig;
import com.ryvione.gatheringchunks.config.GatheringChunksConfig;
import com.ryvione.gatheringchunks.config.WorldScannerConfig;
import com.ryvione.gatheringchunks.config.DifficultyConfig;
import com.ryvione.gatheringchunks.config.HardModeConfig;

public class ConfigAPI {
    
    public static GameplayConfig getGameplayConfig() {
        return ChunkByChunkConfig.get().getGameplayConfig();
    }
    
    
    public static GenerationConfig getGenerationConfig() {
        return ChunkByChunkConfig.get().getGeneration();
    }
    
    
    public static GatheringChunksConfig getGatheringChunksConfig() {
        return ChunkByChunkConfig.get().getGatheringChunksConfig();
    }
    
   
    public static WorldScannerConfig getWorldScannerConfig() {
        return ChunkByChunkConfig.get().getWorldScannerConfig();
    }
    
    
    public static DifficultyConfig getDifficultyConfig() {
        return ChunkByChunkConfig.get().getDifficulty();
    }
   
    public static HardModeConfig getHardModeConfig() {
        return ChunkByChunkConfig.get().getDifficulty().getHardMode();
    }
    
  
    public static boolean isEnabled() {
        return ChunkByChunkConfig.get().getGeneration().isEnabled();
    }
    
   
    public static boolean isSyncNetherEnabled() {
        return ChunkByChunkConfig.get().getGeneration().isSynchNether();
    }
    
  
    public static String getWorldScannerMode() {
        return ChunkByChunkConfig.get().getWorldScannerConfig().getWorldScannerScanMode().name();
    }
    
  
    public static boolean isWorldScannerManualMode() {
        return ChunkByChunkConfig.get().getWorldScannerConfig().getWorldScannerScanMode() 
            == WorldScannerConfig.WorldScannerMode.Manual;
    }
}