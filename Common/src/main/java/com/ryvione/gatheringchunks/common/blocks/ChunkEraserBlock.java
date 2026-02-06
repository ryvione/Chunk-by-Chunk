package com.ryvione.gatheringchunks.common.blocks;

import com.ryvione.gatheringchunks.interop.Services;
import com.ryvione.gatheringchunks.server.world.ChunkBarrierManager;
import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ChunkEraserBlock extends Block {
    private static final Map<UUID, PendingErase> pendingErases = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT = 30000;

    public ChunkEraserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            ChunkPos targetChunk = new ChunkPos(pos);

            if (SpawnChunkHelper.isEmptyChunk(level, targetChunk)) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c[ChunkEraser] §eThis chunk is already empty! Nothing to erase."));
                return InteractionResult.FAIL;
            }

            UUID playerId = player.getUUID();
            PendingErase pending = pendingErases.get(playerId);

            if (pending != null && pending.chunkPos.equals(targetChunk) &&
                    System.currentTimeMillis() - pending.timestamp < CONFIRMATION_TIMEOUT) {

                pendingErases.remove(playerId);
                eraseChunk(serverLevel, targetChunk, pos, serverPlayer);
                serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, 0.5f);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return InteractionResult.SUCCESS;
            } else {
                pendingErases.put(playerId, new PendingErase(targetChunk, System.currentTimeMillis()));
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c[ChunkEraser] §6WARNING: §eYou are about to ERASE chunk [" + targetChunk.x + ", " + targetChunk.z + "]!"));
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§eAll blocks will be removed! Right-click again within 30 seconds to confirm."));
                serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5f, 1.5f);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    private void eraseChunk(ServerLevel level, ChunkPos chunkPos, BlockPos eraserPos, ServerPlayer player) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = level.getMinBuildHeight(); y <= level.getMaxBuildHeight(); y++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
                    mutablePos.set(x, y, z);
                    BlockState currentState = level.getBlockState(mutablePos);
                    if (!currentState.isAir()) {
                        level.setBlock(mutablePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }

        ChunkBarrierManager.placeBarriersAroundChunk(level, chunkPos);

        giveRewards(level, eraserPos, player);

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a[ChunkEraser] §eChunk [" + chunkPos.x + ", " + chunkPos.z + "] has been erased to void!"));
    }

    private void giveRewards(ServerLevel level, BlockPos pos, ServerPlayer player) {
        Random random = new Random();

        int fragmentCount = 3 + random.nextInt(5);
        ItemStack fragments = new ItemStack(Services.PLATFORM.worldFragmentItem(), fragmentCount);
        dropOrGive(level, pos, player, fragments);

        if (random.nextFloat() < 0.6f) {
            int shardCount = 1 + random.nextInt(3);
            ItemStack shards = new ItemStack(Services.PLATFORM.worldShardItem(), shardCount);
            dropOrGive(level, pos, player, shards);
        }

        if (random.nextFloat() < 0.3f) {
            ItemStack crystal = new ItemStack(Services.PLATFORM.worldCrystalItem(), 1);
            dropOrGive(level, pos, player, crystal);
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§a[ChunkEraser] §eYou received materials from the erased chunk!"));
    }

    private void dropOrGive(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            level.addFreshEntity(itemEntity);
        }
    }

    public static void cleanupExpiredConfirmations() {
        long currentTime = System.currentTimeMillis();
        pendingErases.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > CONFIRMATION_TIMEOUT);
    }

    private record PendingErase(ChunkPos chunkPos, long timestamp) {}
}