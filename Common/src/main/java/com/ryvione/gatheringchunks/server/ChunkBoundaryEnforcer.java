/*
 * Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.server;

import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChunkBoundaryEnforcer {

    private static final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private static final long WARNING_COOLDOWN = 100;

    public static void checkPlayerBoundaries(ServerPlayer player) {
        if (!ChunkByChunkConfig.get().getGatheringChunksConfig().isEnforceChunkBoundaries()) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        ChunkPos currentChunk = new ChunkPos(playerPos);

        if (SpawnChunkHelper.isEmptyChunk(player.serverLevel(), currentChunk)) {
            BlockPos safePos = findNearestSpawnedChunk(player);
            if (safePos != null) {
                player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

                long currentTime = player.level().getGameTime();
                Long lastWarning = lastWarningTime.get(player.getUUID());

                if (lastWarning == null || currentTime - lastWarning >= WARNING_COOLDOWN) {
                    player.sendSystemMessage(Component.literal("§c§lYou cannot leave spawned chunks in Hard Mode!"));
                    player.sendSystemMessage(Component.literal("§eSpawn more chunks using a Chunk Spawner."));
                    lastWarningTime.put(player.getUUID(), currentTime);
                }
            }
        }
    }

    private static BlockPos findNearestSpawnedChunk(ServerPlayer player) {
        BlockPos playerPos = player.blockPosition();
        ChunkPos playerChunk = new ChunkPos(playerPos);

        for (int radius = 1; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    ChunkPos checkChunk = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
                    if (!SpawnChunkHelper.isEmptyChunk(player.serverLevel(), checkChunk)) {
                        return checkChunk.getMiddleBlockPosition(playerPos.getY());
                    }
                }
            }
        }

        return player.getRespawnPosition();
    }
}