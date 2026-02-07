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
public class BedrockChestConfig {
    @Name("bedrock_chest_unlock_at_blocks_remaining")
    @Comment("The number of blocks within the chunk above the bedrock chest allowed to remain before it will open")
    @IntRange(min = 0, max = Short.MAX_VALUE * 2)
    private int bedrockChestBlocksRemainingThreshold = 16;
    public int getBedrockChestBlocksRemainingThreshold() {
        return bedrockChestBlocksRemainingThreshold;
    }
    public void setBedrockChestBlocksRemainingThreshold(int bedrockChestBlocksRemainingThreshold) {
        this.bedrockChestBlocksRemainingThreshold = bedrockChestBlocksRemainingThreshold;
    }
}
