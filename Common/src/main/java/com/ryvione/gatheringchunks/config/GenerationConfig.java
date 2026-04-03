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
import com.ryvione.gatheringchunks.config.system.IntRange;
import com.ryvione.gatheringchunks.config.system.Name;

import java.util.Arrays;
import java.util.List;

public class GenerationConfig {

    @Name("enabled")
    @Comment("Is ChunkByChunk generation enabled")
    private boolean enabled = true;

    @Name("seal_world")
    @Comment("Should empty chunks be generated as bedrock")
    private boolean sealWorld = false;

    @Name("synch_nether_chunk_spawn")
    @Comment("Should the nether start empty with chunks spawning in response to overworld spawns")
    private boolean synchNether = false;

    @Name("use_bedrock_chest")
    @Comment("Should the generated chest be a bedrock chest")
    private boolean useBedrockChest = false;

    @Name("chest_contents")
    @Comment("The type of items the bedrock chest provides")
    private ChunkRewardChestContent chestContents = ChunkRewardChestContent.Random;

    @Name("chest_quantity")
    @Comment("The number of items the bedrock chest provides")
    @IntRange(min = 1, max = 64)
    private int chestQuantity = 1;

    @Name("chests_per_chunk")
    @Comment("The number of chests to spawn per newly generated chunk (1-1500, >1500 may cause performance issues)")
    @IntRange(min = 1, max = 65536)
    private int chestsPerChunk = 1;

    @Name("chunk_spawner_mode")
    @Comment("How the chunk spawner works: Edge = spawn adjacent chunks, Void = spawn in void, Both = allow both")
    private ChunkSpawnerMode chunkSpawnerMode = ChunkSpawnerMode.Both;

    @Name("min_chest_spawn_depth")
    @Comment("The minimum depth at which the chunk spawner chest can spawn")
    @IntRange(min = -64, max = 128)
    private int minChestSpawnDepth = -60;

    @Name("max_chest_spawn_depth")
    @Comment("The maximum depth at which the chunk spawner chest can spawn")
    @IntRange(min = -64, max = 128)
    private int maxChestSpawnDepth = -53;

    @Name("initial_chunks")
    @Comment("The number of chunks to spawn initially")
    @IntRange(min = 0, max = 1000)
    private int initialChunks = 1;
    
    @Name("chunk_layer_spawn_rate")
    @IntRange(min = 1, max = 512)
    private int chunkLayerSpawnRate = 8;

    @Name("fragment_ore_enabled")
    @Comment("Enable World Fragment Ore generation")
    private boolean fragmentOreEnabled = true;

    @Name("fragment_ore_min_height")
    @Comment("The minimum height at which World Fragment Ore can spawn")
    @IntRange(min = -64, max = 320)
    private int fragmentOreMinHeight = -64;

    @Name("fragment_ore_max_height")
    @Comment("The maximum height at which World Fragment Ore can spawn")
    @IntRange(min = -64, max = 320)
    private int fragmentOreMaxHeight = 32;

    @Name("fragment_ore_vein_size")
    @Comment("The size of World Fragment Ore veins")
    @IntRange(min = 1, max = 64)
    private int fragmentOreVeinSize = 4;

    @Name("fragment_ore_count")
    @Comment("The number of World Fragment Ore veins per chunk")
    @IntRange(min = 1, max = 64)
    private int fragmentOreCount = 8;

    @Name("disable_water_only_chunks")
    @Comment("Prevent spawning chunks that are mostly water (for biome spawners)")
    private boolean disableWaterOnlyChunks = true;

    @Name("initial_chunk_biomes")
    @Comment("List of biome IDs allowed for the initial chunk spawn location.\n" +
             "Leave empty to allow any biome.\n" +
             "Example: [\"minecraft:plains\", \"minecraft:forest\"]")
    private List<String> initialChunkBiomes = new java.util.ArrayList<>(Arrays.asList(
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:forest",
            "minecraft:flower_forest",
            "minecraft:birch_forest",
            "minecraft:old_growth_birch_forest",
            "minecraft:dark_forest",
            "minecraft:taiga",
            "minecraft:old_growth_pine_taiga",
            "minecraft:old_growth_spruce_taiga",
            "minecraft:savanna",
            "minecraft:savanna_plateau",
            "minecraft:windswept_savanna",
            "minecraft:jungle",
            "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle",
            "minecraft:cherry_grove",
            "minecraft:meadow",
            "minecraft:windswept_forest",
            "minecraft:windswept_hills",
            "minecraft:grove"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getChunkLayerSpawnRate() {
        return chunkLayerSpawnRate;
    }

    public void setChunkLayerSpawnRate(int chunkLayerSpawnRate) {
        this.chunkLayerSpawnRate = chunkLayerSpawnRate;
    }

    public boolean isSynchNether() {
        return synchNether;
    }

    public void setSynchNether(boolean synchNether) {
        this.synchNether = synchNether;
    }

    public boolean useBedrockChest() {
        return useBedrockChest;
    }

    public void setUseBedrockChest(boolean useBedrockChest) {
        this.useBedrockChest = useBedrockChest;
    }

    public int getInitialChunks() {
        return initialChunks;
    }

    public void setInitialChunks(int initialChunks) {
        this.initialChunks = initialChunks;
    }

    public int getChestQuantity() {
        return chestQuantity;
    }

    public void setChestQuantity(int chestQuantity) {
        this.chestQuantity = chestQuantity;
    }

    public int getChestsPerChunk() {
        return chestsPerChunk;
    }

    public void setChestsPerChunk(int chestsPerChunk) {
        this.chestsPerChunk = Math.max(1, chestsPerChunk);
    }

    public ChunkRewardChestContent getChestContents() {
        return chestContents;
    }

    public void setChestContents(ChunkRewardChestContent chestContents) {
        this.chestContents = chestContents;
    }

    public ChunkSpawnerMode getChunkSpawnerMode() {
        return chunkSpawnerMode;
    }

    public void setChunkSpawnerMode(ChunkSpawnerMode chunkSpawnerMode) {
        this.chunkSpawnerMode = chunkSpawnerMode;
    }

    public int getMinChestSpawnDepth() {
        return minChestSpawnDepth;
    }

    public void setMinChestSpawnDepth(int minChestSpawnDepth) {
        this.minChestSpawnDepth = minChestSpawnDepth;
    }

    public int getMaxChestSpawnDepth() {
        return maxChestSpawnDepth;
    }

    public void setMaxChestSpawnDepth(int maxChestSpawnDepth) {
        this.maxChestSpawnDepth = maxChestSpawnDepth;
    }

    public boolean sealWorld() {
        return sealWorld;
    }

    public void setSealWorld(boolean sealWorld) {
        this.sealWorld = sealWorld;
    }

    public List<String> getInitialChunkBiomes() {
        return initialChunkBiomes;
    }

    public void setInitialChunkBiomes(List<String> initialChunkBiomes) {
        this.initialChunkBiomes = initialChunkBiomes;
    }

    public boolean isFragmentOreEnabled() {
        return fragmentOreEnabled;
    }

    public void setFragmentOreEnabled(boolean fragmentOreEnabled) {
        this.fragmentOreEnabled = fragmentOreEnabled;
    }

    public int getFragmentOreMinHeight() {
        return fragmentOreMinHeight;
    }

    public void setFragmentOreMinHeight(int fragmentOreMinHeight) {
        this.fragmentOreMinHeight = fragmentOreMinHeight;
    }

    public int getFragmentOreMaxHeight() {
        return fragmentOreMaxHeight;
    }

    public void setFragmentOreMaxHeight(int fragmentOreMaxHeight) {
        this.fragmentOreMaxHeight = fragmentOreMaxHeight;
    }

    public int getFragmentOreVeinSize() {
        return fragmentOreVeinSize;
    }

    public void setFragmentOreVeinSize(int fragmentOreVeinSize) {
        this.fragmentOreVeinSize = fragmentOreVeinSize;
    }

    public int getFragmentOreCount() {
        return fragmentOreCount;
    }

    public void setFragmentOreCount(int fragmentOreCount) {
        this.fragmentOreCount = fragmentOreCount;
    }
}