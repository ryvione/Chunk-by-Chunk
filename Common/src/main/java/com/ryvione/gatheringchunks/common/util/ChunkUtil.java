/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.common.util;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Random;
import java.util.Set;
public final class ChunkUtil {
    private ChunkUtil() {
    }
    public static Random getChunkRandom(ServerLevel targetLevel, ChunkPos chunkPos) {
        long seed = targetLevel.getSeed() + GatheringChunksConstants.MOD_ID.hashCode();
        Random random = new Random(seed);
        long xModifier = random.nextLong();
        long yModifier = random.nextLong();
        long chunkSeed = (long) chunkPos.x & xModifier ^ (long) chunkPos.z * yModifier ^ seed;
        random.setSeed(chunkSeed);
        return random;
    }
    public static int getSafeSpawnHeight(ChunkAccess chunk, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x,chunk.getMaxBuildHeight() - 1,z);
        while (pos.getY() > chunk.getMinBuildHeight()) {
            if (chunk.getBlockState(pos).getBlock().isPossibleToRespawnInThis(chunk.getBlockState(pos))) {
                break;
            }
            pos.setY(pos.getY() - 1);
        }
        while (pos.getY() > chunk.getMinBuildHeight()) {
            if (!chunk.getBlockState(pos).getBlock().isPossibleToRespawnInThis(chunk.getBlockState(pos))) {
                return pos.getY() + 1;
            }
            pos.setY(pos.getY() - 1);
        }
        return pos.getY();
    }
    public static int countBlocks(ChunkAccess chunk, Set<Block> blocks) {
        if (blocks.size() == 0) {
            return 0;
        }
        if (blocks.size() == 1) {
            return countBlocks(chunk, blocks.stream().findFirst().get());
        }
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0,0,0);
        int count = 0;
        for (pos.setX(chunkPos.getMinBlockX()); pos.getX() <= chunkPos.getMaxBlockX(); pos.setX(pos.getX() + 1)) {
            for (pos.setY(chunk.getMinBuildHeight()); pos.getY() <= chunk.getMaxBuildHeight() - 1; pos.setY(pos.getY() + 1)) {
                for (pos.setZ(chunkPos.getMinBlockZ()); pos.getZ() <= chunkPos.getMaxBlockZ(); pos.setZ(pos.getZ() + 1)) {
                    if (blocks.contains(chunk.getBlockState(pos).getBlock())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
    public static int countBlocks(ChunkAccess chunk, Block block) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0,0,0);
        int count = 0;
        for (pos.setX(chunkPos.getMinBlockX()); pos.getX() <= chunkPos.getMaxBlockX(); pos.setX(pos.getX() + 1)) {
            for (pos.setY(chunk.getMinBuildHeight()); pos.getY() <= chunk.getMaxBuildHeight() - 1; pos.setY(pos.getY() + 1)) {
                for (pos.setZ(chunkPos.getMinBlockZ()); pos.getZ() <= chunkPos.getMaxBlockZ(); pos.setZ(pos.getZ() + 1)) {
                    if (chunk.getBlockState(pos).getBlock() == block) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
    public static int countBlocks(ChunkAccess chunk, TagKey<Block> blockTag) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0,0,0);
        int count = 0;
        for (pos.setX(chunkPos.getMinBlockX()); pos.getX() <= chunkPos.getMaxBlockX(); pos.setX(pos.getX() + 1)) {
            for (pos.setY(chunk.getMinBuildHeight()); pos.getY() <= chunk.getMaxBuildHeight(); pos.setY(pos.getY() + 1)) {
                for (pos.setZ(chunkPos.getMinBlockZ()); pos.getZ() <= chunkPos.getMaxBlockZ(); pos.setZ(pos.getZ() + 1)) {
                    if (chunk.getBlockState(pos).is(blockTag)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
