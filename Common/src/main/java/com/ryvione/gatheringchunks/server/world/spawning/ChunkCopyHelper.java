package com.ryvione.gatheringchunks.server.world.spawning;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.server.world.ControllableChunkMap;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;

import java.util.Arrays;

public final class ChunkCopyHelper {

    private ChunkCopyHelper() {}

    public static void copyBlocks(ServerLevel sourceLevel, ChunkPos sourceChunkPos,
            ServerLevel targetLevel, ChunkPos targetChunkPos,
            int fromLayer, int toLayer, boolean overwrite) {
        ChunkAccess targetChunkAccess = targetLevel.getChunkSource().getChunkNow(targetChunkPos.x, targetChunkPos.z);
        if (targetChunkAccess != null && !targetChunkAccess.getPersistedStatus().isOrAfter(
                net.minecraft.world.level.chunk.status.ChunkStatus.FULL)) {
            return;
        }

        int xOffset = targetChunkPos.getMinBlockX() - sourceChunkPos.getMinBlockX();
        int zOffset = targetChunkPos.getMinBlockZ() - sourceChunkPos.getMinBlockZ();

        Block sealedBlock = Blocks.BEDROCK;
        if (targetLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator skyChunkGenerator
                && skyChunkGenerator.getGenerationType() == SkyChunkGenerator.EmptyGenerationType.Sealed) {
            sealedBlock = skyChunkGenerator.getSealBlock();
        }

        BlockPos.MutableBlockPos sourceBlock = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos targetBlock = new BlockPos.MutableBlockPos();

        for (int y = fromLayer; y < toLayer; y++) {
            for (int z = sourceChunkPos.getMinBlockZ(); z <= sourceChunkPos.getMaxBlockZ(); z++) {
                for (int x = sourceChunkPos.getMinBlockX(); x <= sourceChunkPos.getMaxBlockX(); x++) {
                    sourceBlock.set(x, y, z);
                    targetBlock.set(x + xOffset, y, z + zOffset);

                    BlockState existingState = targetLevel.getBlockState(targetBlock);
                    Block existingBlock = existingState.getBlock();
                    boolean isLavaInNether = targetLevel.dimension() == Level.NETHER && existingBlock == Blocks.LAVA;

                    if (overwrite || existingBlock instanceof AirBlock || existingBlock instanceof LiquidBlock
                            || existingBlock == sealedBlock || existingBlock == Blocks.SNOW || isLavaInNether) {
                        BlockState newBlock = sourceLevel.getBlockState(sourceBlock);
                        if (!overwrite && !targetLevel.getBlockState(targetBlock).isAir()) {
                            continue;
                        }
                        if (ChunkByChunkConfig.get().getGameplayConfig().isChunkSpawnLeafDecayDisabled()
                                && newBlock.getBlock() instanceof LeavesBlock) {
                            newBlock = newBlock.setValue(LeavesBlock.PERSISTENT, true);
                        }
                        targetLevel.setBlock(targetBlock, newBlock, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                        BlockEntity fromBlockEntity = sourceLevel.getBlockEntity(sourceBlock);
                        BlockEntity toBlockEntity = targetLevel.getBlockEntity(targetBlock);
                        if (fromBlockEntity != null && toBlockEntity != null) {
                            toBlockEntity.loadWithComponents(
                                    fromBlockEntity.saveWithFullMetadata(targetLevel.registryAccess()),
                                    targetLevel.registryAccess());
                            targetLevel.setBlockEntity(toBlockEntity);
                        }
                    }
                }
            }
        }
    }

    public static void updateBiomes(ServerLevel sourceLevel, ChunkAccess sourceChunk,
            ServerLevel targetLevel, ChunkAccess targetChunk, ChunkPos targetChunkPos) {
        if (sourceChunk.getSections().length != targetChunk.getSections().length) {
            GatheringChunksConstants.LOGGER.warn("Section count mismatch between {} and {} - {} vs {}",
                    sourceLevel.dimension(), targetLevel.dimension(),
                    sourceChunk.getSections().length, targetChunk.getSections().length);
        }
        if (!(targetChunk instanceof LevelChunk levelChunk)) return;

        boolean biomesUpdated = false;
        for (int targetIndex = 0; targetIndex < targetChunk.getSections().length; targetIndex++) {
            int sourceIndex = (targetIndex < sourceChunk.getSections().length)
                    ? targetIndex : sourceChunk.getSections().length - 1;

            PalettedContainerRO<Holder<Biome>> sourceBiomes = sourceChunk.getSections()[sourceIndex].getBiomes();
            LevelChunkSection targetSection = levelChunk.getSections()[targetIndex];

            byte[] sourceBuffer = new byte[sourceBiomes.getSerializedSize()];
            FriendlyByteBuf sourceBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(sourceBuffer));
            sourceBuf.writerIndex(0);
            sourceBiomes.write(sourceBuf);

            PalettedContainerRO<Holder<Biome>> targetBiomesRO = targetSection.getBiomes();
            byte[] targetBuffer = new byte[targetBiomesRO.getSerializedSize()];
            FriendlyByteBuf targetBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(targetBuffer));
            targetBuf.writerIndex(0);
            targetBiomesRO.write(targetBuf);

            if (!Arrays.equals(sourceBuffer, targetBuffer)) {
                sourceBuf.readerIndex(0);
                PalettedContainer<Holder<Biome>> newBiomes = targetBiomesRO.recreate();
                newBiomes.read(sourceBuf);
                try {
                    PalettedContainer<BlockState> blockStates =
                            (PalettedContainer<BlockState>) targetSection.getStates();
                    levelChunk.getSections()[targetIndex] = new LevelChunkSection(blockStates, newBiomes);
                    biomesUpdated = true;
                } catch (Exception e) {
                    GatheringChunksConstants.LOGGER.error("Failed to update biomes", e);
                }
                targetChunk.setUnsaved(true);
            }
        }
        if (biomesUpdated) {
            ((ControllableChunkMap) targetLevel.getChunkSource().chunkMap).forceReloadChunk(targetChunkPos);
        }
    }

    public static void triggerLightingUpdate(ServerLevel level, ChunkPos chunkPos) {
        try {
            level.getLightEngine().checkBlock(chunkPos.getMiddleBlockPosition(level.getMaxBuildHeight() / 2));

            int minX = chunkPos.getMinBlockX();
            int minZ = chunkPos.getMinBlockZ();
            int maxX = chunkPos.getMaxBlockX();
            int maxZ = chunkPos.getMaxBlockZ();

            for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y += 16) {
                level.getLightEngine().checkBlock(new BlockPos(minX, y, minZ));
                level.getLightEngine().checkBlock(new BlockPos(maxX, y, minZ));
                level.getLightEngine().checkBlock(new BlockPos(minX, y, maxZ));
                level.getLightEngine().checkBlock(new BlockPos(maxX, y, maxZ));
            }

            int[][] borderOffsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] off : borderOffsets) {
                ChunkPos neighbor = new ChunkPos(chunkPos.x + off[0], chunkPos.z + off[1]);
                if (level.hasChunk(neighbor.x, neighbor.z)) {
                    int edgeX = off[0] == 1 ? chunkPos.getMaxBlockX() : (off[0] == -1 ? chunkPos.getMinBlockX() : chunkPos.getMiddleBlockX());
                    int edgeZ = off[1] == 1 ? chunkPos.getMaxBlockZ() : (off[1] == -1 ? chunkPos.getMinBlockZ() : chunkPos.getMiddleBlockZ());
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y += 16) {
                        level.getLightEngine().checkBlock(new BlockPos(edgeX, y, edgeZ));
                    }
                }
            }
        } catch (Exception e) {
            GatheringChunksConstants.LOGGER.warn("[LightUpdate] Failed to trigger lighting update for chunk {}: {}",
                    chunkPos, e.getMessage());
        }
    }
}
