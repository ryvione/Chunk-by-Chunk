package com.ryvione.gatheringchunks.server.world;

import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TreePlacementHandler {

    public static void ensureTreesInChunk(ServerLevel level, ChunkPos chunkPos) {
        if (!ChunkByChunkConfig.get().getGatheringChunksConfig().isAutoSpawnTrees()) {
            return;
        }

        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);

        if (hasVillage(level, chunk)) {
            return;
        }

        if (hasWoodInChunk(level, chunk)) {
            return;
        }

        if (hasMinimumWoodInDimension(level, chunkPos)) {
            return;
        }

        placeTreeInChunk(level, chunk);
    }

    private static boolean hasVillage(ServerLevel level, LevelChunk chunk) {
        for (StructureStart start : chunk.getAllStarts().values()) {
            if (start.isValid()) {
                Holder<net.minecraft.world.level.levelgen.structure.Structure> structure =
                        level.registryAccess()
                                .registryOrThrow(Registries.STRUCTURE)
                                .wrapAsHolder(start.getStructure());
                if (level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(StructureTags.VILLAGE)
                        .map(tag -> tag.contains(structure)).orElse(false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasMinimumWoodInDimension(ServerLevel level, ChunkPos center) {
        int totalLogs = 0;
        int radius = 3;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                if (!level.hasChunk(pos.x, pos.z)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(pos.x, pos.z);

                BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                            mpos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                            if (level.getBlockState(mpos).is(BlockTags.LOGS)) {
                                totalLogs++;
                                if (totalLogs >= 4) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasWoodInChunk(ServerLevel level, LevelChunk chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    BlockState state = level.getBlockState(pos);

                    if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void placeTreeInChunk(ServerLevel level, LevelChunk chunk) {
        List<BlockPos> validPositions = findValidTreePositions(level, chunk);

        if (validPositions.isEmpty()) {
            return;
        }

        Random random = new Random();
        BlockPos treePos = validPositions.get(random.nextInt(validPositions.size()));

        Holder<Biome> biomeHolder = level.getBiome(treePos);
        placeTreeAt(level, treePos, random, biomeHolder);
    }

    private static List<BlockPos> findValidTreePositions(ServerLevel level, LevelChunk chunk) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 2; x < 14; x++) {
            for (int z = 2; z < 14; z++) {
                for (int y = level.getMaxBuildHeight() - 1; y >= level.getMinBuildHeight(); y--) {
                    pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    BlockState state = level.getBlockState(pos);

                    if (!state.isAir() && isExposedToAir(level, pos)) {
                        BlockPos groundPos = pos.immutable();
                        BlockState groundState = level.getBlockState(groundPos);

                        if (groundState.is(BlockTags.DIRT) || groundState.is(Blocks.GRASS_BLOCK) ||
                                groundState.is(Blocks.PODZOL) || groundState.is(Blocks.MYCELIUM) ||
                                groundState.is(Blocks.SAND) || groundState.is(Blocks.RED_SAND)) {
                            positions.add(groundPos.above());
                            break;
                        } else if (groundState.is(Blocks.STONE) || groundState.is(Blocks.DEEPSLATE) ||
                                groundState.is(Blocks.SANDSTONE) || groundState.is(Blocks.RED_SANDSTONE)) {
                            level.setBlock(groundPos, Blocks.DIRT.defaultBlockState(), 3);
                            positions.add(groundPos.above());
                            break;
                        }
                        break;
                    }
                }
            }
        }

        return positions;
    }

    private static boolean isExposedToAir(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        for (int i = 0; i < 8; i++) {
            if (!level.getBlockState(above.above(i)).isAir()) {
                return false;
            }
        }
        return true;
    }

    private static void placeTreeAt(ServerLevel level, BlockPos pos, Random random, Holder<Biome> biomeHolder) {
        if (biomeHolder.is(Biomes.SAVANNA) || biomeHolder.is(Biomes.SAVANNA_PLATEAU) ||
                biomeHolder.is(Biomes.WINDSWEPT_SAVANNA)) {
            placeAcaciaTree(level, pos, random);
        } else if (biomeHolder.is(Biomes.JUNGLE) || biomeHolder.is(Biomes.BAMBOO_JUNGLE) ||
                biomeHolder.is(Biomes.SPARSE_JUNGLE)) {
            placeJungleTree(level, pos, random);
        } else if (biomeHolder.is(Biomes.TAIGA) || biomeHolder.is(Biomes.SNOWY_TAIGA) ||
                biomeHolder.is(Biomes.OLD_GROWTH_PINE_TAIGA) || biomeHolder.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)) {
            placeSpruceTree(level, pos, random);
        } else if (biomeHolder.is(Biomes.BIRCH_FOREST) || biomeHolder.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            placeBirchTree(level, pos, random);
        } else if (biomeHolder.is(Biomes.DARK_FOREST)) {
            if (random.nextBoolean()) {
                placeDarkOakTree(level, pos, random);
            } else {
                placeOakTree(level, pos, random);
            }
        } else if (biomeHolder.is(Biomes.SWAMP) || biomeHolder.is(Biomes.MANGROVE_SWAMP)) {
            placeOakTree(level, pos, random);
        } else if (biomeHolder.is(Biomes.DESERT) || biomeHolder.is(Biomes.BADLANDS) ||
                biomeHolder.is(Biomes.WOODED_BADLANDS) || biomeHolder.is(Biomes.ERODED_BADLANDS)) {
            placeOakTree(level, pos, random);
        } else {
            placeOakTree(level, pos, random);
        }
    }

    private static void placeOakTree(ServerLevel level, BlockPos pos, Random random) {
        int height = 4 + random.nextInt(3);

        for (int i = 0; i < height; i++) {
            level.setBlock(pos.above(i), Blocks.OAK_LOG.defaultBlockState(), 3);
        }

        for (int y = height - 2; y <= height + 1; y++) {
            int radius = (y == height + 1 || y == height - 2) ? 1 : 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0 && y < height + 1) continue;
                    if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;

                    BlockPos leafPos = pos.offset(x, y, z);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(leafPos, Blocks.OAK_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placeBirchTree(ServerLevel level, BlockPos pos, Random random) {
        int height = 5 + random.nextInt(3);

        for (int i = 0; i < height; i++) {
            level.setBlock(pos.above(i), Blocks.BIRCH_LOG.defaultBlockState(), 3);
        }

        for (int y = height - 2; y <= height + 1; y++) {
            int radius = (y == height + 1) ? 1 : 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0 && y < height + 1) continue;
                    if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;

                    BlockPos leafPos = pos.offset(x, y, z);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(leafPos, Blocks.BIRCH_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placeSpruceTree(ServerLevel level, BlockPos pos, Random random) {
        int height = 6 + random.nextInt(4);

        for (int i = 0; i < height; i++) {
            level.setBlock(pos.above(i), Blocks.SPRUCE_LOG.defaultBlockState(), 3);
        }

        for (int y = 1; y < height; y++) {
            int radius = Math.max(1, height - y - 1);
            radius = Math.min(radius, 2);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0) continue;

                    BlockPos leafPos = pos.offset(x, y, z);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(leafPos, Blocks.SPRUCE_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }

        level.setBlock(pos.above(height), Blocks.SPRUCE_LEAVES.defaultBlockState(), 3);
    }

    private static void placeAcaciaTree(ServerLevel level, BlockPos pos, Random random) {
        int height = 5 + random.nextInt(3);

        for (int i = 0; i < height; i++) {
            if (i < 3) {
                level.setBlock(pos.above(i), Blocks.ACACIA_LOG.defaultBlockState(), 3);
            } else {
                int offset = i - 2;
                level.setBlock(pos.offset(offset, i, 0), Blocks.ACACIA_LOG.defaultBlockState(), 3);
            }
        }

        BlockPos canopyCenter = pos.offset(height - 2, height, 0);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 1; y++) {
                    if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;
                    BlockPos leafPos = canopyCenter.offset(x, y, z);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(leafPos, Blocks.ACACIA_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placeJungleTree(ServerLevel level, BlockPos pos, Random random) {
        int height = 7 + random.nextInt(5);

        for (int i = 0; i < height; i++) {
            level.setBlock(pos.above(i), Blocks.JUNGLE_LOG.defaultBlockState(), 3);
        }

        if (random.nextBoolean()) {
            level.setBlock(pos.offset(1, 2, 0), Blocks.VINE.defaultBlockState(), 3);
        }
        if (random.nextBoolean()) {
            level.setBlock(pos.offset(-1, 2, 0), Blocks.VINE.defaultBlockState(), 3);
        }

        for (int y = height - 3; y <= height + 1; y++) {
            int radius = (y == height + 1) ? 1 : 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0 && y < height) continue;

                    BlockPos leafPos = pos.offset(x, y, z);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(leafPos, Blocks.JUNGLE_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void placeDarkOakTree(ServerLevel level, BlockPos pos, Random random) {
        int height = 6 + random.nextInt(3);

        for (int i = 0; i < height; i++) {
            level.setBlock(pos.above(i), Blocks.DARK_OAK_LOG.defaultBlockState(), 3);
            level.setBlock(pos.offset(1, i, 0), Blocks.DARK_OAK_LOG.defaultBlockState(), 3);
            level.setBlock(pos.offset(0, i, 1), Blocks.DARK_OAK_LOG.defaultBlockState(), 3);
            level.setBlock(pos.offset(1, i, 1), Blocks.DARK_OAK_LOG.defaultBlockState(), 3);
        }

        for (int y = height - 2; y <= height + 1; y++) {
            int radius = 3;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if ((x == 0 || x == 1) && (z == 0 || z == 1) && y < height) continue;
                    if (Math.abs(x) == 3 && Math.abs(z) == 3) continue;

                    BlockPos leafPos = pos.offset(x, y, z);
                    if (level.getBlockState(leafPos).isAir()) {
                        level.setBlock(leafPos, Blocks.DARK_OAK_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}