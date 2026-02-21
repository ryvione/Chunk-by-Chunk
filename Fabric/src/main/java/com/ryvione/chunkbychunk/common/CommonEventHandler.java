/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.chunkbychunk.common;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import com.ryvione.chunkbychunk.server.world.SpawnChunkHelper;
import com.ryvione.chunkbychunk.config.ChunkByChunkConfig;
public final class CommonEventHandler {
    private CommonEventHandler() {
    }
    public static boolean isBlockPlacementAllowed(BlockPos pos, Entity playerEntity, LevelAccessor level) {
        return ChunkByChunkConfig.get().getGameplayConfig().isBlockPlacementAllowedOutsideSpawnedChunks() ||
                !playerEntity.level().dimension().equals(Level.OVERWORLD) ||
                !SpawnChunkHelper.isEmptyChunk(level, new ChunkPos(pos));
    }
    public static boolean isBlockPlacementAllowed(BlockPos interactedPos, BlockPos placePos, Entity playerEntity, LevelAccessor level) {
        if (ChunkByChunkConfig.get().getGameplayConfig().isBlockPlacementAllowedOutsideSpawnedChunks()) {
            return true;
        }
        if (!playerEntity.level().dimension().equals(Level.OVERWORLD)) {
            return true;
        }
        if (!SpawnChunkHelper.isEmptyChunk(level, new ChunkPos(interactedPos)) && SpawnChunkHelper.isEmptyChunk(level, new ChunkPos(placePos))) {
            return true;
        }
        return !SpawnChunkHelper.isEmptyChunk(level, new ChunkPos(placePos));
    }
}
