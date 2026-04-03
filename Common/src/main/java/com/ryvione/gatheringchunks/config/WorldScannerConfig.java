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

public class WorldScannerConfig {
    private static boolean MANUAL_SYSTEM_ENABLED = true;
    @Name("fuel_per_fragment")
    @IntRange(min = 1, max = 512)
    @Comment("The amount of fuel provided by each world fragment (and then scaled up for world shard, crystal and core)")
    private int fuelPerFragment = 32;

    @Name("cave_scanner_fuel_per_fragment")
    @IntRange(min = 1, max = 512)
    @Comment("The amount of fuel provided by each world fragment for the Cave Scanner")
    private int caveScannerFuelPerFragment = 32;
    
    @Name("fuel_required_per_chunk")
    @Comment("The amount of fuel required to scan each chunk")
    @IntRange(min = 1, max = Short.MAX_VALUE * 2)
    private int fuelRequiredPerChunk = 32;

    @Name("cave_scanner_fuel_required_per_chunk")
    @Comment("The amount of fuel required for the Cave Scanner to scan each chunk")
    @IntRange(min = 1, max = Short.MAX_VALUE * 2)
    private int caveScannerFuelRequiredPerChunk = 32;
    
    @Name("fuel_consumed_per_tick")
    @Comment("The amount of fuel consumed each tick")
    @IntRange(min = 1, max = Short.MAX_VALUE * 2)
    private int fuelConsumedPerTick = 1;

    @Name("cave_scanner_fuel_consumed_per_tick")
    @Comment("The amount of fuel consumed each tick by the Cave Scanner")
    @IntRange(min = 1, max = Short.MAX_VALUE * 2)
    private int caveScannerFuelConsumedPerTick = 1;

    @Name("experimental_mode")
    @Comment("Enable experimental mode for the world scanner which highlights blocks in the world")
    private boolean experimentalMode = false;
    
    @Name("world_scanner_scan_mode")
    @Comment("World Scanner scan mode: Auto = scan from center, Manual = select chunks to scan")
    private WorldScannerMode worldScannerScanMode = WorldScannerMode.Auto;
    
    @Name("allow_manual_chunk_selection")
    @Comment("Allow manually selecting which chunk to scan instead of scanning from center")
    private boolean allowManualChunkSelection = false;
    
    @Name("manual_chunk_offset_x")
    @Comment("X offset from scanner position for manual chunk selection (in chunks, -15 to 15)")
    @IntRange(min = -15, max = 15)
    private int manualChunkOffsetX = 0;
    
    @Name("manual_chunk_offset_z")
    @Comment("Z offset from scanner position for manual chunk selection (in chunks, -15 to 15)")
    @IntRange(min = -15, max = 15)
    private int manualChunkOffsetZ = 0;
    
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

    public int getCaveScannerFuelPerFragment() {
        return caveScannerFuelPerFragment;
    }

    public void setCaveScannerFuelPerFragment(int caveScannerFuelPerFragment) {
        this.caveScannerFuelPerFragment = caveScannerFuelPerFragment;
    }

    public int getCaveScannerFuelRequiredPerChunk() {
        return caveScannerFuelRequiredPerChunk;
    }

    public void setCaveScannerFuelRequiredPerChunk(int caveScannerFuelRequiredPerChunk) {
        this.caveScannerFuelRequiredPerChunk = caveScannerFuelRequiredPerChunk;
    }

    public int getCaveScannerFuelConsumedPerTick() {
        return caveScannerFuelConsumedPerTick;
    }

    public void setCaveScannerFuelConsumedPerTick(int caveScannerFuelConsumedPerTick) {
        this.caveScannerFuelConsumedPerTick = caveScannerFuelConsumedPerTick;
    }

    public boolean isExperimentalMode() {
        return experimentalMode;
    }

    public void setExperimentalMode(boolean experimentalMode) {
        this.experimentalMode = experimentalMode;
    }
    
    public WorldScannerMode getWorldScannerScanMode() {
        if (!MANUAL_SYSTEM_ENABLED) return WorldScannerMode.Auto;
        return worldScannerScanMode;
    }
    
    public void setWorldScannerScanMode(WorldScannerMode worldScannerScanMode) {
        if (!MANUAL_SYSTEM_ENABLED) return;
        this.worldScannerScanMode = worldScannerScanMode;
    }
    
    public boolean isAllowManualChunkSelection() {
        if (!MANUAL_SYSTEM_ENABLED) return false;
        return allowManualChunkSelection;
    }
    
    public void setAllowManualChunkSelection(boolean allowManualChunkSelection) {
        if (!MANUAL_SYSTEM_ENABLED) return;
        this.allowManualChunkSelection = allowManualChunkSelection;
    }
    
    public int getManualChunkOffsetX() {
        return manualChunkOffsetX;
    }
    
    public void setManualChunkOffsetX(int manualChunkOffsetX) {
        this.manualChunkOffsetX = Math.max(-15, Math.min(15, manualChunkOffsetX));
    }
    
    public int getManualChunkOffsetZ() {
        return manualChunkOffsetZ;
    }
    
    public void setManualChunkOffsetZ(int manualChunkOffsetZ) {
        this.manualChunkOffsetZ = Math.max(-15, Math.min(15, manualChunkOffsetZ));
    }
    
    public enum WorldScannerMode {
        Auto,
        Manual
    }
}