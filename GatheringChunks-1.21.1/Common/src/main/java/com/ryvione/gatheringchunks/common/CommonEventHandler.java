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