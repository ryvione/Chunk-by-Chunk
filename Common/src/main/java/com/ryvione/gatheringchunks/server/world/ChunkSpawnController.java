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

    public static ChunkSpawnController get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getChunkSource().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new ChunkSpawnController(server),
                        (tag, provider) -> ChunkSpawnController.load(server, tag, provider),
                        DataFixTypes.LEVEL
                ),
                "chunkspawncontroller"
        );
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
                    true
            );
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
        return tag;
    }

    private ChunkSpawnController(MinecraftServer server) {
        this.server = server;
    }

    public void tick() {
        if (currentSpawnRequest != null) {
            if (!sourceChunkFuture.isDone()) {
                return;
            }
            switch (phase) {
                case COPY_BIOMES -> {
                    ChunkAccess sourceChunk = sourceChunkFuture.getNow(ChunkResult.error("Chunk not loaded")).orElse(null);
                    if (sourceChunk != null) {
                        updateBiomes(sourceLevel,
                                sourceChunk,
                                targetLevel,
                                targetLevel.getChunk(currentSpawnRequest.targetChunkPos.x, currentSpawnRequest.targetChunkPos.z),
                                currentSpawnRequest.targetChunkPos);
                    }
                    phase = SpawnPhase.SPAWN_BLOCKS;
                    currentLayer = targetLevel.getMinBuildHeight();
                    setDirty();
                }
                case SPAWN_BLOCKS -> {
                    int minLayer = currentLayer;
                    int maxLayer = Math.min(currentLayer + ChunkByChunkConfig.get().getGeneration().getChunkLayerSpawnRate(), targetLevel.getMaxBuildHeight() + 1);
                    copyBlocks(
                            sourceLevel,
                            currentSpawnRequest.sourceChunkPos,
                            targetLevel,
                            currentSpawnRequest.targetChunkPos,
                            minLayer,
                            maxLayer,
                            currentSpawnRequest.overwrite); // Added overwrite flag
                    if (maxLayer > targetLevel.getMaxBuildHeight()) {
                        TreePlacementHandler.ensureTreesInChunk(targetLevel, currentSpawnRequest.targetChunkPos);
                        if (ChunkByChunkConfig.get().getGeneration().spawnNewChunkChest() && !ChunkByChunkConfig.get().getGeneration().spawnChestInInitialChunkOnly()) {
                            SpawnChunkHelper.createNextSpawner(targetLevel, currentSpawnRequest.targetChunkPos);
                        }
                        phase = SpawnPhase.UPDATE_BARRIERS;
                    } else {
                        currentLayer = maxLayer;
                    }
                    setDirty();
                }
                case UPDATE_BARRIERS -> {
                    ChunkBarrierManager.updateBarriersAfterChunkSpawn(targetLevel, currentSpawnRequest.sourceChunkPos, currentSpawnRequest.targetChunkPos);
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
            currentSpawnRequest = requests.removeFirst();
            targetLevel = server.getLevel(currentSpawnRequest.targetLevel());
            sourceLevel = server.getLevel(currentSpawnRequest.sourceLevel());
            forcedTargetChunk = targetLevel.setChunkForced(currentSpawnRequest.targetChunkPos().x, currentSpawnRequest.targetChunkPos().z, true);
            sourceLevel.setChunkForced(currentSpawnRequest.sourceChunkPos().x, currentSpawnRequest.sourceChunkPos().z, true);
            sourceChunkFuture = sourceLevel.getChunkSource().getChunkFuture(
                    currentSpawnRequest.sourceChunkPos().x,
                    currentSpawnRequest.sourceChunkPos().z,
                    ChunkStatus.FULL,
                    true
            );
            if (currentSpawnRequest.immediate) {
                phase = SpawnPhase.UPDATE_BARRIERS;
            } else {
                phase = SpawnPhase.COPY_BIOMES;
            }
            GatheringChunksConstants.LOGGER.info("Spawning chunk " + currentSpawnRequest.targetChunkPos + " in " + targetLevel.dimension());
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
                currentSpawnRequest.sourceChunkPos().getMaxBlockZ()
        );
        List<Entity> entities = sourceLevel.getEntitiesOfClass(Entity.class, boundingBox, (x) -> true);
        for (Entity e : entities) {
            Vec3 pos = new Vec3(
                    e.getX() + (currentSpawnRequest.targetChunkPos().x - currentSpawnRequest.sourceChunkPos().x) * 16,
                    e.getY(),
                    e.getZ() + (currentSpawnRequest.targetChunkPos().z - currentSpawnRequest.sourceChunkPos().z) * 16
            );
            Entity movedEntity = ChangeDimensionHelper.changeDimension(e, targetLevel, pos);
            if (movedEntity != null) {
                movedEntity.setPos(pos);
            }
        }
    }

    private void completeSpawnRequest() {
        if (forcedTargetChunk) {
            targetLevel.setChunkForced(currentSpawnRequest.targetChunkPos().x, currentSpawnRequest.targetChunkPos().z, false);
            sourceLevel.setChunkForced(currentSpawnRequest.sourceChunkPos().x, currentSpawnRequest.sourceChunkPos().z, false);
            currentSpawnRequest = null;
        }
    }

    private static void copyBlocks(ServerLevel sourceLevel, ChunkPos sourceChunkPos, ServerLevel targetLevel, ChunkPos targetChunkPos, int fromLayer, int toLayer, boolean overwrite) {
        int xOffset = targetChunkPos.getMinBlockX() - sourceChunkPos.getMinBlockX();
        int zOffset = targetChunkPos.getMinBlockZ() - sourceChunkPos.getMinBlockZ();
        Block sealedBlock = Blocks.BEDROCK;
        if (targetLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator skyChunkGenerator && skyChunkGenerator.getGenerationType() == SkyChunkGenerator.EmptyGenerationType.Sealed) {
            sealedBlock = skyChunkGenerator.getSealBlock();
        }
        BlockPos.MutableBlockPos sourceBlock = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos targetBlock = new BlockPos.MutableBlockPos();
        for (int y = fromLayer; y < toLayer; y++) {
            for (int z = sourceChunkPos.getMinBlockZ(); z <= sourceChunkPos.getMaxBlockZ(); z++) {
                for (int x = sourceChunkPos.getMinBlockX(); x <= sourceChunkPos.getMaxBlockX(); x++) {
                    sourceBlock.set(x, y, z);
                    targetBlock.set(x + xOffset, y, z + zOffset);
                    Block existingBlock = targetLevel.getBlockState(targetBlock).getBlock();
                    if (overwrite || existingBlock instanceof AirBlock || existingBlock instanceof LiquidBlock || existingBlock == Blocks.BEDROCK || existingBlock == sealedBlock || existingBlock == Blocks.SNOW) {
                        BlockState newBlock = sourceLevel.getBlockState(sourceBlock);
                        if (ChunkByChunkConfig.get().getGameplayConfig().isChunkSpawnLeafDecayDisabled() && newBlock.getBlock() instanceof LeavesBlock) {
                            newBlock = newBlock.setValue(LeavesBlock.PERSISTENT, true);
                        }
                        // Use UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE to avoid redundant neighbor updates during bulk copy
                        targetLevel.setBlock(targetBlock, newBlock, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        BlockEntity fromBlockEntity = sourceLevel.getBlockEntity(sourceBlock);
                        BlockEntity toBlockEntity = targetLevel.getBlockEntity(targetBlock);
                        if (fromBlockEntity != null && toBlockEntity != null) {
                            toBlockEntity.loadWithComponents(fromBlockEntity.saveWithFullMetadata(targetLevel.registryAccess()), targetLevel.registryAccess());
                            targetLevel.setBlockEntity(toBlockEntity);
                        }
                    }
                }
            }
        }
    }

    private static void updateBiomes(ServerLevel sourceLevel, ChunkAccess sourceChunk, ServerLevel targetLevel, ChunkAccess targetChunk, ChunkPos targetChunkPos) {
        if (sourceChunk.getSections().length != targetChunk.getSections().length) {
            GatheringChunksConstants.LOGGER.warn("Section count mismatch between {} and {} - {} vs {}", sourceLevel.dimension(), targetLevel.dimension(), sourceChunk.getSections().length, targetChunk.getSections().length);
        }
        if (!(targetChunk instanceof LevelChunk levelChunk)) {
            return;
        }
        boolean biomesUpdated = false;
        for (int targetIndex = 0; targetIndex < targetChunk.getSections().length; targetIndex++) {
            int sourceIndex = (targetIndex < sourceChunk.getSections().length) ? targetIndex : sourceChunk.getSections().length - 1;
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
                    PalettedContainer<net.minecraft.world.level.block.state.BlockState> blockStates =
                            (PalettedContainer<net.minecraft.world.level.block.state.BlockState>) targetSection.getStates();
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
                    double scale = DimensionType.getTeleportationScale(targetLevel.dimensionType(), synchLevel.dimensionType());
                    BlockPos pos = currentSpawnRequest.targetChunkPos().getMiddleBlockPosition(0);
                    ChunkPos synchChunk = new ChunkPos(new BlockPos((int)(pos.getX() * scale), 0, (int)(pos.getZ() * scale)));
                    request(synchChunk, synchLevelId, synchChunk, synchGenerator.getGenerationLevel(), false, currentSpawnRequest.overwrite);
                }
            }
        }
    }

    /**
     * Checks if a chunk's top layer consists only of water blocks.
     * Used to detect ocean chunks that should be replaced with plains.
     */
    private static boolean isWaterOnlyChunk(ServerLevel level, ChunkPos chunkPos) {
        try {
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                return false;
            }
            
            ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
            int waterBlockCount = 0;
            int totalSurfaceBlocks = 0;
            
            // Optimization: Sampling instead of checking every single block
            // and using Heightmap to find the surface quickly
            for (int x = 0; x < 16; x += 2) {
                for (int z = 0; z < 16; z += 2) {
                    int y = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                    if (y > level.getMinBuildHeight()) {
                        totalSurfaceBlocks++;
                        BlockState state = chunk.getBlockState(new BlockPos(chunkPos.getMinBlockX() + x, y - 1, chunkPos.getMinBlockZ() + z));
                        if (state.getBlock() == Blocks.WATER) {
                            waterBlockCount++;
                        }
                    }
                }
            }
            
            // If more than 85% of sampled surface blocks are water, consider it water-only
            return totalSurfaceBlocks > 0 && ((float) waterBlockCount / totalSurfaceBlocks) > 0.85f;
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

    public boolean request(ServerLevel level, String biomeTheme, boolean random, BlockPos blockPos, boolean immediate, boolean overwrite) {
        ChunkPos targetChunkPos = new ChunkPos(blockPos);
        boolean canSpawn = SpawnChunkHelper.isEmptyChunk(level, targetChunkPos) || overwrite;
        
        if (isValidForLevel(level, biomeTheme, random) && canSpawn && level.getChunkSource().getGenerator() instanceof SkyChunkGenerator generator) {
            ChunkPos sourceChunkPos;
            ResourceKey<Level> sourceLevel;
            String effectiveBiomeTheme = biomeTheme;
            
            // For biome-themed spawners, always use random chunks to generate unique terrain
            // For regular random spawners, also use random chunks
            if (!biomeTheme.isEmpty()) {
                // Determine a large stable offset for this theme to ensure adjacent spawners produce continuous terrain
                Random themeRng = new Random(biomeTheme.hashCode());
                int offsetX = themeRng.nextInt(-1000000, 1000000);
                int offsetZ = themeRng.nextInt(-1000000, 1000000);
                sourceChunkPos = new ChunkPos(targetChunkPos.x + offsetX, targetChunkPos.z + offsetZ);
            } else if (random) {
                // For regular random spawners, keep using a random chunk
                Random rng = new Random(blockPos.asLong());
                sourceChunkPos = new ChunkPos(rng.nextInt(Short.MIN_VALUE, Short.MAX_VALUE), rng.nextInt(Short.MIN_VALUE, Short.MAX_VALUE));
            } else {
                // Only use matching coordinates for non-themed, non-random spawners
                sourceChunkPos = new ChunkPos(targetChunkPos.x, targetChunkPos.z);
            }
            
            if (biomeTheme.isEmpty()) {
                sourceLevel = generator.getGenerationLevel();
            } else {
                sourceLevel = generator.getBiomeDimension(biomeTheme);
            }
            
            // Check if the source chunk would be water-only, and if so, use plains instead (ONLY for non-themed spawners)
            ServerLevel sourceLevelInstance = server.getLevel(sourceLevel);
            if (biomeTheme.isEmpty() && sourceLevelInstance != null && isWaterOnlyChunk(sourceLevelInstance, sourceChunkPos)) {
                GatheringChunksConstants.LOGGER.info("Detected water-only chunk at " + sourceChunkPos + ", spawning plains chunk instead");
                // Try to get plains biome dimension
                ResourceKey<Level> plainsDimension = generator.getBiomeDimension("plains");
                if (plainsDimension != null) {
                    sourceLevel = plainsDimension;
                    effectiveBiomeTheme = "plains";
                    // Generate a new random chunk position for plains
                    Random rng = new Random(blockPos.asLong() + 12345); // Different seed for variety
                    sourceChunkPos = new ChunkPos(rng.nextInt(Short.MIN_VALUE, Short.MAX_VALUE), rng.nextInt(Short.MIN_VALUE, Short.MAX_VALUE));
                }
            }
            
            return request(targetChunkPos, level.dimension(), sourceChunkPos, sourceLevel, immediate, overwrite);
        }
        return false;
    }

    public boolean request(ChunkPos targetChunkPos, ResourceKey<Level> targetLevel, ChunkPos sourceChunkPos, ResourceKey<Level> sourceLevel, boolean immediate, boolean overwrite) {
        SpawnRequest spawnRequest = new SpawnRequest(targetChunkPos, targetLevel, sourceChunkPos, sourceLevel, immediate, overwrite);
        if (!spawnRequest.equals(currentSpawnRequest) && !requests.contains(spawnRequest)) {
            if (immediate) {
                ServerLevel toLevel = server.getLevel(targetLevel);
                ServerLevel fromLevel = server.getLevel(sourceLevel);
                LevelChunk toChunk = toLevel.getChunk(targetChunkPos.x, targetChunkPos.z);
                LevelChunk fromChunk = fromLevel.getChunk(sourceChunkPos.x, sourceChunkPos.z);
                updateBiomes(fromLevel, fromChunk, toLevel, toChunk, targetChunkPos);
                copyBlocks(fromLevel, spawnRequest.sourceChunkPos, toLevel, spawnRequest.targetChunkPos, toLevel.getMinBuildHeight(), toLevel.getMaxBuildHeight() + 1, overwrite);
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
                                ResourceKey<Level> sourceLevel, boolean immediate, boolean overwrite) {
        public static final String TARGET_POS = "targetPos";
        public static final String TARGET_LEVEL = "targetLevel";
        public static final String SOURCE_POS = "sourcePos";
        public static final String SOURCE_LEVEL = "sourceLevel";
        public static final String IMMEDIATE = "immediate";
        public static final String OVERWRITE = "overwrite";

        public static SpawnRequest load(CompoundTag tag) {
            ChunkPos targetPos = new ChunkPos(tag.getLong(TARGET_POS));
            ResourceKey<Level> targetLevel = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(tag.getString(TARGET_LEVEL)));
            ChunkPos sourcePos = new ChunkPos(tag.getLong(SOURCE_POS));
            ResourceKey<Level> sourceLevel = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(tag.getString(SOURCE_LEVEL)));
            boolean immediate = tag.getBoolean(IMMEDIATE);
            boolean overwrite = tag.getBoolean(OVERWRITE);
            return new SpawnRequest(targetPos, targetLevel, sourcePos, sourceLevel, immediate, overwrite);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SpawnRequest that = (SpawnRequest) o;
            if (!targetChunkPos.equals(that.targetChunkPos)) return false;
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
            return tag;
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