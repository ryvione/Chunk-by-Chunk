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
import com.ryvione.gatheringchunks.config.system.Comment;
import com.ryvione.gatheringchunks.config.system.IntRange;
import com.ryvione.gatheringchunks.config.system.Name;
public class WorldScannerConfig {
    @Name("fuel_per_fragment")
    @IntRange(min = 1, max = 512)
    @Comment("The amount of fuel provided by each world fragment (and then scaled up for world shard, crystal and core")
    private int fuelPerFragment = 32;
    @Name("fuel_required_per_chunk")
    @Comment("The amount of fuel required to scan each chunk")
    @IntRange(min = 1, max = Short.MAX_VALUE * 2)
    private int fuelRequiredPerChunk = 32;
    @Name("fuel_consumed_per_tick")
    @Comment("The amount of fuel consumed each tick")
    @IntRange(min = 1, max = Short.MAX_VALUE * 2)
    private int fuelConsumedPerTick = 1;

    @Name("experimental_mode")
    @Comment("Enable experimental mode for the world scanner which highlights blocks in the world")
    private boolean experimentalMode = false;
    public int getFuelConsumedPerTick() {
        return fuelConsumedPerTick;
    }
    public void setFuelConsumedPerTick(int fuelConsumedPerTick) {
        this.fuelConsumedPerTick = fuelConsumedPerTick;
    }
    public int getFuelPerFragment() {
        return fuelPerFragment;
    }
    public void setFuelPerFragment(int fuelPerFragment) {
        this.fuelPerFragment = fuelPerFragment;
    }
    public int getFuelRequiredPerChunk() {
        return fuelRequiredPerChunk;
    }

    public void setFuelRequiredPerChunk(int fuelRequiredPerChunk) {
        this.fuelRequiredPerChunk = fuelRequiredPerChunk;
    }

    public boolean isExperimentalMode() {
        return experimentalMode;
    }

    public void setExperimentalMode(boolean experimentalMode) {
        this.experimentalMode = experimentalMode;
    }
}
