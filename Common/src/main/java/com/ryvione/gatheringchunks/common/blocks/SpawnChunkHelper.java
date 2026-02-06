package com.ryvione.gatheringchunks.common.blocks;

import com.ryvione.gatheringchunks.common.CommonRegistry;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.util.ChunkUtil;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.config.ChunkSpawnerMode;
import com.ryvione.gatheringchunks.server.world.ChestTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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

    private static int resolveY(Random random) {
        int minDepth = ChunkByChunkConfig.get().getGeneration().getMinChestSpawnDepth();
        int maxDepth = ChunkByChunkConfig.get().getGeneration().getMaxChestSpawnDepth();

        int minPos = Math.max(Math.min(minDepth, maxDepth), -64);
        int maxPos = Math.min(Math.max(minDepth, maxDepth), 128);
        if (minPos > maxPos) minPos = maxPos;

        return (minPos == maxPos) ? minPos : random.nextInt(maxPos - minPos + 1) + minPos;
    }


    private static ChunkPos findSpawnedNeighbour(ServerLevel level, ChunkPos chunkPos, int[] directionOut) {
        ChunkPos candidate;

        candidate = new ChunkPos(chunkPos.x + 1, chunkPos.z);
        if (!isEmptyChunk(level, candidate)) { directionOut[0] = 0; return candidate; }

        candidate = new ChunkPos(chunkPos.x - 1, chunkPos.z);
        if (!isEmptyChunk(level, candidate)) { directionOut[0] = 1; return candidate; }

        candidate = new ChunkPos(chunkPos.x, chunkPos.z + 1);
        if (!isEmptyChunk(level, candidate)) { directionOut[0] = 2; return candidate; }

        candidate = new ChunkPos(chunkPos.x, chunkPos.z - 1);
        if (!isEmptyChunk(level, candidate)) { directionOut[0] = 3; return candidate; }

        return null;
    }

    private static BlockPos resolveEdgePosition(ChunkPos neighbourChunk, int directionOfNeighbour, int yPos, Random random) {
        int x, z;
        switch (directionOfNeighbour) {
            case 0:
                x = neighbourChunk.getMinBlockX();
                z = neighbourChunk.getMinBlockZ() + random.nextInt(16);
                break;
            case 1:
                x = neighbourChunk.getMaxBlockX();
                z = neighbourChunk.getMinBlockZ() + random.nextInt(16);
                break;
            case 2:
                x = neighbourChunk.getMinBlockX() + random.nextInt(16);
                z = neighbourChunk.getMinBlockZ();
                break;
            case 3:
                x = neighbourChunk.getMinBlockX() + random.nextInt(16);
                z = neighbourChunk.getMaxBlockZ();
                break;
            default:
                x = neighbourChunk.getMiddleBlockX();
                z = neighbourChunk.getMiddleBlockZ();
                break;
        }
        return new BlockPos(x, yPos, z);
    }

    public static void createNextSpawner(ServerLevel targetLevel, ChunkPos chunkPos) {
        Random random = ChunkUtil.getChunkRandom(targetLevel, chunkPos);
        int yPos = resolveY(random);

        ChunkSpawnerMode mode = ChunkByChunkConfig.get().getGeneration().getChunkSpawnerMode();
        BlockPos spawnerPos;

        if (mode == ChunkSpawnerMode.Edge) {
            int[] dirOut = new int[1];
            ChunkPos neighbour = findSpawnedNeighbour(targetLevel, chunkPos, dirOut);

            if (neighbour != null) {
                spawnerPos = resolveEdgePosition(neighbour, dirOut[0], yPos, random);
            } else {
                spawnerPos = randomPosInChunk(chunkPos, yPos, random);
            }
        } else {
            spawnerPos = randomPosInChunk(chunkPos, yPos, random);
        }

        targetLevel.setBlock(spawnerPos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(GatheringChunksConstants.MOD_ID, "chunkspawner")).defaultBlockState(), Block.UPDATE_CLIENTS);
        if (ChunkByChunkConfig.get().getDifficulty().spawnNewChunkChest()) {
            placeRewardChest(targetLevel, chunkPos, random);
        }
    }


    private static BlockPos randomPosInChunk(ChunkPos chunkPos, int yPos, Random random) {
        return new BlockPos(
                chunkPos.getMinBlockX() + random.nextInt(16),
                yPos,
                chunkPos.getMinBlockZ() + random.nextInt(16));
    }

    private static void placeRewardChest(ServerLevel targetLevel, ChunkPos chunkPos, Random random) {
        int chestY = resolveY(random);
        BlockPos chestPos = randomPosInChunk(chunkPos, chestY, random);

        if (ChunkByChunkConfig.get().getGeneration().useBedrockChest()) {
            targetLevel.setBlock(chestPos, CommonRegistry.BEDROCK_CHEST_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else {
            targetLevel.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

        if (targetLevel.getBlockEntity(chestPos) instanceof RandomizableContainerBlockEntity chestEntity) {
            List<ItemStack> items = ChunkByChunkConfig.get().getGeneration().getChestContents().getItems(random,
                    ChunkByChunkConfig.get().getGeneration().getChestQuantity());
            for (int i = 0; i < items.size(); i++) {
                chestEntity.setItem(i, items.get(i));
            }
        }

        ChestTracker tracker = ChestTracker.get(targetLevel.getServer());
        tracker.addChest(chestPos);

        for (ServerPlayer player : targetLevel.getServer().getPlayerList().getPlayers()) {
            if (tracker.isTrackerEnabled(player.getUUID())) {
                player.sendSystemMessage(
                        Component.literal("\u00a76[Chunk Chest] \u00a7eA new chest has been spawned at \u00a7b" +
                                chestPos.getX() + ", " + chestPos.getY() + ", " + chestPos.getZ() +
                                " \u00a7ein chunk \u00a7b[" + chunkPos.x + ", " + chunkPos.z + "]")
                );
            }
        }
    }
}