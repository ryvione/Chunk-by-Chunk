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