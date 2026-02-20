/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.server;

import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.chunk.LevelChunk;

import java.lang.reflect.Method;
import java.util.Random;

public class CauldronRainFiller {

    private static final Random RANDOM = new Random();
    private static final float FILL_CHANCE = 0.05f;

    private static Method getChunksMethod;
    static {
        Method found = null;
        for (Method m : ChunkMap.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && Iterable.class.isAssignableFrom(m.getReturnType())) {
                m.setAccessible(true);
                found = m;
                break;
            }
        }
        if (found == null) {
            throw new RuntimeException("Failed to find getChunks() method in ChunkMap");
        }
        getChunksMethod = found;
    }

    @SuppressWarnings("unchecked")
    public static void tick(ServerLevel level) {
        if (!(level.getChunkSource().getGenerator() instanceof SkyChunkGenerator)) return;
        if (!level.isRaining()) return;

        ChunkMap chunkMap = level.getChunkSource().chunkMap;
        Iterable<ChunkHolder> chunks;
        try {
            chunks = (Iterable<ChunkHolder>) getChunksMethod.invoke(chunkMap);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (ChunkHolder holder : chunks) {
            LevelChunk levelChunk = holder.getChunkToSend();
            if (levelChunk == null) continue;

            ChunkPos chunkPos = levelChunk.getPos();
            if (SpawnChunkHelper.isEmptyChunk(level, chunkPos)) continue;
            if (RANDOM.nextFloat() > FILL_CHANCE * 16) continue;

            int x = chunkPos.getMinBlockX() + RANDOM.nextInt(16);
            int z = chunkPos.getMinBlockZ() + RANDOM.nextInt(16);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos top = new BlockPos(x, y - 1, z);
            var state = level.getBlockState(top);

            if (state.is(Blocks.CAULDRON)) {
                level.setBlockAndUpdate(top, Blocks.WATER_CAULDRON.defaultBlockState()
                        .setValue(LayeredCauldronBlock.LEVEL, 1));
            } else if (state.is(Blocks.WATER_CAULDRON)) {
                int current = state.getValue(LayeredCauldronBlock.LEVEL);
                if (current < 3) {
                    level.setBlockAndUpdate(top, state.setValue(LayeredCauldronBlock.LEVEL, current + 1));
                }
            }
        }
    }
}