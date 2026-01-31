package com.ryvione.gatheringchunks.config;

import com.ryvione.gatheringchunks.config.system.Comment;
import com.ryvione.gatheringchunks.config.system.Name;

public class GameplayConfig {
    @Name("block_placement_allowed_outside_spawned_chunks")
    @Comment("Can blocks be placed outside spawned chunks")
    private boolean blockPlacementAllowedOutsideSpawnedChunks = true;

    @Name("start_restriction")
    @Comment("What restriction to place on starting location")
    private StartRestriction startRestriction = StartRestriction.Village;

    @Name("starting_biome")
    @Comment("The tag or name of the biome the starting spawn should be in (if not a village, blank for any")
    private String startingBiome = "#minecraft:is_forest";

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

    public boolean isBlockPlacementAllowedOutsideSpawnedChunks() {
        return blockPlacementAllowedOutsideSpawnedChunks;
    }

    public void setBlockPlacementAllowedOutsideSpawnedChunks(boolean blockPlacementAllowedOutsideSpawnedChunks) {
        this.blockPlacementAllowedOutsideSpawnedChunks = blockPlacementAllowedOutsideSpawnedChunks;
    }

    public StartRestriction getStartRestriction() {
        return startRestriction;
    }

    public void setStartRestriction(StartRestriction startRestriction) {
        this.startRestriction = startRestriction;
    }

    public String getStartingBiome() {
        return startingBiome;
    }

    public void setStartingBiome(String startingBiome) {
        this.startingBiome = startingBiome;
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

    public enum StartRestriction {
        None,
        Village,
        Biome
    }
}