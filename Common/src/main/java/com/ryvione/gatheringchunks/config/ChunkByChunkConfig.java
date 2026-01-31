/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.config;
import com.ryvione.gatheringchunks.config.system.Name;

public class ChunkByChunkConfig {
    private static final ChunkByChunkConfig instance = new ChunkByChunkConfig();

    public static ChunkByChunkConfig get() {
        return instance;
    }

    @Name("ChunkGeneration")
    private final GenerationConfig generation = new GenerationConfig();

    @Name("Gameplay")
    private final GameplayConfig gameplayConfig = new GameplayConfig();

    @Name("GatheringChunks")
    private final GatheringChunksConfig gatheringChunksConfig = new GatheringChunksConfig();

    @Name("WorldForge")
    private final WorldForgeConfig worldForge = new WorldForgeConfig();

    @Name("WorldScanner")
    private final WorldScannerConfig worldScannerConfig = new WorldScannerConfig();

    @Name("WorldMender")
    private final WorldMenderConfig worldMenderConfig = new WorldMenderConfig();

    @Name("BedrockChest")
    private final BedrockChestConfig bedrockChest = new BedrockChestConfig();

    public GenerationConfig getGeneration() {
        return generation;
    }

    public BedrockChestConfig getBedrockChest() {
        return bedrockChest;
    }

    public WorldForgeConfig getWorldForge() {
        return worldForge;
    }

    public GameplayConfig getGameplayConfig() {
        return gameplayConfig;
    }

    public GatheringChunksConfig getGatheringChunksConfig() {
        return gatheringChunksConfig;
    }

    public WorldScannerConfig getWorldScannerConfig() {
        return worldScannerConfig;
    }

    public WorldMenderConfig getWorldMenderConfig() {
        return worldMenderConfig;
    }
}