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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.mojang.datafixers.util.Pair;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.blockEntities.WorldScannerBlockEntity;
import com.ryvione.gatheringchunks.common.data.ScannerData;
import com.ryvione.gatheringchunks.common.data.SkyDimensionData;
import com.ryvione.gatheringchunks.common.util.ChunkUtil;
import com.ryvione.gatheringchunks.common.util.ConfigUtil;
import com.ryvione.gatheringchunks.common.util.SpiralIterator;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.interop.Services;
import com.ryvione.gatheringchunks.common.mixinterface.IHolderReference;
import com.ryvione.gatheringchunks.common.mixinterface.IMultiNoiseBiomeSource;
import com.ryvione.gatheringchunks.server.world.*;
import com.ryvione.gatheringchunks.server.world.WorldMigrationManager;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class ServerEventHandler {
    private static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    private static final int MAX_FIND_CHUNK_ATTEMPTS = 512;
    private static final int CHUNK_SPAWN_TIMEOUT_SECONDS = 30;

    private static final List<List<int[]>> CHUNK_SPAWN_OFFSETS = ImmutableList.<List<int[]>>builder()
            .add(ImmutableList.of(new int[] { 0, 0 }))
            .add(ImmutableList.of(new int[] { 0, 0 }, new int[] { 1, 0 }))
            .add(ImmutableList.of(new int[] { 0, 0 }, new int[] { 1, 0 }, new int[] { 0, 1 }))
            .add(ImmutableList.of(new int[] { 0, 0 }, new int[] { 1, 0 }, new int[] { 0, 1 }, new int[] { 1, 1 }))
            .add(ImmutableList.of(new int[] { 0, 0 }, new int[] { 1, 0 }, new int[] { 0, 1 }, new int[] { -1, 0 },
                    new int[] { 0, -1 }))
            .add(ImmutableList.of(new int[] { 0, 0 }, new int[] { 1, 0 }, new int[] { 0, 1 }, new int[] { -1, 0 },
                    new int[] { 0, -1 }, new int[] { 1, 1 }))
            .add(ImmutableList.of(new int[] { 0, 0 }, new int[] { 1, 0 }, new int[] { 0, 1 }, new int[] { -1, 0 },
                    new int[] { 0, -1 }, new int[] { 1, 1 }, new int[] { -1, -1 }))
            .add(ImmutableList.of(new int[] { 0, 0 }, new int[] { 1, 0 }, new int[] { 0, 1 }, new int[] { -1, 0 },
                    new int[] { 0, -1 }, new int[] { 1, 1 }, new int[] { -1, -1 }, new int[] { 1, -1 }))
            .add(ImmutableList.of(new int[] { 0, 0 }, new int[] { 1, 0 }, new int[] { 0, 1 }, new int[] { -1, 0 },
                    new int[] { 0, -1 }, new int[] { 1, 1 }, new int[] { -1, -1 }, new int[] { 1, -1 },
                    new int[] { -1, 1 }))
            .build();

    private ServerEventHandler() {
    }

    public static void onServerStarting(MinecraftServer server) {
        LOGGER.info("[ServerEventHandler] Server starting - loading config from centralized location");

        ConfigUtil.loadDefaultConfig();

        if (ChunkByChunkConfig.get().getGeneration().isEnabled()) {
            LOGGER.info("[ServerEventHandler] Setting up sky dimensions");
            applySkyDimensionConfig(server.registryAccess());
            applyChunkByChunkWorldGeneration(server);
        }
    }

    public static void applySkyDimensionConfig(RegistryAccess registryAccess) {
        if (ChunkByChunkConfig.get().getGeneration().isSynchNether()) {
            SkyDimensions.getSkyDimensions().values().stream()
                    .filter(x -> "minecraft:the_nether".equals(x.dimensionId) || "the_nether".equals(x.dimensionId))
                    .forEach(x -> {
                        if (!x.synchToDimensions.contains("minecraft:overworld")) {
                            x.synchToDimensions.add("minecraft:overworld");
                        }
                    });
            SkyDimensions.getSkyDimensions().values().stream()
                    .filter(x -> "minecraft:overworld".equals(x.dimensionId) || "overworld".equals(x.dimensionId))
                    .forEach(x -> {
                        if (!x.synchToDimensions.contains("minecraft:the_nether")) {
                            x.synchToDimensions.add("minecraft:the_nether");
                        }
                    });
        } else {
            SkyDimensions.getSkyDimensions().values().stream()
                    .filter(x -> "minecraft:the_nether".equals(x.dimensionId) || "the_nether".equals(x.dimensionId))
                    .forEach(x -> {
                        x.synchToDimensions.remove("minecraft:overworld");
                        x.synchToDimensions.remove("overworld");
                    });
            SkyDimensions.getSkyDimensions().values().stream()
                    .filter(x -> "minecraft:overworld".equals(x.dimensionId) || "overworld".equals(x.dimensionId))
                    .forEach(x -> {
                        x.synchToDimensions.remove("minecraft:the_nether");
                        x.synchToDimensions.remove("the_nether");
                    });
        }
        if (ChunkByChunkConfig.get().getGeneration().sealWorld()) {
            SkyDimensions.getSkyDimensions().values().stream()
                    .filter(x -> "minecraft:overworld".equals(x.dimensionId) || "overworld".equals(x.dimensionId))
                    .forEach(x -> {
                        x.generationType = SkyChunkGenerator.EmptyGenerationType.Sealed;
                    });
        }
        if (ChunkByChunkConfig.get().getGeneration().getInitialChunks() != 1) {
            SkyDimensions.getSkyDimensions().values().stream()
                    .filter(x -> "minecraft:overworld".equals(x.dimensionId) || "overworld".equals(x.dimensionId))
                    .forEach(x -> {
                        x.initialChunks = ChunkByChunkConfig.get().getGeneration().getInitialChunks();
                    });
        }
    }

    public static void applyChunkByChunkWorldGeneration(MinecraftServer server) {
        MappedRegistry<LevelStem> dimensions = (MappedRegistry<LevelStem>) server.registryAccess()
                .registryOrThrow(Registries.LEVEL_STEM);
        MappedRegistry<Biome> biomeRegistry = (MappedRegistry<Biome>) server.registryAccess()
                .registryOrThrow(Registries.BIOME);
        Registry<DimensionType> dimensionTypeRegistry = server.registryAccess()
                .registryOrThrow(Registries.DIMENSION_TYPE);
        Registry<Block> blocks = server.registryAccess().registryOrThrow(Registries.BLOCK);

        try {
            Services.PLATFORM.unfreezeRegistry(dimensions);
            Services.PLATFORM.unfreezeRegistry(biomeRegistry);
        } catch (Exception e) {
            GatheringChunksConstants.LOGGER.error("Failed to unfreeze registries", e);
            return;
        }

        for (Map.Entry<ResourceLocation, SkyDimensionData> entry : SkyDimensions.getSkyDimensions().entrySet()) {
            setupDimension(entry.getKey(), entry.getValue(), dimensions, blocks, biomeRegistry, dimensionTypeRegistry);
        }
        configureDimensionSynching(dimensions);
    }

    private static void configureDimensionSynching(MappedRegistry<LevelStem> dimensions) {
        for (SkyDimensionData config : SkyDimensions.getSkyDimensions().values()) {
            if (!config.enabled) {
                continue;
            }
            LevelStem dimension = dimensions.get(ResourceLocation.parse(config.dimensionId));
            if (dimension.generator() instanceof SkyChunkGenerator generator) {
                for (String synchDimId : config.synchToDimensions) {
                    generator.addSynchLevel(
                            ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(synchDimId)));
                }
            } else {
                GatheringChunksConstants.LOGGER
                        .warn("Cannot setup synch for dimension {}, as it is not a sky dimension", config.dimensionId);
            }
        }
    }

    private static void setupDimension(ResourceLocation skyDimensionId, SkyDimensionData config,
            MappedRegistry<LevelStem> dimensions, Registry<Block> blocks, WritableRegistry<Biome> biomeRegistry,
            Registry<DimensionType> dimensionTypeRegistry) {
        if (!config.validate(skyDimensionId, dimensions)) {
            config.enabled = false;
        }
        if (!config.enabled) {
            return;
        }
        GatheringChunksConstants.LOGGER.info("Setting up sky dimension for {}", config.dimensionId);
        LevelStem level = dimensions.get(ResourceLocation.parse(config.dimensionId));
        ChunkGenerator rootGenerator;
        if (level.generator() instanceof SkyChunkGenerator skyChunkGenerator) {
            rootGenerator = skyChunkGenerator.getParent();
        } else {
            rootGenerator = level.generator();
        }
        SkyChunkGenerator generator = setupCoreGenerationDimension(config, dimensions, blocks, biomeRegistry, level,
                rootGenerator);
        Holder<DimensionType> themeDimensionType = level.type();
        if (config.biomeThemeDimensionType != null && !config.biomeThemeDimensionType.isEmpty()) {
            Optional<Holder.Reference<DimensionType>> holder = dimensionTypeRegistry.getHolder(ResourceKey
                    .create(Registries.DIMENSION_TYPE, ResourceLocation.parse(config.biomeThemeDimensionType)));
            if (holder.isPresent()) {
                themeDimensionType = holder.get();
            }
        }
        for (Map.Entry<String, List<String>> biomeTheme : config.biomeThemes.entrySet()) {
            ResourceKey<Level> biomeDim = setupThemeDimension(config.dimensionId, biomeTheme.getKey(),
                    biomeTheme.getValue(), level, dimensions, rootGenerator, biomeRegistry, themeDimensionType);
            if (biomeDim != null) {
                generator.addBiomeDimension(biomeTheme.getKey(), biomeDim);
            }
        }
    }

    private static SkyChunkGenerator setupCoreGenerationDimension(SkyDimensionData config,
            MappedRegistry<LevelStem> dimensions, Registry<Block> blocks, Registry<Biome> biomes, LevelStem level,
            ChunkGenerator rootGenerator) {
        ResourceLocation genDimensionId = config.getGenDimensionId();
        ResourceKey<LevelStem> genLevelId = ResourceKey.create(Registries.LEVEL_STEM, genDimensionId);
        LevelStem generationLevel = dimensions.get(genDimensionId);
        if (generationLevel == null) {
            generationLevel = new LevelStem(level.type(), rootGenerator);
            dimensions.register(genLevelId, generationLevel, RegistrationInfo.BUILT_IN);
            GatheringChunksConstants.LOGGER.info("Created generation dimension: {}", genDimensionId);
        }
        SkyChunkGenerator skyGenerator;
        ResourceKey<LevelStem> mainLevelKey = ResourceKey.create(Registries.LEVEL_STEM,
                ResourceLocation.parse(config.dimensionId));
        LevelStem currentLevel = dimensions.get(mainLevelKey);
        if (currentLevel.generator() instanceof SkyChunkGenerator existing) {
            skyGenerator = existing;
            GatheringChunksConstants.LOGGER.info("Sky dimension already configured for {}", config.dimensionId);
        } else {
            skyGenerator = new SkyChunkGenerator(rootGenerator);
            LevelStem newLevelStem = new LevelStem(currentLevel.type(), skyGenerator);
            Optional<Holder.Reference<LevelStem>> holderOpt = dimensions.getHolder(mainLevelKey);
            if (holderOpt.isPresent()) {
                Holder.Reference<LevelStem> holder = holderOpt.get();
                if (holder instanceof IHolderReference accessor) {
                    @SuppressWarnings("unchecked")
                    IHolderReference<LevelStem> typed = (IHolderReference<LevelStem>) accessor;
                    typed.gc$setValue(newLevelStem);
                    GatheringChunksConstants.LOGGER.info("Updated existing dimension via accessor: {}",
                            config.dimensionId);
                } else {
                    GatheringChunksConstants.LOGGER.warn("Could not update dimension {} - holder is not an accessor",
                            config.dimensionId);
                }
            } else {
                dimensions.register(mainLevelKey, newLevelStem, RegistrationInfo.BUILT_IN);
                GatheringChunksConstants.LOGGER.info("Registered new dimension: {}", config.dimensionId);
            }
        }
        Block sealBlock = blocks.get(ResourceLocation.parse(config.sealBlock));
        if (sealBlock == null) {
            sealBlock = Blocks.BEDROCK;
        }
        Block coverBlock = blocks.get(ResourceLocation.parse(config.sealCoverBlock));
        if (config.unspawnedBiome != null && !config.unspawnedBiome.isEmpty()) {
            biomes.getHolder(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(config.unspawnedBiome)))
                    .ifPresent(skyGenerator::setUnspawnedBiome);
        }
        skyGenerator.configure(ResourceKey.create(Registries.DIMENSION, genLevelId.location()), config.generationType,
                sealBlock, coverBlock, config.initialChunks, config.allowChunkSpawner,
                config.allowUnstableChunkSpawner);
        return skyGenerator;
    }

    private static ResourceKey<Level> setupThemeDimension(String dimId, String themeName, List<String> biomes,
            LevelStem sourceLevel, MappedRegistry<LevelStem> dimensions, ChunkGenerator rootGenerator,
            WritableRegistry<Biome> biomeRegistry, Holder<DimensionType> themeDimensionType) {
        ResourceLocation biomeDimId = ResourceLocation.parse(dimId + "_" + themeName + "_gen");
        ResourceKey<LevelStem> levelKey = ResourceKey.create(Registries.LEVEL_STEM, biomeDimId);
        if (dimensions.containsKey(levelKey)) {
            GatheringChunksConstants.LOGGER.info("Theme dimension {} already exists, skipping registration",
                    biomeDimId);
            return ResourceKey.create(Registries.DIMENSION, biomeDimId);
        }
        List<ResourceKey<Biome>> biomeKeys = biomes.stream()
                .map(x -> ResourceKey.create(Registries.BIOME, ResourceLocation.parse(x))).filter(key -> {
                    boolean valid = biomeRegistry.containsKey(key);
                    if (!valid) {
                        GatheringChunksConstants.LOGGER.warn("Could not resolve biome {} for {}", key, dimId);
                    }
                    return valid;
                }).toList();

        if (biomeKeys.isEmpty()) {
            GatheringChunksConstants.LOGGER.warn("No valid biomes found for theme {}, skipping", themeName);
            return null;
        }

        BiomeSource source = null;
        if (!(rootGenerator instanceof NoiseBasedChunkGenerator)) {
            GatheringChunksConstants.LOGGER
                    .warn("Root generator is not NoiseBasedChunkGenerator for theme {}, skipping", themeName);
            return null;
        }

        if (biomeKeys.size() == 1) {
            source = new FixedBiomeSource(biomeRegistry.getHolderOrThrow(biomeKeys.get(0)));
        } else {
            ImmutableList.Builder<Pair<Climate.ParameterPoint, Holder<Biome>>> builder = ImmutableList.builder();
            NoiseBasedChunkGenerator noiseGen = (NoiseBasedChunkGenerator) rootGenerator;
            BiomeSource biomeSource = noiseGen.getBiomeSource();

            if (biomeSource instanceof IMultiNoiseBiomeSource accessor) {
                accessor.gc$getParameters().left().ifPresent(params -> {
                    params.values().forEach(pair -> {
                        pair.getSecond().unwrapKey().ifPresent(key -> {
                            if (biomeKeys.contains(key)) {
                                builder.add(pair);
                            }
                        });
                    });
                });
            } else {
                GatheringChunksConstants.LOGGER
                        .warn("MultiNoiseBiomeSource is not accessible via mixin for theme {}", themeName);
            }

            ImmutableList<Pair<Climate.ParameterPoint, Holder<Biome>>> paramList = builder.build();
            if (paramList.isEmpty()) {
                GatheringChunksConstants.LOGGER.warn(
                        "No matching biome parameters found for theme {}, using first biome as fallback", themeName);
                source = new FixedBiomeSource(biomeRegistry.getHolderOrThrow(biomeKeys.get(0)));
            } else {
                Climate.ParameterList<Holder<Biome>> parameterList = new Climate.ParameterList<>(paramList);
                source = MultiNoiseBiomeSource.createFromList(parameterList);
            }
        }

        if (source == null) {
            GatheringChunksConstants.LOGGER.warn("Failed to create biome source for theme {}", themeName);
            return null;
        }

        LevelStem biomeLevel = new LevelStem(themeDimensionType,
                new NoiseBasedChunkGenerator(source, ChunkGeneratorAccess.getNoiseGeneratorSettings(rootGenerator)));
        dimensions.register(levelKey, biomeLevel, RegistrationInfo.BUILT_IN);
        return ResourceKey.create(Registries.DIMENSION, biomeDimId);
    }

    public static void onServerStarted(MinecraftServer server) {
        WorldMigrationManager migrationManager = WorldMigrationManager.get(server);
        migrationManager.runMigrationsIfNeeded(server);

        if (ChunkByChunkConfig.get().getGeneration().isEnabled()) {
            checkSpawnInitialChunks(server);
            if (ChunkByChunkConfig.get().getGeneration().isSynchNether()) {
                server.tell(new net.minecraft.server.TickTask(server.getTickCount() + 40, () -> {
                    ChunkSpawnController.get(server).checkAndSyncExistingChunks();
                }));
            }
        }
    }

    private static void checkSpawnInitialChunks(MinecraftServer server) {
        ServerLevel overworldLevel = server.getLevel(Level.OVERWORLD);
        BlockPos overworldSpawnPos;
        if (overworldLevel != null
                && overworldLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator skyGenerator) {
            ServerLevel generationLevel = server.getLevel(skyGenerator.getGenerationLevel());
            overworldSpawnPos = generationLevel.getSharedSpawnPos();
            ChunkPos chunkSpawnPos = new ChunkPos(overworldSpawnPos);
            if (SpawnChunkHelper.isEmptyChunk(overworldLevel, chunkSpawnPos)) {
                overworldSpawnPos = findAppropriateSpawnChunk(overworldLevel, generationLevel, server.registryAccess());
                spawnInitialChunks(overworldLevel, skyGenerator.getInitialChunks(), overworldSpawnPos,
                        ChunkByChunkConfig.get().getDifficulty().spawnNewChunkChest());
            }
        } else {
            overworldSpawnPos = overworldLevel.getSharedSpawnPos();
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level != overworldLevel
                    && level.getChunkSource().getGenerator() instanceof SkyChunkGenerator levelGenerator) {
                if (levelGenerator.getInitialChunks() > 0) {
                    spawnInitialChunks(level, levelGenerator.getInitialChunks(), overworldSpawnPos, false);
                }
            }
        }
    }

    private static BlockPos findAppropriateSpawnChunk(ServerLevel overworldLevel, ServerLevel generationLevel,
            RegistryAccess registryAccess) {
        final long startTime = System.currentTimeMillis();
        final int MAX_SEARCH_TIME_MS = 20000;
        final int MAX_CHUNKS_TO_CHECK = 100;
        final int BATCH_SIZE = 20;
        final int BATCH_TIMEOUT_MS = 2000;

        if (ChunkByChunkConfig.get().getDifficulty().isSpawnChunkStrip()) {
            LOGGER.info("[SpawnFinder] Spawn chunk strip mode enabled - using default spawn");
            return overworldLevel.getSharedSpawnPos();
        }

        TagKey<Block> logsTag = BlockTags.LOGS;
        TagKey<Block> leavesTag = BlockTags.LEAVES;
        Set<Block> copper = ImmutableSet.of(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK);
        BlockPos spawnPos = overworldLevel.getSharedSpawnPos();
        boolean disableVillage = ChunkByChunkConfig.get().getDifficulty().getHardMode().isDisableVillages();

        if (!disableVillage) {
            if (ChunkByChunkConfig.get().getDifficulty().isAlwaysSpawnVillage()) {
                spawnPos = findVillage(generationLevel, registryAccess, spawnPos);
            } else {
                switch (ChunkByChunkConfig.get().getDifficulty().getStartRestriction()) {
                    case Village -> spawnPos = findVillage(generationLevel, registryAccess, spawnPos);
                    case Biome -> {
                        String startingBiome = ChunkByChunkConfig.get().getDifficulty().getStartingBiome();
                        spawnPos = findBiome(overworldLevel, generationLevel, registryAccess, spawnPos, startingBiome);
                    }
                }
            }
        }

        ChunkPos initialChunkPos = new ChunkPos(spawnPos);
        AtomicReference<BlockPos> foundSpawn = new AtomicReference<>(null);
        AtomicInteger checkedCount = new AtomicInteger(0);

        LOGGER.info("[SpawnFinder] Starting spawn search from chunk [{}, {}] - Max time: {}ms, Max chunks: {}",
                initialChunkPos.x, initialChunkPos.z, MAX_SEARCH_TIME_MS, MAX_CHUNKS_TO_CHECK);

        try {
            SpiralIterator iterator = new SpiralIterator(initialChunkPos.x, initialChunkPos.z);
            int totalBatches = (MAX_CHUNKS_TO_CHECK + BATCH_SIZE - 1) / BATCH_SIZE;

            outerLoop: for (int batch = 0; batch < totalBatches; batch++) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > MAX_SEARCH_TIME_MS) {
                    LOGGER.warn("[SpawnFinder] TIMEOUT after {}ms - stopping search", elapsed);
                    break;
                }

                List<ChunkPos> batchPositions = new ArrayList<>(BATCH_SIZE);
                for (int i = 0; i < BATCH_SIZE && checkedCount.get() < MAX_CHUNKS_TO_CHECK; i++) {
                    batchPositions.add(new ChunkPos(iterator.getX(), iterator.getY()));
                    iterator.next();
                }

                for (ChunkPos pos : batchPositions) {
                    checkedCount.incrementAndGet();

                    if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME_MS) {
                        LOGGER.warn("[SpawnFinder] TIMEOUT mid-batch at chunk {}", checkedCount.get());
                        break outerLoop;
                    }

                    try {
                        LevelChunk chunk = generationLevel.getChunkSource().getChunkNow(pos.x, pos.z);
                        if (chunk == null) {
                            continue;
                        }

                        if (isGoodSpawnChunk(chunk, logsTag, leavesTag, copper)) {
                            BlockPos goodSpawn = new BlockPos(
                                    pos.getMiddleBlockX(),
                                    ChunkUtil.getSafeSpawnHeight(chunk, pos.getMiddleBlockX(), pos.getMiddleBlockZ()),
                                    pos.getMiddleBlockZ());
                            foundSpawn.set(goodSpawn);
                            LOGGER.info("[SpawnFinder] Found spawn at {} after {} checks ({}ms)",
                                    pos, checkedCount.get(), System.currentTimeMillis() - startTime);
                            break outerLoop;
                        }
                    } catch (Exception e) {
                        LOGGER.debug("[SpawnFinder] Error checking {}: {}", pos, e.getMessage());
                    }
                }

                if (batch % 3 == 0 && batch > 0) {
                    LOGGER.info("[SpawnFinder] Progress: {} chunks checked ({}ms)",
                            checkedCount.get(), System.currentTimeMillis() - startTime);
                }
            }

        } catch (Exception e) {
            LOGGER.error("[SpawnFinder] Critical error during spawn search", e);
        }

        long totalTime = System.currentTimeMillis() - startTime;

        if (foundSpawn.get() != null) {
            spawnPos = foundSpawn.get();
            LOGGER.info("[SpawnFinder] SUCCESS: Found spawn in {}ms after {} chunks", totalTime, checkedCount.get());
        } else {
            LOGGER.warn("[SpawnFinder] FALLBACK: No ideal spawn found after {} chunks ({}ms) - using default",
                    checkedCount.get(), totalTime);
        }

        ServerLevelData levelData = (ServerLevelData) overworldLevel.getLevelData();
        levelData.setSpawn(spawnPos, levelData.getSpawnAngle());
        return spawnPos;
    }

    private static boolean isGoodSpawnChunk(LevelChunk chunk, TagKey<Block> logsTag, TagKey<Block> leavesTag,
            Set<Block> copper) {
        int logCount = ChunkUtil.countBlocks(chunk, logsTag);
        if (logCount <= 2)
            return false;

        int waterCount = ChunkUtil.countBlocks(chunk, Blocks.WATER);
        if (waterCount == 0)
            return false;

        int leavesCount = ChunkUtil.countBlocks(chunk, leavesTag);
        if (leavesCount <= 3)
            return false;

        int copperCount = ChunkUtil.countBlocks(chunk, copper);
        if (copperCount < 36)
            return false;

        return true;
    }

    private static BlockPos findBiome(ServerLevel overworldLevel, ServerLevel generationLevel,
            RegistryAccess registryAccess, BlockPos spawnPos, String startingBiome) {
        if (startingBiome.startsWith("#")) {
            Optional<HolderSet.Named<Biome>> tagSet = registryAccess.registry(Registries.BIOME).orElseThrow()
                    .getTag(TagKey.create(Registries.BIOME, ResourceLocation.parse(startingBiome.substring(1))));
            if (tagSet.isPresent()) {
                Pair<BlockPos, Holder<Biome>> location = generationLevel
                        .findClosestBiome3d(x -> tagSet.get().contains(x), spawnPos, 6400, 32, 64);
                if (location != null) {
                    spawnPos = location.getFirst();
                    GatheringChunksConstants.LOGGER.info("Spawn shifted to nearest biome of tag " + startingBiome);
                }
            } else {
                GatheringChunksConstants.LOGGER.warn("No biome matching '" + startingBiome + "' found");
            }
        } else {
            Biome biome = registryAccess.registry(Registries.BIOME).orElseThrow()
                    .get(ResourceLocation.parse(startingBiome));
            if (biome != null) {
                Pair<BlockPos, Holder<Biome>> location = generationLevel
                        .findClosestBiome3d(x -> x.value().equals(biome), spawnPos, 6400, 32, 64);
                if (location != null) {
                    spawnPos = location.getFirst();
                    GatheringChunksConstants.LOGGER.info("Spawn shifted to nearest biome: " + startingBiome);
                } else {
                    GatheringChunksConstants.LOGGER.warn("No biome matching '" + startingBiome + "' found");
                }
            }
        }
        return spawnPos;
    }

    private static BlockPos findVillage(ServerLevel generationLevel, RegistryAccess registryAccess, BlockPos spawnPos) {
        Registry<Structure> structures = registryAccess.registryOrThrow(Registries.STRUCTURE);
        Optional<HolderSet.Named<Structure>> structuresTag = structures.getTag(StructureTags.VILLAGE);
        if (structuresTag.isPresent()) {
            HolderSet<Structure> holders = structuresTag.get();
            Pair<BlockPos, Holder<Structure>> nearest = generationLevel.getChunkSource().getGenerator()
                    .findNearestMapStructure(generationLevel, holders, spawnPos, 100, false);
            if (nearest != null) {
                spawnPos = nearest.getFirst();
                GatheringChunksConstants.LOGGER.info("Spawn shifted to nearest village");
            }
        } else {
            GatheringChunksConstants.LOGGER.warn("Could not find village spawn");
        }
        return spawnPos;
    }

    private static void spawnInitialChunks(ServerLevel level, int initialChunks, BlockPos overworldSpawn,
            boolean spawnChest) {
        ChunkSpawnController chunkSpawnController = ChunkSpawnController.get(level.getServer());
        BlockPos scaledSpawn = new BlockPos(
                Mth.floor(overworldSpawn.getX() / level.dimensionType().coordinateScale()),
                overworldSpawn.getY(),
                Mth.floor(overworldSpawn.getZ() / level.dimensionType().coordinateScale()));
        ChunkPos centerChunkPos = new ChunkPos(scaledSpawn);

        LOGGER.info("[InitialSpawn] Queuing {} chunks for async spawn at [{}, {}]",
                initialChunks, centerChunkPos.x, centerChunkPos.z);
        long startTime = System.currentTimeMillis();

        List<ChunkPos> queuedChunks = new ArrayList<>();

        if (initialChunks > 0 && initialChunks <= CHUNK_SPAWN_OFFSETS.size()) {
            List<int[]> chunkOffsets = CHUNK_SPAWN_OFFSETS.get(initialChunks - 1);
            for (int[] offset : chunkOffsets) {
                ChunkPos targetPos = new ChunkPos(centerChunkPos.x + offset[0], centerChunkPos.z + offset[1]);
                boolean isInitial = (offset[0] == 0 && offset[1] == 0);

                if (chunkSpawnController.request(level, "", false, targetPos.getMiddleBlockPosition(0),
                        false, false, isInitial)) {
                    queuedChunks.add(targetPos);
                    LOGGER.info("[InitialSpawn] Queued chunk {} (async)", targetPos);

                    if (spawnChest && isInitial) {
                        SpawnChunkHelper.createNextSpawner(level, targetPos);
                    }
                } else {
                    LOGGER.warn("[InitialSpawn] Failed to queue chunk {}", targetPos);
                }
            }
        } else {
            SpiralIterator spiralIterator = new SpiralIterator(centerChunkPos.x, centerChunkPos.z);
            for (int i = 0; i < initialChunks; i++) {
                ChunkPos targetPos = new ChunkPos(spiralIterator.getX(), spiralIterator.getY());
                boolean isInitial = (i == 0);

                if (chunkSpawnController.request(level, "", false, targetPos.getMiddleBlockPosition(0),
                        false, false, isInitial)) {
                    queuedChunks.add(targetPos);
                    LOGGER.info("[InitialSpawn] Queued chunk {} (async)", targetPos);

                    if (spawnChest && isInitial) {
                        SpawnChunkHelper.createNextSpawner(level, targetPos);
                    }
                } else {
                    LOGGER.warn("[InitialSpawn] Failed to queue chunk {}", targetPos);
                }

                spiralIterator.next();
            }
        }

        long queueTime = System.currentTimeMillis() - startTime;
        LOGGER.info("[InitialSpawn] Queued {} chunks in {}ms - spawning will complete asynchronously",
                queuedChunks.size(), queueTime);

        LOGGER.info("[InitialSpawn] Server can now continue - chunks spawning in background");
    }

    private static void spawnChunkWithTimeout(ChunkSpawnController controller, ServerLevel level,
            ChunkPos targetPos, boolean isInitial, boolean addSpawner,
            List<ChunkPos> spawnedChunks) {
        if (controller.request(level, "", false, targetPos.getMiddleBlockPosition(0), true, isInitial, isInitial)) {
            spawnedChunks.add(targetPos);
            LOGGER.info("[InitialSpawn]Queued chunk {} for spawning", targetPos);

            if (addSpawner) {
                SpawnChunkHelper.createNextSpawner(level, targetPos);
            }
        } else {
            LOGGER.warn("[InitialSpawn] Failed to queue chunk {} - skipping", targetPos);
        }
    }

    public static void onResourceManagerReload(ResourceManager resourceManager) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(SkyChunkGenerator.EmptyGenerationType.class,
                        (JsonDeserializer<SkyChunkGenerator.EmptyGenerationType>) (json, typeOfT,
                                context) -> SkyChunkGenerator.EmptyGenerationType.getFromString(json.getAsString()))
                .create();
        loadScannerData(resourceManager, gson);
        SkyDimensions.loadSkyDimensionData(resourceManager, gson);
    }

    private static void loadScannerData(ResourceManager resourceManager, Gson gson) {
        WorldScannerBlockEntity.clearItemMappings();
        int count = 0;
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                GatheringChunksConstants.SCANNER_DATA_PATH,
                r -> !r.getPath().isEmpty() && !GatheringChunksConstants.SCANNER_DATA_PATH.equals(r.getPath()));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            Resource resource = entry.getValue();
            try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                ScannerData data = gson.fromJson(reader, ScannerData.class);
                data.process(location, null);
                count++;
            } catch (IOException | RuntimeException e) {
                GatheringChunksConstants.LOGGER.error("Failed to read scanner data '{}'", location, e);
            }
        }
        GatheringChunksConstants.LOGGER.info("Loaded {} scanner data configs", count);
    }

    public static void onLevelTick(MinecraftServer server) {
        ChunkSpawnController chunkSpawnController = ChunkSpawnController.get(server);
        if (chunkSpawnController != null) {
            chunkSpawnController.tick();
        }

        BiomeCoordinateCache.get(server).tick();

        ChunkEngineManager.get(server).tick();

        if (server.getTickCount() % 10 == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                for (ServerPlayer player : level.players()) {
                    ChunkBoundaryEnforcer.checkPlayerBoundaries(player);
                }
            }
        }

        if (server.getTickCount() % 100 == 0) {
            ChestTracker tracker = ChestTracker.get(server);
            Set<BlockPos> chests = new java.util.HashSet<>(tracker.getChestPositions());
            for (BlockPos pos : chests) {
                for (ServerLevel level : server.getAllLevels()) {
                    if (level.isLoaded(pos)) {
                        tracker.checkAndRemoveIfEmpty(pos, level);
                        break;
                    }
                }
            }
        }

        if (server.getTickCount() % 200 == 0 && ChunkByChunkConfig.get().getDifficulty().isEnableProgressionHelper()) {
            PlayerProgressionHelper.checkPlayers(server);
        }
    }

    public static void onPlayerChangedDimension(ServerPlayer player, ResourceKey<Level> fromLevel, ResourceKey<Level> toLevel) {
        ServerLevel targetLevel = player.server.getLevel(toLevel);
        if (targetLevel == null) return;
        checkAndRedirectPlayerToNearestChunk(player, targetLevel);
    }

    public static void onPlayerArrived(ServerPlayer player, ServerLevel targetLevel) {
        checkAndRedirectPlayerToNearestChunk(player, targetLevel);
    }

    private static void checkAndRedirectPlayerToNearestChunk(ServerPlayer player, ServerLevel targetLevel) {
        if (!ChunkByChunkConfig.get().getGeneration().isEnabled()) {
            return;
        }

        if (!(targetLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator)) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        ChunkPos playerChunk = new ChunkPos(playerPos);

        if (!SpawnChunkHelper.isEmptyChunk(targetLevel, playerChunk)) {
            return;
        }

        LOGGER.info("[PortalFix] Player {} arrived in empty chunk [{},{}] in {}, finding nearest spawned chunk",
                player.getName().getString(), playerChunk.x, playerChunk.z, targetLevel.dimension().location());

        ChunkPos nearest = findNearestSpawnedChunk(targetLevel, playerChunk, 64);
        if (nearest == null) {
            BlockPos spawn = targetLevel.getSharedSpawnPos();
            nearest = new ChunkPos(spawn);
            LOGGER.info("[PortalFix] No nearby spawned chunk found, using spawn chunk [{},{}]", nearest.x, nearest.z);
        } else {
            LOGGER.info("[PortalFix] Found nearest spawned chunk [{},{}]", nearest.x, nearest.z);
        }

        LevelChunk targetChunk = targetLevel.getChunk(nearest.x, nearest.z);
        int safeY = ChunkUtil.getSafeSpawnHeight(targetChunk, nearest.getMiddleBlockX(), nearest.getMiddleBlockZ());

        player.teleportTo(
                targetLevel,
                nearest.getMiddleBlockX() + 0.5,
                safeY,
                nearest.getMiddleBlockZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
    }

    private static ChunkPos findNearestSpawnedChunk(ServerLevel level, ChunkPos origin, int maxRadius) {
        SpiralIterator spiral = new SpiralIterator(origin.x, origin.z);
        int checked = 0;
        int maxChecks = (2 * maxRadius + 1) * (2 * maxRadius + 1);

        while (checked < maxChecks) {
            int cx = spiral.getX();
            int cz = spiral.getY();

            if (Math.abs(cx - origin.x) > maxRadius || Math.abs(cz - origin.z) > maxRadius) {
                spiral.next();
                checked++;
                continue;
            }

            ChunkPos candidate = new ChunkPos(cx, cz);
            if (!SpawnChunkHelper.isEmptyChunk(level, candidate)) {
                return candidate;
            }

            spiral.next();
            checked++;
        }

        return null;
    }
}