/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.config;

import com.ryvione.gatheringchunks.config.system.Comment;
import com.ryvione.gatheringchunks.config.system.Name;

public class GameplayConfig {
    @Name("block_placement_allowed_outside_spawned_chunks")
    @Comment("Can blocks be placed outside spawned chunks")
    private boolean blockPlacementAllowedOutsideSpawnedChunks = false;

    @Name("chunk_spawn_leaf_decay_disabled")
    @Comment("Prevent leaves spawned by the chunk spawners from decaying")
    private boolean chunkSpawnLeafDecayDisabled = false;

    @Name("enable_chunk_barriers")
    @Comment(
            "If enabled, barrier blocks will be placed around the edges of each chunk\n" +
                    "to prevent water, lava, and other liquids from flowing between chunks.\n" +
                    "Barriers are automatically removed when adjacent chunks are spawned.\n" +
                    "This prevents players from easily reaching lower chunks by following liquids."
    )
    private boolean enableChunkBarriers = true;

    @Name("unstable_chunk_chance")
    @Comment("Percentage chance for unstable chunks to spawn in chests (0-100)")
    private int unstableChunkChance = 20;

    public boolean isBlockPlacementAllowedOutsideSpawnedChunks() {
        return blockPlacementAllowedOutsideSpawnedChunks;
    }

    public void setBlockPlacementAllowedOutsideSpawnedChunks(boolean blockPlacementAllowedOutsideSpawnedChunks) {
        this.blockPlacementAllowedOutsideSpawnedChunks = blockPlacementAllowedOutsideSpawnedChunks;
    }

    public boolean isChunkSpawnLeafDecayDisabled() {
        return chunkSpawnLeafDecayDisabled;
    }

    public void setChunkSpawnLeafDecayDisabled(boolean chunkSpawnLeafDecayDisabled) {
        this.chunkSpawnLeafDecayDisabled = chunkSpawnLeafDecayDisabled;
    }

    public boolean isEnableChunkBarriers() {
        return enableChunkBarriers;
    }

    public void setEnableChunkBarriers(boolean enableChunkBarriers) {
        this.enableChunkBarriers = enableChunkBarriers;
    }

    @Name("enable_world_fragment_ore")
    @Comment("If disabled, World Fragment Ores will not generate in the world")
    private boolean enableWorldFragmentOre = true;

    @Name("enable_cave_scanner")
    @Comment("If disabled, the Cave Scanner will be non-functional")
    private boolean enableCaveScanner = true;

    public boolean isEnableWorldFragmentOre() {
        return enableWorldFragmentOre;
    }

    public void setEnableWorldFragmentOre(boolean enableWorldFragmentOre) {
        this.enableWorldFragmentOre = enableWorldFragmentOre;
    }

    public boolean isEnableCaveScanner() {
        return enableCaveScanner;
    }

    public void setEnableCaveScanner(boolean enableCaveScanner) {
        this.enableCaveScanner = enableCaveScanner;
    }

    public int getUnstableChunkChance() {
        return unstableChunkChance;
    }

    public void setUnstableChunkChance(int unstableChunkChance) {
        this.unstableChunkChance = Math.max(0, Math.min(100, unstableChunkChance));
    }

    public enum StartRestriction {
        None,
        Village,
        Biome
    }
}