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

public class GatheringChunksConfig {

    @Comment("Allow mobs to drop world fragments when killed")
    @Name("Mobs Drop Fragments")
    private boolean mobsDropFragments = true;

    @Comment("Percentage chance for mobs to drop fragments (0-100)")
    @Name("Fragment Drop Chance (%)")
    @IntRange(min = 0, max = 100)
    private int fragmentDropChance = 5;

    @Comment("Minimum fragments dropped by mobs (when they do drop)")
    @Name("Min Fragment Drop")
    @IntRange(min = 1, max = 16)
    private int minFragmentDrop = 1;

    @Comment("Maximum fragments dropped by mobs (when they do drop)")
    @Name("Max Fragment Drop")
    @IntRange(min = 1, max = 16)
    private int maxFragmentDrop = 3;

    @Comment("Enable automatic tree spawning in chunks without wood")
    @Name("Auto-Spawn Trees")
    private boolean autoSpawnTrees = true;

    @Comment("Prevent fluids from flowing into the void")
    @Name("Prevent Fluid Flow Into Void")
    private boolean preventFluidFlowIntoVoid = true;

    @Comment("Where a themed chunk spawner draws its terrain from. DIRECT: the same coordinate as the "
            + "target chunk, so the terrain matches the surrounding world (biome-only change). RANDOM: "
            + "a random coordinate inside the theme's own dimension, giving terrain suited to the theme "
            + "but no longer lined up with neighbouring chunks. Applies to themed spawner blocks and the "
            + "World Mender alike.")
    @Name("Themed Spawner Source")
    private ThemedSpawnerSource themedSpawnerSource = ThemedSpawnerSource.DIRECT;

    public enum ThemedSpawnerSource {
        DIRECT,
        RANDOM
    }

    public ThemedSpawnerSource getThemedSpawnerSource() {
        return themedSpawnerSource;
    }

    public void setThemedSpawnerSource(ThemedSpawnerSource themedSpawnerSource) {
        this.themedSpawnerSource = themedSpawnerSource != null ? themedSpawnerSource : ThemedSpawnerSource.DIRECT;
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

    public boolean isAutoSpawnTrees() {
        return autoSpawnTrees;
    }

    public void setAutoSpawnTrees(boolean autoSpawnTrees) {
        this.autoSpawnTrees = autoSpawnTrees;
    }

    public boolean isPreventFluidFlowIntoVoid() {
        return preventFluidFlowIntoVoid;
    }

    public void setPreventFluidFlowIntoVoid(boolean preventFluidFlowIntoVoid) {
        this.preventFluidFlowIntoVoid = preventFluidFlowIntoVoid;
    }
}