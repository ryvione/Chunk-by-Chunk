/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
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
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import java.util.List;
import java.util.Random;

public final class SpawnChunkHelper {
    private SpawnChunkHelper() {
    }

    public static boolean isEmptyChunk(Level level, ChunkPos chunkPos) {
        BlockPos bedrockCheckBlock = chunkPos.getMiddleBlockPosition(level.getMinBuildHeight());
        return !Blocks.BEDROCK.equals(level.getBlockState(bedrockCheckBlock).getBlock());
    }

    public static void createNextSpawner(ServerLevel targetLevel, ChunkPos chunkPos) {
        Random random = ChunkUtil.getChunkRandom(targetLevel, chunkPos);
        int minPos = Math.min(ChunkByChunkConfig.get().getGeneration().getMinChestSpawnDepth(),
                ChunkByChunkConfig.get().getGeneration().getMaxChestSpawnDepth());
        int maxPos = Math.max(ChunkByChunkConfig.get().getGeneration().getMinChestSpawnDepth(),
                ChunkByChunkConfig.get().getGeneration().getMaxChestSpawnDepth());


        if (maxPos < 0) {
            maxPos = 64;
        }
        if (minPos < 0) {
            minPos = 1;
        }

        if (minPos > maxPos) {
            int temp = minPos;
            minPos = maxPos;
            maxPos = temp;
        }

        BlockPos centerPos = new BlockPos(chunkPos.getMiddleBlockX(), maxPos, chunkPos.getMiddleBlockZ());
        while (maxPos > minPos && targetLevel.getBlockState(centerPos).getBlock() instanceof AirBlock) {
            maxPos--;
            centerPos = new BlockPos(chunkPos.getMiddleBlockX(), maxPos, chunkPos.getMiddleBlockZ());
        }

        int yPos;
        if (minPos == maxPos) {
            yPos = minPos;
        } else {
            yPos = random.nextInt(minPos, maxPos + 1);
        }

        if (yPos < 1) {
            yPos = 1;
        }

        int xPos = chunkPos.getMinBlockX() + random.nextInt(0, 16);
        int zPos = chunkPos.getMinBlockZ() + random.nextInt(0, 16);
        BlockPos blockPos = new BlockPos(xPos, yPos, zPos);

        if (ChunkByChunkConfig.get().getGeneration().useBedrockChest()) {
            targetLevel.setBlock(blockPos, Services.PLATFORM.bedrockChestBlock().defaultBlockState(), Block.UPDATE_CLIENTS);
        } else {
            targetLevel.setBlock(blockPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        if (targetLevel.getBlockEntity(blockPos) instanceof RandomizableContainerBlockEntity chestEntity) {
            List<ItemStack> items = ChunkByChunkConfig.get().getGeneration().getChestContents().getItems(random,
                    ChunkByChunkConfig.get().getGeneration().getChestQuantity());
            for (int i = 0; i < items.size(); i++) {
                chestEntity.setItem(i, items.get(i));
            }
        }

        ChestTracker tracker = ChestTracker.get(targetLevel.getServer());
        tracker.addChest(blockPos);

        for (ServerPlayer player : targetLevel.getServer().getPlayerList().getPlayers()) {
            if (tracker.isTrackerEnabled(player.getUUID())) {
                if (targetLevel.getBlockState(blockPos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock ||
                        targetLevel.getBlockState(blockPos).getBlock() == Services.PLATFORM.bedrockChestBlock()) {

                    player.sendSystemMessage(
                            Component.literal("§6[Chunk Chest] §eA new chest has been spawned at §b" +
                                    blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ() +
                                    " §ein chunk §b[" + chunkPos.x + ", " + chunkPos.z + "]")
                    );
                }
            }
        }
    }
}