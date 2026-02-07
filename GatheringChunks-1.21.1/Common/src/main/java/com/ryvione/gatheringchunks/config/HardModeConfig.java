/*
 * Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.config;

import com.ryvione.gatheringchunks.config.system.Comment;
import com.ryvione.gatheringchunks.config.system.Name;

public class HardModeConfig {

    @Name("enabled")
    @Comment("Enable Hard Mode")
    private boolean enabled = false;

    @Name("enforce_chunk_boundaries")
    @Comment("Prevent players from leaving spawned chunks")
    private boolean enforceChunkBoundaries = true;

    @Name("disable_villages")
    @Comment("Disable village spawning")
    private boolean disableVillages = true;

    @Name("spawn_initial_engine")
    @Comment("Spawn a Chunk Engine in the first chunk")
    private boolean spawnInitialEngine = true;

    @Name("initial_engine_fuel")
    @Comment("Give the initial Chunk Engine some starting fuel")
    private boolean initialEngineFuel = true;

    @Name("disable_chests_command")
    @Comment("Disable the /chests command")
    private boolean disableChestsCommand = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnforceChunkBoundaries() {
        return enforceChunkBoundaries;
    }

    public void setEnforceChunkBoundaries(boolean enforceChunkBoundaries) {
        this.enforceChunkBoundaries = enforceChunkBoundaries;
    }

    public boolean isDisableVillages() {
        return disableVillages;
    }

    public void setDisableVillages(boolean disableVillages) {
        this.disableVillages = disableVillages;
    }

    public boolean isSpawnInitialEngine() {
        return spawnInitialEngine;
    }

    public void setSpawnInitialEngine(boolean spawnInitialEngine) {
        this.spawnInitialEngine = spawnInitialEngine;
    }

    public boolean isInitialEngineFuel() {
        return initialEngineFuel;
    }

    public void setInitialEngineFuel(boolean initialEngineFuel) {
        this.initialEngineFuel = initialEngineFuel;
    }

    public boolean isDisableChestsCommand() {
        return disableChestsCommand;
    }

    public void setDisableChestsCommand(boolean disableChestsCommand) {
        this.disableChestsCommand = disableChestsCommand;
    }
}