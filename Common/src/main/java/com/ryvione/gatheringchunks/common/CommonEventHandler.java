/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.common;

import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class CommonEventHandler {
    private CommonEventHandler() {
    }
    public static boolean isBlockPlacementAllowed(BlockPos pos, Player playerEntity, Level level) {
        return ChunkByChunkConfig.get().getGameplayConfig().isBlockPlacementAllowedOutsideSpawnedChunks() ||
                !playerEntity.level().dimension().equals(Level.OVERWORLD) ||
                !SpawnChunkHelper.isEmptyChunk(level, new ChunkPos(pos));
    }
}