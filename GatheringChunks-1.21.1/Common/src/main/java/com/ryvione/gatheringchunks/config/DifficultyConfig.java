package com.ryvione.gatheringchunks.config;

import com.ryvione.gatheringchunks.config.system.Comment;
import com.ryvione.gatheringchunks.config.system.Name;

public class DifficultyConfig {

    @Name("Hard Mode")
    private final HardModeConfig hardMode = new HardModeConfig();

    @Name("Chunk Engine Requires Fuel")
    @Comment("Does the Chunk Engine require fuel to operate?")
    private boolean engineRequiresFuel = false;

    @Name("Experimental Chunk Limit")
    @Comment("Enable the experimental chunk limit (Start with 9, upgrade via Engine)")
    private boolean experimentalChunkLimit = false;

    @Comment("Enable automatic progression helper (gives chunk spawner if stuck)")
    @Name("Enable Progression Helper")
    private boolean enableProgressionHelper = true;

    @Name("start_restriction")
    @Comment("What restriction to place on starting location")
    private GameplayConfig.StartRestriction startRestriction = GameplayConfig.StartRestriction.Village;

    @Name("starting_biome")
    @Comment("The tag or name of the biome the starting spawn should be in (if not a village, blank for any)")
    private String startingBiome = "#minecraft:is_forest";

    @Name("always_spawn_village")
    @Comment("Always attempt to spawn the initial chunk in a village (overridden by Hard Mode)")
    private boolean alwaysSpawnVillage = true;

    @Name("spawn_new_chunk_chest")
    @Comment("Should chunks include a chest with materials for generating further chunks?")
    private boolean spawnNewChunkChest = true;

    @Name("spawn_chunk_strip")
    @Comment("Whether to spawn a full strip of chunks along an axis")
    private boolean spawnChunkStrip = false;

    @Name("spawn_chest_in_initial_chunk_only")
    @Comment("Should the chest spawn in the initial chunk only?")
    private boolean spawnChestInInitialChunkOnly = false;

    public HardModeConfig getHardMode() {
        return hardMode;
    }

    public boolean isEngineRequiresFuel() {
        return engineRequiresFuel;
    }

    public void setEngineRequiresFuel(boolean engineRequiresFuel) {
        this.engineRequiresFuel = engineRequiresFuel;
    }

    public boolean isExperimentalChunkLimit() {
        return experimentalChunkLimit;
    }

    public void setExperimentalChunkLimit(boolean experimentalChunkLimit) {
        this.experimentalChunkLimit = experimentalChunkLimit;
    }

    public boolean isEnableProgressionHelper() {
        return enableProgressionHelper;
    }

    public void setEnableProgressionHelper(boolean enableProgressionHelper) {
        this.enableProgressionHelper = enableProgressionHelper;
    }

    public GameplayConfig.StartRestriction getStartRestriction() {
        return startRestriction;
    }

    public void setStartRestriction(GameplayConfig.StartRestriction startRestriction) {
        this.startRestriction = startRestriction;
    }

    public String getStartingBiome() {
        return startingBiome;
    }

    public void setStartingBiome(String startingBiome) {
        this.startingBiome = startingBiome;
    }

    public boolean isAlwaysSpawnVillage() {
        if (hardMode.isEnabled()) {
            return false;
        }
        return alwaysSpawnVillage;
    }

    public void setAlwaysSpawnVillage(boolean alwaysSpawnVillage) {
        this.alwaysSpawnVillage = alwaysSpawnVillage;
    }

    public boolean spawnNewChunkChest() {
        return spawnNewChunkChest;
    }

    public void setSpawnNewChunkChest(boolean spawnNewChunkChest) {
        this.spawnNewChunkChest = spawnNewChunkChest;
    }

    public boolean isSpawnChunkStrip() {
        return spawnChunkStrip;
    }

    public void setSpawnChunkStrip(boolean spawnChunkStrip) {
        this.spawnChunkStrip = spawnChunkStrip;
    }

    public boolean spawnChestInInitialChunkOnly() {
        return spawnChestInInitialChunkOnly;
    }

    public void setSpawnChestInInitialChunkOnly(boolean spawnChestInInitialChunkOnly) {
        this.spawnChestInInitialChunkOnly = spawnChestInInitialChunkOnly;
    }
}