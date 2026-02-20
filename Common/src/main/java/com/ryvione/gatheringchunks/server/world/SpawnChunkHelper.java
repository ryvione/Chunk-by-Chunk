/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.server.world;

import com.ryvione.gatheringchunks.common.util.ChunkUtil;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.interop.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SpawnChunkHelper {
    private SpawnChunkHelper() {
    }

    public static boolean isEmptyChunk(Level level, ChunkPos chunkPos) {
        BlockPos bedrockCheckBlock = chunkPos.getMiddleBlockPosition(level.getMinBuildHeight());
        if (Blocks.BEDROCK.equals(level.getBlockState(bedrockCheckBlock).getBlock())) {
            return false;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();

        int[][] samples = {{8, 8}, {2, 2}, {13, 13}, {2, 13}, {13, 2}};
        for (int[] sample : samples) {
            pos.set(chunkPos.getMinBlockX() + sample[0], minY + 1, chunkPos.getMinBlockZ() + sample[1]);
            if (!level.getBlockState(pos).isAir()) {
                return false;
            }
        }

        return true;
    }

    private static BlockPos pickChestPos(ServerLevel targetLevel, ChunkPos chunkPos, Random random, List<BlockPos> usedPositions) {
        int minDepth = ChunkByChunkConfig.get().getGeneration().getMinChestSpawnDepth();
        int maxDepth = ChunkByChunkConfig.get().getGeneration().getMaxChestSpawnDepth();

        int minPos = Math.min(minDepth, maxDepth);
        int maxPos = Math.max(minDepth, maxDepth);

        minPos = Math.max(minPos, targetLevel.getMinBuildHeight());
        maxPos = Math.min(maxPos, targetLevel.getMaxBuildHeight());

        if (minPos > maxPos) minPos = maxPos;

        int attempts = 0;
        while (attempts < 20) {
            int yPos;
            if (minPos == maxPos) {
                yPos = minPos;
            } else {
                yPos = random.nextInt(maxPos - minPos + 1) + minPos;
            }
            yPos = Math.max(targetLevel.getMinBuildHeight(), Math.min(targetLevel.getMaxBuildHeight(), yPos));

            int xPos = chunkPos.getMinBlockX() + random.nextInt(16);
            int zPos = chunkPos.getMinBlockZ() + random.nextInt(16);
            BlockPos candidate = new BlockPos(xPos, yPos, zPos);

            boolean tooClose = false;
            for (BlockPos used : usedPositions) {
                if (candidate.distSqr(used) < 9) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                return candidate;
            }
            attempts++;
        }

        int yPos;
        if (minPos == maxPos) {
            yPos = minPos;
        } else {
            yPos = random.nextInt(maxPos - minPos + 1) + minPos;
        }
        return new BlockPos(
                chunkPos.getMinBlockX() + random.nextInt(16),
                yPos,
                chunkPos.getMinBlockZ() + random.nextInt(16));
    }

    public static void createNextSpawner(ServerLevel targetLevel, ChunkPos chunkPos) {
        Random random = ChunkUtil.getChunkRandom(targetLevel, chunkPos);

        int chestsToSpawn = ChunkByChunkConfig.get().getGeneration().getChestsPerChunk();

        List<BlockPos> spawnedPositions = new ArrayList<>();
        ChestTracker tracker = ChestTracker.get(targetLevel.getServer());

        for (int i = 0; i < chestsToSpawn; i++) {
            BlockPos blockPos = pickChestPos(targetLevel, chunkPos, random, spawnedPositions);

            if (ChunkByChunkConfig.get().getGeneration().useBedrockChest()) {
                targetLevel.setBlock(blockPos, Services.PLATFORM.bedrockChestBlock().defaultBlockState(), Block.UPDATE_CLIENTS);
            } else {
                targetLevel.setBlock(blockPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
            }

            if (targetLevel.getBlockEntity(blockPos) instanceof RandomizableContainerBlockEntity chestEntity) {
                List<ItemStack> items = ChunkByChunkConfig.get().getGeneration().getChestContents().getItems(random,
                        ChunkByChunkConfig.get().getGeneration().getChestQuantity());
                for (int slot = 0; slot < items.size(); slot++) {
                    chestEntity.setItem(slot, items.get(slot));
                }
            }

            spawnedPositions.add(blockPos);
            tracker.addChest(blockPos);

            final BlockPos notifyPos = blockPos;
            for (ServerPlayer player : targetLevel.getServer().getPlayerList().getPlayers()) {
                if (tracker.isTrackerEnabled(player.getUUID())) {
                    if (targetLevel.getBlockState(notifyPos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock ||
                            targetLevel.getBlockState(notifyPos).getBlock() == Services.PLATFORM.bedrockChestBlock()) {
                        player.sendSystemMessage(
                                Component.literal("§6[Chunk Chest] §eA new chest has been spawned at §b" +
                                        notifyPos.getX() + ", " + notifyPos.getY() + ", " + notifyPos.getZ() +
                                        " §ein chunk §b[" + chunkPos.x + ", " + chunkPos.z + "]")
                        );
                    }
                }
            }
        }
    }
}