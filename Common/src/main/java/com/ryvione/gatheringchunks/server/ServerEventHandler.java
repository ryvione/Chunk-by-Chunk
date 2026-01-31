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
import com.ryvione.gatheringchunks.common.util.SpiralIterator;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.config.system.ConfigSystem;
import com.ryvione.gatheringchunks.interop.Services;
import com.ryvione.gatheringchunks.mixins.HolderReferenceAccessor;
import com.ryvione.gatheringchunks.server.world.*;
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
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ServerEventHandler {
    private static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    private static final int MAX_FIND_CHUNK_ATTEMPTS = 512;
    private static final String SERVERCONFIG = "serverconfig";
    private static final ConfigSystem configSystem = new ConfigSystem();
    private static final List<List<int[]>> CHUNK_SPAWN_OFFSETS = ImmutableList.<List<int[]>>builder()
            .add(ImmutableList.of(new int[]{0, 0}))
            .add(ImmutableList.of(new int[]{0, 0}, new int[]{1, 0}))
            .add(ImmutableList.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}))
            .add(ImmutableList.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{1, 1}))
            .add(ImmutableList.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}))
            .add(ImmutableList.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 1}))
            .add(ImmutableList.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 1}, new int[]{-1, -1}))
            .add(ImmutableList.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 1}, new int[]{-1, -1}, new int[]{1, -1}))
            .add(ImmutableList.of(new int[]{0, 0}, new int[]{1, 0}, new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 1}, new int[]{-1, -1}, new int[]{1, -1}, new int[]{-1, 1}))
            .build();

    private ServerEventHandler() {
    }

    public static void onServerStarting(MinecraftServer server) {
        configSystem.synchConfig(server.getWorldPath(LevelResource.ROOT).resolve(SERVERCONFIG).resolve(GatheringChunksConstants.CONFIG_FILE), Paths.get(GatheringChunksConstants.DEFAULT_CONFIG_PATH).resolve(GatheringChunksConstants.CONFIG_FILE), ChunkByChunkConfig.get());
        if (ChunkByChunkConfig.get().getGeneration().isEnabled()) {
            GatheringChunksConstants.LOGGER.info("Setting up sky dimensions");
            applySkyDimensionConfig(server.registryAccess());
            applyChunkByChunkWorldGeneration(server);
        }
    }

    public static void applySkyDimensionConfig(RegistryAccess registryAccess) {
        if (ChunkByChunkConfig.get().getGeneration().isSynchNether()) {
            SkyDimensions.getSkyDimensions().values().stream().filter(x -> "minecraft:the_nether".equals(x.dimensionId) || "the_nether".equals(x.dimensionId)).forEach(x -> {
                x.synchToDimensions.add("minecraft:overworld");
            });
        }
        if (ChunkByChunkConfig.get().getGeneration().sealWorld()) {
            SkyDimensions.getSkyDimensions().values().stream().filter(x -> "minecraft:overworld".equals(x.dimensionId) || "overworld".equals(x.dimensionId)).forEach(x -> {
                x.generationType = SkyChunkGenerator.EmptyGenerationType.Sealed;
            });
        }
        if (ChunkByChunkConfig.get().getGeneration().getInitialChunks() != 1) {
            SkyDimensions.getSkyDimensions().values().stream().filter(x -> "minecraft:overworld".equals(x.dimensionId) || "overworld".equals(x.dimensionId)).forEach(x -> {
                x.initialChunks = ChunkByChunkConfig.get().getGeneration().getInitialChunks();
            });
        }
    }

    public static void applyChunkByChunkWorldGeneration(MinecraftServer server) {
        MappedRegistry<LevelStem> dimensions = (MappedRegistry<LevelStem>) server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        MappedRegistry<Biome> biomeRegistry = (MappedRegistry<Biome>) server.registryAccess().registryOrThrow(Registries.BIOME);
        Registry<DimensionType> dimensionTypeRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
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
            for (String synchDimId : config.synchToDimensions) {
                LevelStem synchDim = dimensions.get(ResourceLocation.parse(synchDimId));
                if (DimensionType.getTeleportationScale(synchDim.type().value(), dimension.type().value()) > 1) {
                    GatheringChunksConstants.LOGGER.warn("Cowardly refusing to synch dimension {} with {}, as the coordinate scale would result in a performance issues", config.dimensionId, synchDimId);
                    continue;
                }
                if (synchDim.generator() instanceof SkyChunkGenerator generator) {
                    generator.addSynchLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(config.dimensionId)));
                } else {
                    GatheringChunksConstants.LOGGER.warn("Cannot synch dimension {} with {}, as it is not a sky dimension", config.dimensionId, synchDimId);
                }
            }
        }
    }

    private static void setupDimension(ResourceLocation skyDimensionId, SkyDimensionData config, MappedRegistry<LevelStem> dimensions, Registry<Block> blocks, WritableRegistry<Biome> biomeRegistry, Registry<DimensionType> dimensionTypeRegistry) {
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
        SkyChunkGenerator generator = setupCoreGenerationDimension(config, dimensions, blocks, biomeRegistry, level, rootGenerator);
        Holder<DimensionType> themeDimensionType = level.type();
        if (config.biomeThemeDimensionType != null && !config.biomeThemeDimensionType.isEmpty()) {
            Optional<Holder.Reference<DimensionType>> holder = dimensionTypeRegistry.getHolder(ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.parse(config.biomeThemeDimensionType)));
            if (holder.isPresent()) {
                themeDimensionType = holder.get();
            }
        }
        for (Map.Entry<String, List<String>> biomeTheme : config.biomeThemes.entrySet()) {
            ResourceKey<Level> biomeDim = setupThemeDimension(config.dimensionId, biomeTheme.getKey(), biomeTheme.getValue(), level, dimensions, rootGenerator, biomeRegistry, themeDimensionType);
            if (biomeDim != null) {
                generator.addBiomeDimension(biomeTheme.getKey(), biomeDim);
            }
        }
    }

    private static SkyChunkGenerator setupCoreGenerationDimension(SkyDimensionData config, MappedRegistry<LevelStem> dimensions, Registry<Block> blocks, Registry<Biome> biomes, LevelStem level, ChunkGenerator rootGenerator) {
        ResourceLocation genDimensionId = config.getGenDimensionId();
        ResourceKey<LevelStem> genLevelId = ResourceKey.create(Registries.LEVEL_STEM, genDimensionId);
        LevelStem generationLevel = dimensions.get(genDimensionId);
        if (generationLevel == null) {
            generationLevel = new LevelStem(level.type(), rootGenerator);
            dimensions.register(genLevelId, generationLevel, RegistrationInfo.BUILT_IN);
            GatheringChunksConstants.LOGGER.info("Created generation dimension: {}", genDimensionId);
        }
        SkyChunkGenerator skyGenerator;
        ResourceKey<LevelStem> mainLevelKey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.parse(config.dimensionId));
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
                if (holder instanceof HolderReferenceAccessor accessor) {
                    @SuppressWarnings("unchecked")
                    HolderReferenceAccessor<LevelStem> typed = (HolderReferenceAccessor<LevelStem>) accessor;
                    typed.setValue(newLevelStem);
                    GatheringChunksConstants.LOGGER.info("Updated existing dimension via accessor: {}", config.dimensionId);
                } else {
                    GatheringChunksConstants.LOGGER.warn("Could not update dimension {} - holder is not an accessor", config.dimensionId);
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
            biomes.getHolder(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(config.unspawnedBiome))).ifPresent(skyGenerator::setUnspawnedBiome);
        }
        skyGenerator.configure(ResourceKey.create(Registries.DIMENSION, genLevelId.location()), config.generationType, sealBlock, coverBlock, config.initialChunks, config.allowChunkSpawner, config.allowUnstableChunkSpawner);
        return skyGenerator;
    }

    private static ResourceKey<Level> setupThemeDimension(String dimId, String themeName, List<String> biomes, LevelStem sourceLevel, MappedRegistry<LevelStem> dimensions, ChunkGenerator rootGenerator, WritableRegistry<Biome> biomeRegistry, Holder<DimensionType> themeDimensionType) {
        ResourceLocation biomeDimId = ResourceLocation.parse(dimId + "_" + themeName + "_gen");
        ResourceKey<LevelStem> levelKey = ResourceKey.create(Registries.LEVEL_STEM, biomeDimId);
        if (dimensions.containsKey(levelKey)) {
            GatheringChunksConstants.LOGGER.info("Theme dimension {} already exists, skipping registration", biomeDimId);
            return ResourceKey.create(Registries.DIMENSION, biomeDimId);
        }
        List<ResourceKey<Biome>> biomeKeys = biomes.stream().map(x -> ResourceKey.create(Registries.BIOME, ResourceLocation.parse(x))).filter(key -> {
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
            GatheringChunksConstants.LOGGER.warn("Root generator is not NoiseBasedChunkGenerator for theme {}, skipping", themeName);
            return null;
        }

        if (biomeKeys.size() == 1) {
            source = new FixedBiomeSource(biomeRegistry.getHolderOrThrow(biomeKeys.get(0)));
        } else {
            ImmutableList.Builder<Pair<Climate.ParameterPoint, Holder<Biome>>> builder = ImmutableList.builder();
            NoiseBasedChunkGenerator noiseGen = (NoiseBasedChunkGenerator) rootGenerator;
            BiomeSource biomeSource = noiseGen.getBiomeSource();

            if (biomeSource instanceof MultiNoiseBiomeSource multiNoise) {
                if (multiNoise instanceof com.ryvione.gatheringchunks.mixins.MultiNoiseBiomeSourceAccessor accessor) {
                    accessor.getParameters().left().ifPresent(params -> {
                        params.values().forEach(pair -> {
                            pair.getSecond().unwrapKey().ifPresent(key -> {
                                if (biomeKeys.contains(key)) {
                                    builder.add(pair);
                                }
                            });
                        });
                    });
                } else {
                    GatheringChunksConstants.LOGGER.warn("MultiNoiseBiomeSource is not accessible via mixin for theme {}", themeName);
                }
            }

            ImmutableList<Pair<Climate.ParameterPoint, Holder<Biome>>> paramList = builder.build();
            if (paramList.isEmpty()) {
                GatheringChunksConstants.LOGGER.warn("No matching biome parameters found for theme {}, using first biome as fallback", themeName);
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

        LevelStem biomeLevel = new LevelStem(themeDimensionType, new NoiseBasedChunkGenerator(source, ChunkGeneratorAccess.getNoiseGeneratorSettings(rootGenerator)));
        dimensions.register(levelKey, biomeLevel, RegistrationInfo.BUILT_IN);
        return ResourceKey.create(Registries.DIMENSION, biomeDimId);
    }

    public static void onServerStarted(MinecraftServer server) {
        if (ChunkByChunkConfig.get().getGeneration().isEnabled()) {
            checkSpawnInitialChunks(server);
        }
    }

    private static void checkSpawnInitialChunks(MinecraftServer server) {
        ServerLevel overworldLevel = server.getLevel(Level.OVERWORLD);
        BlockPos overworldSpawnPos;
        if (overworldLevel != null && overworldLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator skyGenerator) {
            ServerLevel generationLevel = server.getLevel(skyGenerator.getGenerationLevel());
            overworldSpawnPos = generationLevel.getSharedSpawnPos();
            ChunkPos chunkSpawnPos = new ChunkPos(overworldSpawnPos);
            if (SpawnChunkHelper.isEmptyChunk(overworldLevel, chunkSpawnPos)) {
                overworldSpawnPos = findAppropriateSpawnChunk(overworldLevel, generationLevel, server.registryAccess());
                spawnInitialChunks(overworldLevel, skyGenerator.getInitialChunks(), overworldSpawnPos, ChunkByChunkConfig.get().getGeneration().spawnNewChunkChest());
            }
        } else {
            overworldSpawnPos = overworldLevel.getSharedSpawnPos();
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level != overworldLevel && level.getChunkSource().getGenerator() instanceof SkyChunkGenerator levelGenerator) {
                if (levelGenerator.getInitialChunks() > 0) {
                    spawnInitialChunks(level, levelGenerator.getInitialChunks(), overworldSpawnPos, false);
                }
            }
        }
    }

    private static BlockPos findAppropriateSpawnChunk(ServerLevel overworldLevel, ServerLevel generationLevel, RegistryAccess registryAccess) {
        if (ChunkByChunkConfig.get().getGeneration().isSpawnChunkStrip()) {
            return overworldLevel.getSharedSpawnPos();
        }
        TagKey<Block> logsTag = BlockTags.LOGS;
        TagKey<Block> leavesTag = BlockTags.LEAVES;
        Set<Block> copper = ImmutableSet.of(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK);
        BlockPos spawnPos = overworldLevel.getSharedSpawnPos();
        if (!ChunkByChunkConfig.get().getGatheringChunksConfig().isHardMode()) {
            switch (ChunkByChunkConfig.get().getGameplayConfig().getStartRestriction()) {
                case Village -> {
                    spawnPos = findVillage(generationLevel, registryAccess, spawnPos);
                }
                case Biome -> {
                    String startingBiome = ChunkByChunkConfig.get().getGameplayConfig().getStartingBiome();
                    spawnPos = findBiome(overworldLevel, generationLevel, registryAccess, spawnPos, startingBiome);
                }
            }
        } else {
            LOGGER.info("Hard Mode enabled - skipping village spawning, using natural chunk selection");
        }
        ChunkPos initialChunkPos = new ChunkPos(spawnPos);
        SpiralIterator iterator = new SpiralIterator(initialChunkPos.x, initialChunkPos.z);
        int attempts = 0;
        while (attempts < MAX_FIND_CHUNK_ATTEMPTS) {
            LevelChunk chunk = generationLevel.getChunk(iterator.getX(), iterator.getY());
            if (ChunkUtil.countBlocks(chunk, logsTag) > 2 && ChunkUtil.countBlocks(chunk, Blocks.WATER) > 0 && ChunkUtil.countBlocks(chunk, leavesTag) > 3 && ChunkUtil.countBlocks(chunk, copper) >= 36) {
                spawnPos = new BlockPos(chunk.getPos().getMiddleBlockX(), ChunkUtil.getSafeSpawnHeight(chunk, chunk.getPos().getMiddleBlockX(), chunk.getPos().getMiddleBlockZ()), chunk.getPos().getMiddleBlockZ());
                break;
            }
            iterator.next();
            attempts++;
        }
        if (attempts < MAX_FIND_CHUNK_ATTEMPTS) {
            LOGGER.info("Found appropriate spawn chunk in {} attempts", attempts);
        } else {
            LOGGER.info("No appropriate spawn chunk found :(");
        }
        ServerLevelData levelData = (ServerLevelData) overworldLevel.getLevelData();
        levelData.setSpawn(spawnPos, levelData.getSpawnAngle());
        return spawnPos;
    }

    private static BlockPos findBiome(ServerLevel overworldLevel, ServerLevel generationLevel, RegistryAccess registryAccess, BlockPos spawnPos, String startingBiome) {
        if (startingBiome.startsWith("#")) {
            Optional<HolderSet.Named<Biome>> tagSet = registryAccess.registry(Registries.BIOME).orElseThrow().getTag(TagKey.create(Registries.BIOME, ResourceLocation.parse(startingBiome.substring(1))));
            if (tagSet.isPresent()) {
                Pair<BlockPos, Holder<Biome>> location = generationLevel.findClosestBiome3d(x -> tagSet.get().contains(x), spawnPos, 6400, 32, 64);
                if (location != null) {
                    spawnPos = location.getFirst();
                    GatheringChunksConstants.LOGGER.info("Spawn shifted to nearest biome of tag " + startingBiome);
                }
            } else {
                GatheringChunksConstants.LOGGER.warn("No biome matching '" + startingBiome + "' found");
            }
        } else {
            Biome biome = registryAccess.registry(Registries.BIOME).orElseThrow().get(ResourceLocation.parse(startingBiome));
            if (biome != null) {
                Pair<BlockPos, Holder<Biome>> location = generationLevel.findClosestBiome3d(x -> x.value().equals(biome), spawnPos, 6400, 32, 64);
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
            Pair<BlockPos, Holder<Structure>> nearest = generationLevel.getChunkSource().getGenerator().findNearestMapStructure(generationLevel, holders, spawnPos, 100, false);
            if (nearest != null) {
                spawnPos = nearest.getFirst();
                GatheringChunksConstants.LOGGER.info("Spawn shifted to nearest village");
            }
        } else {
            GatheringChunksConstants.LOGGER.warn("Could not find village spawn");
        }
        return spawnPos;
    }

    private static void spawnInitialChunks(ServerLevel level, int initialChunks, BlockPos overworldSpawn, boolean spawnChest) {
        ChunkSpawnController chunkSpawnController = ChunkSpawnController.get(level.getServer());
        BlockPos scaledSpawn = new BlockPos(Mth.floor(overworldSpawn.getX() / level.dimensionType().coordinateScale()), overworldSpawn.getY(), Mth.floor(overworldSpawn.getZ() / level.dimensionType().coordinateScale()));
        ChunkPos centerChunkPos = new ChunkPos(scaledSpawn);
        if (initialChunks > 0 && initialChunks <= CHUNK_SPAWN_OFFSETS.size()) {
            List<int[]> chunkOffsets = CHUNK_SPAWN_OFFSETS.get(initialChunks - 1);
            for (int[] offset : chunkOffsets) {
                ChunkPos targetPos = new ChunkPos(centerChunkPos.x + offset[0], centerChunkPos.z + offset[1]);
                if (chunkSpawnController.request(level, "", false, targetPos.getMiddleBlockPosition(0), offset[0] == 0 && offset[1] == 0)) {
                    if (spawnChest && offset[0] == 0 && offset[1] == 0) {
                        SpawnChunkHelper.createNextSpawner(level, targetPos);
                    }
                }
            }
        } else {
            SpiralIterator spiralIterator = new SpiralIterator(centerChunkPos.x, centerChunkPos.z);
            for (int i = 0; i < initialChunks; i++) {
                ChunkPos targetPos = new ChunkPos(spiralIterator.getX(), spiralIterator.getY());
                if (chunkSpawnController.request(level, "", false, targetPos.getMiddleBlockPosition(0), i == 0)) {
                    if (spawnChest && i == 0) {
                        SpawnChunkHelper.createNextSpawner(level, targetPos);
                    }
                }
                spiralIterator.next();
            }
        }
    }

    public static void onResourceManagerReload(ResourceManager resourceManager) {
        Gson gson = new GsonBuilder().registerTypeAdapter(SkyChunkGenerator.EmptyGenerationType.class, (JsonDeserializer<SkyChunkGenerator.EmptyGenerationType>) (json, typeOfT, context) -> SkyChunkGenerator.EmptyGenerationType.getFromString(json.getAsString())).create();
        loadScannerData(resourceManager, gson);
        SkyDimensions.loadSkyDimensionData(resourceManager, gson);
    }

    private static void loadScannerData(ResourceManager resourceManager, Gson gson) {
        WorldScannerBlockEntity.clearItemMappings();
        int count = 0;
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(GatheringChunksConstants.SCANNER_DATA_PATH, r -> !r.getPath().isEmpty() && !GatheringChunksConstants.SCANNER_DATA_PATH.equals(r.getPath()));
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

        if (server.getTickCount() % 200 == 0) {
            PlayerProgressionHelper.checkPlayers(server);
        }
    }
}