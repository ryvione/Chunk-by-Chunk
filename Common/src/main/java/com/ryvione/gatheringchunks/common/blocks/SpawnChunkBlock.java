package com.ryvione.gatheringchunks.common.blocks;

import com.ryvione.gatheringchunks.interop.Services;
import com.ryvione.gatheringchunks.server.world.ChunkSpawnController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class SpawnChunkBlock extends Block {
    private static final EnumSet<Direction> HORIZONTAL_DIR = EnumSet.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private final String biomeTheme;
    private final boolean random;

    public SpawnChunkBlock(String biomeTheme, boolean random, Properties blockProperties) {
        super(blockProperties);
        this.biomeTheme = biomeTheme;
        this.random = random;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel) {
            ChunkSpawnController chunkSpawnController = ChunkSpawnController.get(serverLevel.getServer());

            String effectiveBiomeTheme = biomeTheme;
            boolean effectiveRandom = random;

            if (biomeTheme.isEmpty() && !random) {
                ChunkPos currentChunk = new ChunkPos(pos);
                String inheritedBiome = findAdjacentBiomeTheme(serverLevel, currentChunk);
                if (inheritedBiome != null && !inheritedBiome.isEmpty()) {
                    effectiveBiomeTheme = inheritedBiome;
                }
            }

            if (chunkSpawnController.isValidForLevel(serverLevel, effectiveBiomeTheme, effectiveRandom)) {
                List<BlockPos> targetPositions = new ArrayList<>();
                BlockPos initialPos = pos.atY(level.getMaxBuildHeight() - 1);
                targetPositions.add(initialPos);
                Direction targetDirection = hit.getDirection();
                if (!HORIZONTAL_DIR.contains(targetDirection)) {
                    targetDirection = Direction.NORTH;
                }
                targetPositions.add(initialPos.relative(targetDirection.getOpposite()));
                targetPositions.add(initialPos.relative(targetDirection.getCounterClockWise()));
                targetPositions.add(initialPos.relative(targetDirection.getClockWise()));
                targetPositions.add(initialPos.relative(targetDirection));
                for (BlockPos targetPos : targetPositions) {
                    if (chunkSpawnController.request(serverLevel, effectiveBiomeTheme, effectiveRandom, targetPos)) {
                        level.playSound(null, pos, Services.PLATFORM.spawnChunkSoundEffect(), SoundSource.BLOCKS, 1.0f, 1.0f);
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    private String findAdjacentBiomeTheme(ServerLevel level, ChunkPos currentChunk) {
        for (Direction dir : HORIZONTAL_DIR) {
            ChunkPos adjacentChunk = new ChunkPos(
                    currentChunk.x + dir.getStepX(),
                    currentChunk.z + dir.getStepZ()
            );

            if (!isEmptyChunk(level, adjacentChunk)) {
                BlockPos centerPos = adjacentChunk.getMiddleBlockPosition(level.getMaxBuildHeight() - 10);

                for (int y = level.getMaxBuildHeight() - 10; y >= level.getMinBuildHeight(); y--) {
                    BlockPos checkPos = new BlockPos(centerPos.getX(), y, centerPos.getZ());
                    Block block = level.getBlockState(checkPos).getBlock();

                    if (block instanceof SpawnChunkBlock spawnBlock) {
                        String theme = spawnBlock.getBiomeTheme();
                        if (!theme.isEmpty()) {
                            return theme;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isEmptyChunk(Level level, ChunkPos chunkPos) {
        BlockPos bedrockCheckBlock = chunkPos.getMiddleBlockPosition(level.getMinBuildHeight());
        return !Blocks.BEDROCK.equals(level.getBlockState(bedrockCheckBlock).getBlock());
    }

    public String getBiomeTheme() {
        return biomeTheme;
    }

    public boolean isRandom() {
        return random;
    }
}