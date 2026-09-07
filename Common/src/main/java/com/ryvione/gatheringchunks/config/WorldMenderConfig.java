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
public class WorldMenderConfig {
    @Name("cooldown")
    @IntRange(min = 1, max = 72000)
    @Comment("Ticks between chunk spawns per world mender (world menders will not spawn chunks if chunks are already being spawned)")
    private int cooldown = 1;
    @Name("emptyRescanInterval")
    @IntRange(min = 1, max = 1200000)
    @Comment("When a world mender finds no empty chunks to fill, it sleeps this many ticks before "
            + "scanning again. Keeps the empty-chunk scan off the tick loop in settled areas, while "
            + "letting a dormant mender notice chunks erased nearby (e.g. by a Chunk Eraser) within "
            + "about half a minute at the default value. Upstream Chunk By Chunk hard-coded 1200000 "
            + "(~16.7 in-game hours), which made menders effectively permanently dormant.")
    private int emptyRescanInterval = 600;
    public int getCooldown() {
        return cooldown;
    }
    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }
    public int getEmptyRescanInterval() {
        return emptyRescanInterval;
    }
    public void setEmptyRescanInterval(int emptyRescanInterval) {
        this.emptyRescanInterval = Math.max(1, emptyRescanInterval);
    }
}