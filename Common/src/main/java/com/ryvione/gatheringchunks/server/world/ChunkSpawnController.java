package com.ryvione.gatheringchunks.server.world;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.util.ChangeDimensionHelper;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChunkSpawnController extends SavedData {
    private final MinecraftServer server;
    private final Deque<SpawnRequest> requests = new ArrayDeque<>();
    @Nullable
    private SpawnRequest currentSpawnRequest = null;
    @Nullable
    private SpawnPhase phase;
    private boolean forcedTargetChunk;
    private int currentLayer;
    @Nullable
    private transient ServerLevel sourceLevel;
    @Nullable
    private transient ServerLevel targetLevel;
    @Nullable
    private transient CompletableFuture<ChunkResult<ChunkAccess>> sourceChunkFuture;
    private Map<String, Integer> maxChunks = new HashMap<>();
    private Map<String, Integer> spawnedChunkCount = new HashMap<>();
    private Map<ChunkPos, TerrainProfile> chunkTerrainProfiles = new HashMap<>();
    private final List<PendingSearch> pendingSearches = new ArrayList<>();
    private final Map<String, Set<ChunkPos>> knownGoodSourceChunks = new HashMap<>();

    private static class PendingSearch {
        final ServerLevel level;
        final String biomeTheme;
        final boolean immediate;
        final boolean overwrite;
        final Random seedFinder;
        final TerrainProfile targetProfile;
        final ChunkPos targetPos;
        int attempts;

        PendingSearch(ServerLevel level, String biomeTheme, boolean immediate,
                boolean overwrite, Random seedFinder, TerrainProfile targetProfile, ChunkPos targetPos) {
            this.level = level;
            this.biomeTheme = biomeTheme;
            this.immediate = immediate;
            this.overwrite = overwrite;
            this.seedFinder = seedFinder;
            this.targetProfile = targetProfile;
            this.targetPos = targetPos;
            this.attempts = 0;
        }
    }

    public int getMaxChunks(String dimensionId) {
        if (dimensionId.equals("minecraft:the_nether")) {
            return maxChunks.getOrDefault(dimensionId, 1);
        }
        return maxChunks.getOrDefault(dimensionId, 4);
    }

    public void setMaxChunks(String dimensionId, int max) {
        this.maxChunks.put(dimensionId, max);
        setDirty();
    }

    public int getSpawnedChunkCount(String dimensionId) {
        return spawnedChunkCount.getOrDefault(dimensionId, 0);
    }

    public void decreaseSpawnedChunkCount(String dimensionId) {
        int val = spawnedChunkCount.getOrDefault(dimensionId, 0);
        if (val > 0) {
            spawnedChunkCount.put(dimensionId, val - 1);
            setDirty();
        }
    }

    public static ChunkSpawnController get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getChunkSource().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new ChunkSpawnController(server),
                        (tag, provider) -> ChunkSpawnController.load(server, tag, provider),
                        DataFixTypes.LEVEL),
                "chunkspawncontroller");
    }

    private static ChunkSpawnController load(MinecraftServer server, CompoundTag tag, HolderLookup.Provider provider) {
        ChunkSpawnController chunkSpawnController = new ChunkSpawnController(server);
        chunkSpawnController.loadInternal(tag, provider);
        return chunkSpawnController;
    }

    private void loadInternal(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag requestsTag = tag.getList("requests", ListTag.TAG_COMPOUND);
        for (int i = 0; i < requestsTag.size(); i++) {
            requests.add(SpawnRequest.load(requestsTag.getCompound(i)));
        }
        if (tag.contains("currentRequest")) {
            currentSpawnRequest = SpawnRequest.load(tag.getCompound("currentRequest"));
            phase = SpawnPhase.valueOf(tag.getString("phase"));
            forcedTargetChunk = tag.getBoolean("forcedTargetChunk");
            currentLayer = tag.getInt("currentLayer");
            sourceLevel = server.getLevel(currentSpawnRequest.sourceLevel);
            targetLevel = server.getLevel(currentSpawnRequest.targetLevel);
            sourceChunkFuture = sourceLevel.getChunkSource().getChunkFuture(
                    currentSpawnRequest.sourceChunkPos().x,
                    currentSpawnRequest.sourceChunkPos().z,
                    ChunkStatus.FULL,
                    true);
        }
        if (tag.contains("maxChunksMap")) {
            CompoundTag map = tag.getCompound("maxChunksMap");
            for (String key : map.getAllKeys()) {
                maxChunks.put(key, map.getInt(key));
            }
        } else if (tag.contains("maxChunks")) {
            maxChunks.put("minecraft:overworld", tag.getInt("maxChunks"));
        }

        if (tag.contains("spawnedChunkCountMap")) {
            CompoundTag map = tag.getCompound("spawnedChunkCountMap");
            for (String key : map.getAllKeys()) {
                spawnedChunkCount.put(key, map.getInt(key));
            }
        } else if (tag.contains("spawnedChunkCount")) {
            spawnedChunkCount.put("minecraft:overworld", tag.getInt("spawnedChunkCount"));
        }
        if (tag.contains("terrainProfiles")) {
            CompoundTag profilesTag = tag.getCompound("terrainProfiles");
            for (String key : profilesTag.getAllKeys()) {
                String[] parts = key.split(",");
                if (parts.length == 2) {
                    try {
                        ChunkPos pos = new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                        chunkTerrainProfiles.put(pos, TerrainProfile.load(profilesTag.getCompound(key)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag requestsTag = new ListTag();
        for (SpawnRequest request : requests) {
            requestsTag.add(request.save());
        }
        tag.put("requests", requestsTag);
        if (currentSpawnRequest != null) {
            tag.put("currentRequest", currentSpawnRequest.save());
            tag.putString("phase", phase.name());
            tag.putBoolean("forcedTargetChunk", forcedTargetChunk);
            tag.putInt("currentLayer", currentLayer);
        }
        CompoundTag maxChunksMap = new CompoundTag();
        maxChunks.forEach(maxChunksMap::putInt);
        tag.put("maxChunksMap", maxChunksMap);

        CompoundTag spawnedCountMap = new CompoundTag();
        spawnedChunkCount.forEach(spawnedCountMap::putInt);
        tag.put("spawnedChunkCountMap", spawnedCountMap);

        CompoundTag profilesTag = new CompoundTag();
        chunkTerrainProfiles.forEach((pos, profile) -> {
            profilesTag.put(pos.x + "," + pos.z, profile.save());
        });
        tag.put("terrainProfiles", profilesTag);

        return tag;
    }

    private ChunkSpawnController(MinecraftServer server) {
        this.server = server;
    }

    public void tick() {

        Iterator<PendingSearch> it = pendingSearches.iterator();
        int totalAttemptsThisTick = 0;
        int maxAttemptsPerTick = 10;

        while (it.hasNext() && totalAttemptsThisTick < maxAttemptsPerTick) {
            PendingSearch search = it.next();
            ResourceKey<Level> sourceLevelKey = ((SkyChunkGenerator) search.level.getChunkSource().getGenerator())
                    .getBiomeDimension(search.biomeTheme);
            ServerLevel sourceLevelInstance = server.getLevel(sourceLevelKey);

            if (sourceLevelInstance == null) {
                it.remove();
                continue;
            }

            for (int i = 0; i < 2 && totalAttemptsThisTick < maxAttemptsPerTick; i++) {
                totalAttemptsThisTick++;
                search.attempts++;

                int offsetX = search.seedFinder.nextInt(-500000, 500000);
                int offsetZ = search.seedFinder.nextInt(-500000, 500000);
                ChunkPos candidatePos = new ChunkPos(offsetX, offsetZ);

                if (!isWaterOnlyChunk(sourceLevelInstance, candidatePos, search.biomeTheme)) {
                    if (search.targetProfile != null) {
                        TerrainProfile candidateProfile = analyzeChunkTerrain(sourceLevelInstance, candidatePos,
                                search.biomeTheme);
                        if (candidateProfile != null && terrainsMatch(search.targetProfile, candidateProfile,
                                search.targetPos, candidatePos)) {
                            requests.add(new SpawnRequest(search.targetPos, search.level.dimension(), candidatePos,
                                    sourceLevelKey, search.immediate, search.overwrite, false));
                            chunkTerrainProfiles.put(search.targetPos, candidateProfile);
                            updatePreScanCache(sourceLevelInstance, candidatePos, search.biomeTheme);
                            setDirty();

                            it.remove();
                            break;
                        }
                    } else {
                        requests.add(new SpawnRequest(search.targetPos, search.level.dimension(), candidatePos,
                                sourceLevelKey, search.immediate, search.overwrite, false));

                        TerrainProfile prof = analyzeChunkTerrain(sourceLevelInstance, candidatePos, search.biomeTheme);
                        if (prof != null) {
                            chunkTerrainProfiles.put(search.targetPos, prof);
                            updatePreScanCache(sourceLevelInstance, candidatePos, search.biomeTheme);
                            setDirty();
                        }

                        it.remove();
                        break;
                    }
                }

                if (search.attempts >= 500) {
                    GatheringChunksConstants.LOGGER
                            .warn("Async search for '" + search.biomeTheme + "' failed after 500 attempts.");
                    it.remove();
                    break;
                }
            }
        }

        if (currentSpawnRequest != null) {
            if (!sourceChunkFuture.isDone()) {
                return;
            }
            switch (phase) {
                case COPY_BIOMES -> {
                    ChunkAccess sourceChunk = sourceChunkFuture.getNow(ChunkResult.error("Chunk not loaded"))
                            .orElse(null);
                    if (sourceChunk != null) {
                        updateBiomes(sourceLevel,
                                sourceChunk,
                                targetLevel,
                                targetLevel.getChunk(currentSpawnRequest.targetChunkPos.x,
                                        currentSpawnRequest.targetChunkPos.z),
                                currentSpawnRequest.targetChunkPos);
                    }
                    phase = SpawnPhase.SPAWN_BLOCKS;
                    currentLayer = targetLevel.getMinBuildHeight();
                    setDirty();
                }
                case SPAWN_BLOCKS -> {
                    int minLayer = currentLayer;
                    int maxLayer = Math.min(
                            currentLayer + ChunkByChunkConfig.get().getGeneration().getChunkLayerSpawnRate(),
                            targetLevel.getMaxBuildHeight() + 1);
                    copyBlocks(
                            sourceLevel,
                            currentSpawnRequest.sourceChunkPos,
                            targetLevel,
                            currentSpawnRequest.targetChunkPos,
                            minLayer,
                            maxLayer,
                            currentSpawnRequest.overwrite);
                    if (maxLayer > targetLevel.getMaxBuildHeight()) {
                        TreePlacementHandler.ensureTreesInChunk(targetLevel, currentSpawnRequest.targetChunkPos);
                        if (ChunkByChunkConfig.get().getDifficulty().spawnNewChunkChest()
                                && !ChunkByChunkConfig.get().getDifficulty().spawnChestInInitialChunkOnly()) {
                            SpawnChunkHelper.createNextSpawner(targetLevel, currentSpawnRequest.targetChunkPos);
                        }
                        if (targetLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator generator
                                && generator.getGenerationType() == SkyChunkGenerator.EmptyGenerationType.Nether) {
                            generator.markChunkSpawned(currentSpawnRequest.targetChunkPos.toLong());
                        }
                        phase = SpawnPhase.UPDATE_BARRIERS;
                    } else {
                        currentLayer = maxLayer;
                    }
                    setDirty();
                }
                case UPDATE_BARRIERS -> {
                    ChunkBarrierManager.updateBarriersAfterChunkSpawn(targetLevel, currentSpawnRequest.sourceChunkPos,
                            currentSpawnRequest.targetChunkPos);
                    phase = SpawnPhase.SYNCH_CHUNKS;
                    setDirty();
                }
                case SYNCH_CHUNKS -> {
                    synchChunks();
                    phase = SpawnPhase.SPAWN_ENTITIES;
                    setDirty();
                }
                case SPAWN_ENTITIES -> {
                    if (sourceLevel.areEntitiesLoaded(currentSpawnRequest.sourceChunkPos.toLong())) {
                        spawnChunkEntities();
                        completeSpawnRequest();
                        setDirty();
                    }
                }
            }
        } else if (!requests.isEmpty()) {
            currentSpawnRequest = requests.poll();
            targetLevel = server.getLevel(currentSpawnRequest.targetLevel());
            sourceLevel = server.getLevel(currentSpawnRequest.sourceLevel());
            forcedTargetChunk = targetLevel.setChunkForced(currentSpawnRequest.targetChunkPos().x,
                    currentSpawnRequest.targetChunkPos().z, true);
            sourceLevel.setChunkForced(currentSpawnRequest.sourceChunkPos().x, currentSpawnRequest.sourceChunkPos().z,
                    true);
            sourceChunkFuture = sourceLevel.getChunkSource().getChunkFuture(
                    currentSpawnRequest.sourceChunkPos().x,
                    currentSpawnRequest.sourceChunkPos().z,
                    ChunkStatus.FULL,
                    true);
            if (currentSpawnRequest.immediate) {
                phase = SpawnPhase.UPDATE_BARRIERS;
            } else {
                phase = SpawnPhase.COPY_BIOMES;
            }
            GatheringChunksConstants.LOGGER
                    .info("Spawning chunk " + currentSpawnRequest.targetChunkPos + " in " + targetLevel.dimension());
            setDirty();
        }
    }

    private void spawnChunkEntities() {
        AABB boundingBox = new AABB(
                currentSpawnRequest.sourceChunkPos().getMinBlockX(),
                sourceLevel.getMinBuildHeight(),
                currentSpawnRequest.sourceChunkPos().getMinBlockZ(),
                currentSpawnRequest.sourceChunkPos().getMaxBlockX(),
                sourceLevel.getMaxBuildHeight(),
                currentSpawnRequest.sourceChunkPos().getMaxBlockZ());
        List<Entity> entities = sourceLevel.getEntitiesOfClass(Entity.class, boundingBox, (x) -> true);
        for (Entity e : entities) {
            Vec3 pos = new Vec3(
                    e.getX() + (currentSpawnRequest.targetChunkPos().x - currentSpawnRequest.sourceChunkPos().x) * 16,
                    e.getY(),
                    e.getZ() + (currentSpawnRequest.targetChunkPos().z - currentSpawnRequest.sourceChunkPos().z) * 16);
            Entity movedEntity = ChangeDimensionHelper.changeDimension(e, targetLevel, pos);
            if (movedEntity != null) {
                movedEntity.setPos(pos);
            }
        }
    }

    private void completeSpawnRequest() {
        if (forcedTargetChunk) {
            targetLevel.setChunkForced(currentSpawnRequest.targetChunkPos.x, currentSpawnRequest.targetChunkPos.z,
                    false);
            sourceLevel.setChunkForced(currentSpawnRequest.sourceChunkPos.x, currentSpawnRequest.sourceChunkPos.z,
                    false);
            if (currentSpawnRequest.isInitial()) {
                ChunkEngineManager.get(server).notifyInitialChunkSpawned(targetLevel,
                        currentSpawnRequest.targetChunkPos);
            } else {
                ChunkEngineManager.get(server).notifyChunkSpawned(targetLevel, currentSpawnRequest.targetChunkPos);
            }
            if (!currentSpawnRequest.overwrite) {
                String dim = targetLevel.dimension().location().toString();
                spawnedChunkCount.put(dim, spawnedChunkCount.getOrDefault(dim, 0) + 1);
                setDirty();
            }
            currentSpawnRequest = null;
        }
    }

    private static void copyBlocks(ServerLevel sourceLevel, ChunkPos sourceChunkPos, ServerLevel targetLevel,
            ChunkPos targetChunkPos, int fromLayer, int toLayer, boolean overwrite) {
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
                    if (existingBlock == Blocks.NETHER_PORTAL || existingBlock == Blocks.OBSIDIAN) {
                        continue;
                    }

                    boolean isLavaInNether = targetLevel.dimension() == Level.NETHER && existingBlock == Blocks.LAVA;

                    if (overwrite || existingBlock instanceof AirBlock || existingBlock instanceof LiquidBlock
                            || existingBlock == Blocks.BEDROCK || existingBlock == sealedBlock
                            || existingBlock == Blocks.SNOW || isLavaInNether) {
                        BlockState newBlock = sourceLevel.getBlockState(sourceBlock);

                        if (newBlock.is(Blocks.NETHER_PORTAL)) {
                            continue;
                        }

                        if (ChunkByChunkConfig.get().getGameplayConfig().isChunkSpawnLeafDecayDisabled()
                                && newBlock.getBlock() instanceof LeavesBlock) {
                            newBlock = newBlock.setValue(LeavesBlock.PERSISTENT, true);
                        }
                        targetLevel.setBlock(targetBlock, newBlock, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
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

    private static void updateBiomes(ServerLevel sourceLevel, ChunkAccess sourceChunk, ServerLevel targetLevel,
            ChunkAccess targetChunk, ChunkPos targetChunkPos) {
        if (sourceChunk.getSections().length != targetChunk.getSections().length) {
            GatheringChunksConstants.LOGGER.warn("Section count mismatch between {} and {} - {} vs {}",
                    sourceLevel.dimension(), targetLevel.dimension(), sourceChunk.getSections().length,
                    targetChunk.getSections().length);
        }
        if (!(targetChunk instanceof LevelChunk levelChunk)) {
            return;
        }
        boolean biomesUpdated = false;
        for (int targetIndex = 0; targetIndex < targetChunk.getSections().length; targetIndex++) {
            int sourceIndex = (targetIndex < sourceChunk.getSections().length) ? targetIndex
                    : sourceChunk.getSections().length - 1;
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
                    PalettedContainer<net.minecraft.world.level.block.state.BlockState> blockStates = (PalettedContainer<net.minecraft.world.level.block.state.BlockState>) targetSection
                            .getStates();
                    LevelChunkSection newSection = new LevelChunkSection(blockStates, newBiomes);
                    levelChunk.getSections()[targetIndex] = newSection;
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

    private void synchChunks() {
        if (targetLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator generator) {
            for (ResourceKey<Level> synchLevelId : generator.getSynchedLevels()) {
                ServerLevel synchLevel = server.getLevel(synchLevelId);
                if (synchLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator synchGenerator) {
                    double scale = DimensionType.getTeleportationScale(targetLevel.dimensionType(),
                            synchLevel.dimensionType());
                    BlockPos pos = currentSpawnRequest.targetChunkPos().getMiddleBlockPosition(0);
                    ChunkPos synchChunk = new ChunkPos(
                            new BlockPos((int) (pos.getX() * scale), 0, (int) (pos.getZ() * scale)));
                    request(synchChunk, synchLevelId, synchChunk, synchGenerator.getGenerationLevel(), false,
                            currentSpawnRequest.overwrite);
                }
            }
        }
    }

    private TerrainProfile analyzeChunkTerrain(ServerLevel level, ChunkPos pos, String biomeTheme) {
        try {
            ChunkAccess chunk = level.getChunk(pos.x, pos.z, ChunkStatus.SURFACE, true);
            if (chunk == null) {
                return null;
            }

            int totalHeight = 0;
            int sampleCount = 0;
            List<Integer> heights = new ArrayList<>();

            int[] northEdge = new int[16];
            int[] southEdge = new int[16];
            int[] eastEdge = new int[16];
            int[] westEdge = new int[16];

            for (int i = 0; i < 16; i++) {
                northEdge[i] = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, i, 0);
                southEdge[i] = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, i, 15);
                westEdge[i] = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 0, i);
                eastEdge[i] = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 15, i);
            }

            for (int x = 0; x < 16; x += 4) {
                for (int z = 0; z < 16; z += 4) {
                    int y = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                    if (y > level.getMinBuildHeight()) {
                        heights.add(y);
                        totalHeight += y;
                        sampleCount++;
                    }
                }
            }

            if (sampleCount == 0)
                return null;
            int avgHeight = totalHeight / sampleCount;

            int variance = 0;
            for (int h : heights)
                variance += Math.abs(h - avgHeight);
            variance = variance / heights.size();

            return new TerrainProfile(northEdge, southEdge, eastEdge, westEdge, avgHeight, variance, biomeTheme, pos);
        } catch (Exception e) {
            GatheringChunksConstants.LOGGER.warn("Failed to analyze chunk terrain: " + e.getMessage());
            return null;
        }
    }

    private boolean terrainsMatch(TerrainProfile target, TerrainProfile candidate, ChunkPos targetPos,
            ChunkPos candidatePos) {
        if (target == null || candidate == null)
            return true;
        if (!target.biomeTheme.equals(candidate.biomeTheme))
            return false;

        int dx = candidatePos.x - targetPos.x;
        int dz = candidatePos.z - targetPos.z;

        int[] targetEdge = null;
        int[] candidateEdge = null;

        if (dx == 1 && dz == 0) {
            targetEdge = target.eastEdge;
            candidateEdge = candidate.westEdge;
        } else if (dx == -1 && dz == 0) {
            targetEdge = target.westEdge;
            candidateEdge = candidate.eastEdge;
        } else if (dx == 0 && dz == 1) {
            targetEdge = target.southEdge;
            candidateEdge = candidate.northEdge;
        } else if (dx == 0 && dz == -1) {
            targetEdge = target.northEdge;
            candidateEdge = candidate.southEdge;
        }

        if (targetEdge != null && candidateEdge != null) {
            int totalDiff = 0;
            for (int i = 0; i < 16; i++) {
                totalDiff += Math.abs(targetEdge[i] - candidateEdge[i]);
            }
            return (totalDiff / 16) <= 2;
        }

        return Math.abs(target.averageHeight - candidate.averageHeight) <= 5;
    }

    private List<ChunkPos> getAdjacentChunksWithTheme(ChunkPos targetPos, String biomeTheme) {
        List<ChunkPos> adjacent = new ArrayList<>();
        int[][] offsets = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        for (int[] offset : offsets) {
            ChunkPos adjPos = new ChunkPos(targetPos.x + offset[0], targetPos.z + offset[1]);
            TerrainProfile profile = chunkTerrainProfiles.get(adjPos);
            if (profile != null && profile.biomeTheme.equals(biomeTheme)) {
                adjacent.add(adjPos);
            }
        }

        return adjacent;
    }

    private static boolean isWaterOnlyChunk(ServerLevel level, ChunkPos chunkPos, String biomeTheme) {
        try {
            ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);
            if (chunk == null) {
                return true;
            }

            int waterBlockCount = 0;
            int totalSurfaceBlocks = 0;
            Set<Block> uniqueBlocks = new HashSet<>();

            for (int x = 0; x < 16; x += 2) {
                for (int z = 0; z < 16; z += 2) {
                    int y = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                    if (y > level.getMinBuildHeight()) {
                        totalSurfaceBlocks++;
                        BlockPos surfacePos = new BlockPos(chunkPos.getMinBlockX() + x, y, chunkPos.getMinBlockZ() + z);
                        BlockState state = chunk.getBlockState(surfacePos);
                        Block block = state.getBlock();

                        if (block == Blocks.WATER) {
                            waterBlockCount++;
                        } else if (!(block instanceof AirBlock)) {
                            uniqueBlocks.add(block);
                        }

                        for (int dy = 1; dy <= 3; dy++) {
                            BlockState belowState = chunk.getBlockState(surfacePos.below(dy));
                            Block belowBlock = belowState.getBlock();
                            if (!(belowBlock instanceof AirBlock) && !(belowBlock instanceof LiquidBlock)) {
                                uniqueBlocks.add(belowBlock);
                            }
                        }
                    }
                }
            }

            if (totalSurfaceBlocks == 0) {
                return false;
            }

            float waterPercentage = (float) waterBlockCount / totalSurfaceBlocks;

            boolean isLandBiome = !biomeTheme.isEmpty() &&
                    !biomeTheme.equals("ocean") &&
                    !biomeTheme.equals("river");

            if (isLandBiome) {
                return waterPercentage > 0.3f || uniqueBlocks.size() < 3;
            } else {
                return waterPercentage > 0.85f;
            }
        } catch (Exception e) {
            GatheringChunksConstants.LOGGER.warn("Failed to check for water-only chunk: " + e.getMessage());
            return false;
        }
    }

    public boolean isValidForLevel(ServerLevel level, String biomeTheme, boolean random) {
        if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator generator) {
            if (!biomeTheme.isEmpty()) {
                return generator.getBiomeDimension(biomeTheme) != null;
            } else if (random) {
                return generator.isRandomChunkSpawnerAllowed();
            } else {
                return generator.isChunkSpawnerAllowed();
            }
        }
        return false;
    }

    public boolean request(ServerLevel level, String biomeTheme, boolean random, BlockPos blockPos) {
        return request(level, biomeTheme, random, blockPos, false);
    }

    public boolean request(ServerLevel level, String biomeTheme, boolean random, BlockPos blockPos, boolean immediate) {
        return request(level, biomeTheme, random, blockPos, immediate, false);
    }

    public boolean request(ServerLevel level, String biomeTheme, boolean random, BlockPos blockPos, boolean immediate,
            boolean overwrite) {
        return request(level, biomeTheme, random, blockPos, immediate, overwrite, false);
    }

    public boolean request(ServerLevel level, String biomeTheme, boolean random, BlockPos blockPos, boolean immediate,
            boolean overwrite, boolean isInitial) {
        ChunkPos targetChunkPos = new ChunkPos(blockPos);
        boolean isEmptyChunk = SpawnChunkHelper.isEmptyChunk(level, targetChunkPos);
        boolean canSpawn = isEmptyChunk || overwrite;

        boolean experimentalLimit = ChunkByChunkConfig.get().getDifficulty().isExperimentalChunkLimit();
        String dim = level.dimension().location().toString();
        int max = getMaxChunks(dim);
        int current = getSpawnedChunkCount(dim);

        if (experimentalLimit && !overwrite && current >= max) {
            if (!isInitial) {
                GatheringChunksConstants.LOGGER
                        .info("Spawn prevented: Chunk Limit Reached (" + current + "/" + max + ")");
                return false;
            }
        }

        if (isValidForLevel(level, biomeTheme, random) && canSpawn
                && level.getChunkSource().getGenerator() instanceof SkyChunkGenerator generator) {

            if (!biomeTheme.isEmpty()) {
                ResourceKey<Level> sourceLevel = generator.getBiomeDimension(biomeTheme);
                if (sourceLevel == null) {
                    GatheringChunksConstants.LOGGER
                            .warn("Biome theme '" + biomeTheme + "' has no associated dimension!");
                    return false;
                }

                long biomeSeed = biomeTheme.hashCode() + (targetChunkPos.x * 31L + targetChunkPos.z * 17L);
                Random seedFinder = new Random(biomeSeed);

                List<ChunkPos> adjacentChunks = getAdjacentChunksWithTheme(targetChunkPos, biomeTheme);
                TerrainProfile targetProfile = null;
                if (!adjacentChunks.isEmpty()) {
                    targetProfile = chunkTerrainProfiles.get(adjacentChunks.get(0));

                    ChunkPos chainedPos = tryChainedSourceMatch(level, targetChunkPos, biomeTheme);
                    if (chainedPos != null) {
                        GatheringChunksConstants.LOGGER
                                .info("Using CHAINED source chunk for '" + biomeTheme + "' at " + chainedPos);
                        return request(targetChunkPos, level.dimension(), chainedPos, sourceLevel, immediate, overwrite,
                                isInitial);
                    }
                }

                ChunkPos cachedPos = tryCachedSourceMatch(level, biomeTheme);
                if (cachedPos != null) {
                    GatheringChunksConstants.LOGGER
                            .info("Using CACHED source chunk for '" + biomeTheme + "' at " + cachedPos);
                    return request(targetChunkPos, level.dimension(), cachedPos, sourceLevel, immediate, overwrite,
                            isInitial);
                }

                pendingSearches.add(new PendingSearch(level, biomeTheme, immediate, overwrite, seedFinder,
                        targetProfile, targetChunkPos));

                return true;
            } else if (random) {
                long randomSeed = 424242L + (targetChunkPos.x * 31L + targetChunkPos.z * 17L);
                Random seedFinder = new Random(randomSeed);
                pendingSearches.add(
                        new PendingSearch(level, biomeTheme, immediate, overwrite, seedFinder, null, targetChunkPos));
                return true;
            } else {
                return request(targetChunkPos, level.dimension(), targetChunkPos, generator.getGenerationLevel(),
                        immediate, overwrite, isInitial);
            }
        }
        return false;
    }

    public boolean request(ChunkPos targetChunkPos, ResourceKey<Level> targetLevel, ChunkPos sourceChunkPos,
            ResourceKey<Level> sourceLevel, boolean immediate, boolean overwrite) {
        return request(targetChunkPos, targetLevel, sourceChunkPos, sourceLevel, immediate, overwrite, false);
    }

    public boolean request(ChunkPos targetChunkPos, ResourceKey<Level> targetLevel, ChunkPos sourceChunkPos,
            ResourceKey<Level> sourceLevel, boolean immediate, boolean overwrite, boolean isInitial) {
        SpawnRequest spawnRequest = new SpawnRequest(targetChunkPos, targetLevel, sourceChunkPos, sourceLevel,
                immediate, overwrite, isInitial);
        if (!spawnRequest.equals(currentSpawnRequest) && !requests.contains(spawnRequest)) {
            if (immediate) {
                ServerLevel toLevel = server.getLevel(targetLevel);
                ServerLevel fromLevel = server.getLevel(sourceLevel);
                LevelChunk toChunk = toLevel.getChunk(targetChunkPos.x, targetChunkPos.z);
                LevelChunk fromChunk = fromLevel.getChunk(sourceChunkPos.x, sourceChunkPos.z);
                updateBiomes(fromLevel, fromChunk, toLevel, toChunk, targetChunkPos);

                updatePreScanCache(fromLevel, sourceChunkPos, "unknown");
                copyBlocks(fromLevel, spawnRequest.sourceChunkPos, toLevel, spawnRequest.targetChunkPos,
                        toLevel.getMinBuildHeight(), toLevel.getMaxBuildHeight() + 1, overwrite);
                requests.addFirst(spawnRequest);
            } else {
                requests.add(spawnRequest);
            }
            setDirty();
            return true;
        }
        return false;
    }

    public boolean isBusy() {
        return currentSpawnRequest != null || !requests.isEmpty();
    }

    private record SpawnRequest(ChunkPos targetChunkPos, ResourceKey<Level> targetLevel, ChunkPos sourceChunkPos,
            ResourceKey<Level> sourceLevel, boolean immediate, boolean overwrite, boolean isInitial) {
        public static final String TARGET_POS = "targetPos";
        public static final String TARGET_LEVEL = "targetLevel";
        public static final String SOURCE_POS = "sourcePos";
        public static final String SOURCE_LEVEL = "sourceLevel";
        public static final String IMMEDIATE = "immediate";
        public static final String OVERWRITE = "overwrite";
        public static final String IS_INITIAL = "isInitial";

        public static SpawnRequest load(CompoundTag tag) {
            ChunkPos targetPos = new ChunkPos(tag.getLong(TARGET_POS));
            ResourceKey<Level> targetLevel = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString(TARGET_LEVEL)));
            ChunkPos sourcePos = new ChunkPos(tag.getLong(SOURCE_POS));
            ResourceKey<Level> sourceLevel = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString(SOURCE_LEVEL)));
            boolean immediate = tag.getBoolean(IMMEDIATE);
            boolean overwrite = tag.getBoolean(OVERWRITE);
            boolean isInitial = tag.getBoolean(IS_INITIAL);
            return new SpawnRequest(targetPos, targetLevel, sourcePos, sourceLevel, immediate, overwrite, isInitial);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            SpawnRequest that = (SpawnRequest) o;
            if (!targetChunkPos.equals(that.targetChunkPos))
                return false;
            return targetLevel.equals(that.targetLevel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetChunkPos, targetLevel);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong(TARGET_POS, targetChunkPos.toLong());
            tag.putString(TARGET_LEVEL, targetLevel.location().toString());
            tag.putLong(SOURCE_POS, sourceChunkPos.toLong());
            tag.putString(SOURCE_LEVEL, sourceLevel.location().toString());
            tag.putBoolean(IMMEDIATE, immediate);
            tag.putBoolean(OVERWRITE, overwrite);
            tag.putBoolean(IS_INITIAL, isInitial);
            return tag;
        }
    }

    private static class TerrainProfile {
        final int[] northEdge;
        final int[] southEdge;
        final int[] eastEdge;
        final int[] westEdge;
        final int averageHeight;
        final int heightVariance;
        final String biomeTheme;
        final ChunkPos sourcePos;

        TerrainProfile(int[] north, int[] south, int[] east, int[] west, int averageHeight, int heightVariance,
                String biomeTheme, ChunkPos sourcePos) {
            this.northEdge = north;
            this.southEdge = south;
            this.eastEdge = east;
            this.westEdge = west;
            this.averageHeight = averageHeight;
            this.heightVariance = heightVariance;
            this.biomeTheme = biomeTheme;
            this.sourcePos = sourcePos;
        }

        static TerrainProfile load(CompoundTag tag) {
            int avgHeight = tag.getInt("avgHeight");
            int variance = tag.getInt("variance");
            String theme = tag.getString("theme");
            int[] north = tag.getIntArray("north");
            int[] south = tag.getIntArray("south");
            int[] east = tag.getIntArray("east");
            int[] west = tag.getIntArray("west");

            if (north.length == 0)
                north = new int[16];
            if (south.length == 0)
                south = new int[16];
            if (east.length == 0)
                east = new int[16];
            if (west.length == 0)
                west = new int[16];

            long sourcePosLong = tag.contains("sourcePos") ? tag.getLong("sourcePos") : ChunkPos.INVALID_CHUNK_POS;
            ChunkPos source = new ChunkPos(sourcePosLong);

            return new TerrainProfile(north, south, east, west, avgHeight, variance, theme, source);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("avgHeight", averageHeight);
            tag.putInt("variance", heightVariance);
            tag.putString("theme", biomeTheme);
            tag.putIntArray("north", northEdge);
            tag.putIntArray("south", southEdge);
            tag.putIntArray("east", eastEdge);
            tag.putIntArray("west", westEdge);
            if (sourcePos != null) {
                tag.putLong("sourcePos", sourcePos.toLong());
            }
            return tag;
        }
    }

    private ChunkPos tryChainedSourceMatch(ServerLevel targetLevel, ChunkPos targetPos, String theme) {
        int[][] offsets = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] offset : offsets) {
            ChunkPos adjTarget = new ChunkPos(targetPos.x + offset[0], targetPos.z + offset[1]);
            TerrainProfile adjProfile = chunkTerrainProfiles.get(adjTarget);

            if (adjProfile != null && adjProfile.biomeTheme.equals(theme)
                    && adjProfile.sourcePos.toLong() != ChunkPos.INVALID_CHUNK_POS) {
                ChunkPos candidateSource = new ChunkPos(adjProfile.sourcePos.x - offset[0],
                        adjProfile.sourcePos.z - offset[1]);

                SkyChunkGenerator gen = (SkyChunkGenerator) targetLevel.getChunkSource().getGenerator();
                ServerLevel sourceLevel = server.getLevel(gen.getBiomeDimension(theme));

                if (sourceLevel != null && !isWaterOnlyChunk(sourceLevel, candidateSource, theme)) {
                    return candidateSource;
                }
            }
        }
        return null;
    }

    private ChunkPos tryCachedSourceMatch(ServerLevel targetLevel, String theme) {
        Set<ChunkPos> cached = knownGoodSourceChunks.get(theme);
        if (cached != null && !cached.isEmpty()) {
            Iterator<ChunkPos> it = cached.iterator();
            ChunkPos pos = it.next();
            it.remove();
            return pos;
        }
        return null;
    }

    private void updatePreScanCache(ServerLevel sourceLevel, ChunkPos sourcePos, String theme) {
        Set<ChunkPos> cache = knownGoodSourceChunks.computeIfAbsent(theme, k -> new HashSet<>());
        if (cache.size() > 50)
            return;

        int[][] offsets = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] offset : offsets) {
            ChunkPos neighbor = new ChunkPos(sourcePos.x + offset[0], sourcePos.z + offset[1]);
            if (!isWaterOnlyChunk(sourceLevel, neighbor, theme)) {
                cache.add(neighbor);
            }
        }
    }

    private enum SpawnPhase {
        COPY_BIOMES,
        SPAWN_BLOCKS,
        UPDATE_BARRIERS,
        SYNCH_CHUNKS,
        SPAWN_ENTITIES
    }
}