/*
 * Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.config;

import com.ryvione.gatheringchunks.config.system.Comment;
import com.ryvione.gatheringchunks.config.system.IntRange;
import com.ryvione.gatheringchunks.config.system.Name;

public class GatheringChunksConfig {

    @Comment("Hard Mode - Disables village spawning and enforces strict chunk boundaries")
    @Name("Hard Mode")
    private boolean hardMode = false;

    @Comment("Allow mobs to drop world fragments when killed")
    @Name("Mobs Drop Fragments")
    private boolean mobsDropFragments = true;

    @Comment("Percentage chance for mobs to drop fragments (0-100)")
    @Name("Fragment Drop Chance (%)")
    @IntRange(min = 0, max = 100)
    private int fragmentDropChance = 5;

    @Comment("Prevent players from leaving spawned chunks (Hard Mode feature)")
    @Name("Enforce Chunk Boundaries")
    private boolean enforceChunkBoundaries = false;

    @Comment("Minimum fragments dropped by mobs (when they do drop)")
    @Name("Min Fragment Drop")
    @IntRange(min = 1, max = 16)
    private int minFragmentDrop = 1;

    @Comment("Maximum fragments dropped by mobs (when they do drop)")
    @Name("Max Fragment Drop")
    @IntRange(min = 1, max = 16)
    private int maxFragmentDrop = 3;

    @Comment("Enable automatic progression helper (gives chunk spawner if stuck)")
    @Name("Enable Progression Helper")
    private boolean enableProgressionHelper = true;

    @Comment("Enable automatic tree spawning in chunks without wood")
    @Name("Auto-Spawn Trees")
    private boolean autoSpawnTrees = true;

    public boolean isHardMode() {
        return hardMode;
    }

    public void setHardMode(boolean hardMode) {
        this.hardMode = hardMode;
        if (hardMode) {
            this.enforceChunkBoundaries = true;
        }
    }

    public boolean isMobsDropFragments() {
        return mobsDropFragments;
    }

    public void setMobsDropFragments(boolean mobsDropFragments) {
        this.mobsDropFragments = mobsDropFragments;
    }

    public int getFragmentDropChance() {
        return fragmentDropChance;
    }

    public void setFragmentDropChance(int fragmentDropChance) {
        this.fragmentDropChance = Math.max(0, Math.min(100, fragmentDropChance));
    }

    public boolean isEnforceChunkBoundaries() {
        return enforceChunkBoundaries;
    }

    public void setEnforceChunkBoundaries(boolean enforceChunkBoundaries) {
        this.enforceChunkBoundaries = enforceChunkBoundaries;
    }

    public int getMinFragmentDrop() {
        return minFragmentDrop;
    }

    public void setMinFragmentDrop(int minFragmentDrop) {
        this.minFragmentDrop = Math.max(1, Math.min(16, minFragmentDrop));
    }

    public int getMaxFragmentDrop() {
        return maxFragmentDrop;
    }

    public void setMaxFragmentDrop(int maxFragmentDrop) {
        this.maxFragmentDrop = Math.max(1, Math.min(16, maxFragmentDrop));
    }

    public boolean isEnableProgressionHelper() {
        return enableProgressionHelper;
    }

    public void setEnableProgressionHelper(boolean enableProgressionHelper) {
        this.enableProgressionHelper = enableProgressionHelper;
    }

    public boolean isAutoSpawnTrees() {
        return autoSpawnTrees;
    }

    public void setAutoSpawnTrees(boolean autoSpawnTrees) {
        this.autoSpawnTrees = autoSpawnTrees;
    }
}