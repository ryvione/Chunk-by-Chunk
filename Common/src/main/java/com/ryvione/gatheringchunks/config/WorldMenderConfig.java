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
public class WorldMenderConfig {
    @Name("cooldown")
    @IntRange(min = 1, max = 72000)
    @Comment("Ticks between chunk spawns per world mender (world menders will not spawn chunks if chunks are already being spawned)")
    private int cooldown = 1;
    public int getCooldown() {
        return cooldown;
    }
    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }
}
