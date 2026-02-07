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

    public static void createNextSpawner(ServerLevel targetLevel, ChunkPos chunkPos) {
        Random random = ChunkUtil.getChunkRandom(targetLevel, chunkPos);
        int minDepth = ChunkByChunkConfig.get().getGeneration().getMinChestSpawnDepth();
        int maxDepth = ChunkByChunkConfig.get().getGeneration().getMaxChestSpawnDepth();

        int minPos = Math.min(minDepth, maxDepth);
        int maxPos = Math.max(minDepth, maxDepth);

        minPos = Math.max(minPos, -64);
        maxPos = Math.min(maxPos, 128);

        if (minPos > maxPos) {
            minPos = maxPos;
        }

        int yPos;
        if (minPos == maxPos) {
            yPos = minPos;
        } else {
            yPos = random.nextInt(maxPos - minPos + 1) + minPos;
        }

        yPos = Math.max(-64, Math.min(128, yPos));

        int xPos = chunkPos.getMinBlockX() + random.nextInt(16);
        int zPos = chunkPos.getMinBlockZ() + random.nextInt(16);
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