package com.ryvione.gatheringchunks.server.world;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BiomeCoordinateCache extends SavedData {
    private final MinecraftServer server;
    private final Map<String, Map<String, List<ChunkPos>>> biomeCache = new ConcurrentHashMap<>();
    private final Map<String, Set<ChunkPos>> scannedChunks = new ConcurrentHashMap<>();
    private final Map<String, Integer> scanProgress = new ConcurrentHashMap<>();
    private final Map<String, Boolean> initialScanComplete = new ConcurrentHashMap<>();

    private int chunksPerTick = 1;
    private long lastTickTime = System.currentTimeMillis();
    private final Deque<Long> tickTimes = new ArrayDeque<>();
    private long tickCounter = 0;
    private static final int TICK_HISTORY_SIZE = 100;

    private static final int INITIAL_SCAN_RADIUS = 100;
    private static final int MAX_CACHED_CHUNKS_PER_BIOME = 500;
    private static final int PROGRESSIVE_SCAN_RANGE = 1000;
    private static final long TARGET_TICK_TIME_MS = 30;
    private static final int SCAN_INTERVAL_TICKS = 5;

    public static BiomeCoordinateCache get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getChunkSource().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new BiomeCoordinateCache(server),
                        (tag, provider) -> BiomeCoordinateCache.load(server, tag, provider),
                        DataFixTypes.LEVEL),
                "biomecoordinatecache");
    }

    private static BiomeCoordinateCache load(MinecraftServer server, CompoundTag tag, HolderLookup.Provider provider) {
        BiomeCoordinateCache cache = new BiomeCoordinateCache(server);
        cache.loadInternal(tag);
        return cache;
    }

    private BiomeCoordinateCache(MinecraftServer server) {
        this.server = server;
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (availableProcessors <= 2) {
            this.chunksPerTick = 1;
        } else if (availableProcessors <= 4) {
            this.chunksPerTick = 2;
        } else if (availableProcessors <= 8) {
            this.chunksPerTick = 3;
        } else {
            this.chunksPerTick = 5;
        }
    }

    private void loadInternal(CompoundTag tag) {
        CompoundTag dimensionsTag = tag.getCompound("dimensions");
        for (String dimensionKey : dimensionsTag.getAllKeys()) {
            CompoundTag dimensionData = dimensionsTag.getCompound(dimensionKey);

            Map<String, List<ChunkPos>> biomesForDimension = new ConcurrentHashMap<>();
            CompoundTag biomesTag = dimensionData.getCompound("biomes");
            for (String biomeTheme : biomesTag.getAllKeys()) {
                ListTag chunksTag = biomesTag.getList(biomeTheme, ListTag.TAG_LONG);
                List<ChunkPos> chunks = Collections.synchronizedList(new ArrayList<>());
                for (int i = 0; i < chunksTag.size(); i++) {
                    chunks.add(new ChunkPos(((net.minecraft.nbt.LongTag) chunksTag.get(i)).getAsLong()));
                }
                biomesForDimension.put(biomeTheme, chunks);
            }
            biomeCache.put(dimensionKey, biomesForDimension);

            Set<ChunkPos> scanned = ConcurrentHashMap.newKeySet();
            long[] scannedArray = dimensionData.getLongArray("scanned");
            for (long pos : scannedArray) {
                scanned.add(new ChunkPos(pos));
            }
            scannedChunks.put(dimensionKey, scanned);

            scanProgress.put(dimensionKey, dimensionData.getInt("scanProgress"));
            initialScanComplete.put(dimensionKey, dimensionData.getBoolean("initialScanComplete"));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag dimensionsTag = new CompoundTag();

        for (Map.Entry<String, Map<String, List<ChunkPos>>> dimensionEntry : biomeCache.entrySet()) {
            CompoundTag dimensionData = new CompoundTag();

            CompoundTag biomesTag = new CompoundTag();
            for (Map.Entry<String, List<ChunkPos>> biomeEntry : dimensionEntry.getValue().entrySet()) {
                ListTag chunksTag = new ListTag();
                for (ChunkPos pos : biomeEntry.getValue()) {
                    chunksTag.add(net.minecraft.nbt.LongTag.valueOf(pos.toLong()));
                }
                biomesTag.put(biomeEntry.getKey(), chunksTag);
            }
            dimensionData.put("biomes", biomesTag);

            Set<ChunkPos> scanned = scannedChunks.getOrDefault(dimensionEntry.getKey(), new HashSet<>());
            long[] scannedArray = new long[Math.min(scanned.size(), 5000)];
            int i = 0;
            for (ChunkPos pos : scanned) {
                if (i >= scannedArray.length) break;
                scannedArray[i++] = pos.toLong();
            }
            dimensionData.putLongArray("scanned", scannedArray);
            dimensionData.putInt("scanProgress", scanProgress.getOrDefault(dimensionEntry.getKey(), 0));
            dimensionData.putBoolean("initialScanComplete", initialScanComplete.getOrDefault(dimensionEntry.getKey(), false));

            dimensionsTag.put(dimensionEntry.getKey(), dimensionData);
        }

        tag.put("dimensions", dimensionsTag);
        return tag;
    }

    public void tick() {
        tickCounter++;

        if (tickCounter % SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        adjustChunksPerTick();

        for (ServerLevel level : server.getAllLevels()) {
            if (!(level.getChunkSource().getGenerator() instanceof SkyChunkGenerator)) {
                continue;
            }

            String dimensionKey = level.dimension().location().toString();

            if (!initialScanComplete.getOrDefault(dimensionKey, false)) {
                performInitialScan(level, dimensionKey);
            } else {
                performProgressiveScan(level, dimensionKey);
            }
        }
    }

    private void adjustChunksPerTick() {
        long currentTime = System.currentTimeMillis();
        long tickDuration = currentTime - lastTickTime;
        lastTickTime = currentTime;

        tickTimes.addLast(tickDuration);
        if (tickTimes.size() > TICK_HISTORY_SIZE) {
            tickTimes.removeFirst();
        }

        if (tickTimes.size() < 20) {
            return;
        }

        double avgTickTime = tickTimes.stream().mapToLong(Long::longValue).average().orElse(50.0);

        if (avgTickTime > TARGET_TICK_TIME_MS * 2 && chunksPerTick > 1) {
            chunksPerTick = Math.max(1, chunksPerTick - 1);
        } else if (avgTickTime < TARGET_TICK_TIME_MS && chunksPerTick < 10) {
            chunksPerTick++;
        }
    }

    private void performInitialScan(ServerLevel level, String dimensionKey) {
        int progress = scanProgress.getOrDefault(dimensionKey, 0);
        int chunksScanned = 0;

        BlockPos spawn = level.getSharedSpawnPos();

        int chunkRadius = progress;
        int endRadius = Math.min(chunkRadius + 3, INITIAL_SCAN_RADIUS);

        for (int r = chunkRadius; r < endRadius && chunksScanned < chunksPerTick; r++) {
            for (int x = -r; x <= r && chunksScanned < chunksPerTick; x++) {
                for (int z = -r; z <= r && chunksScanned < chunksPerTick; z++) {
                    if (Math.abs(x) != r && Math.abs(z) != r) continue;

                    ChunkPos chunkPos = new ChunkPos(
                            (spawn.getX() >> 4) + x,
                            (spawn.getZ() >> 4) + z
                    );

                    if (!isChunkScanned(dimensionKey, chunkPos)) {
                        scanAndCacheChunk(level, dimensionKey, chunkPos);
                        chunksScanned++;
                    }
                }
            }
        }

        scanProgress.put(dimensionKey, endRadius);

        if (endRadius >= INITIAL_SCAN_RADIUS) {
            initialScanComplete.put(dimensionKey, true);
            int totalCached = biomeCache.getOrDefault(dimensionKey, Collections.emptyMap())
                    .values().stream().mapToInt(List::size).sum();
            GatheringChunksConstants.LOGGER.info("Initial biome scan complete for dimension: {} - Cached {} chunks across {} biomes",
                    dimensionKey, totalCached, biomeCache.getOrDefault(dimensionKey, Collections.emptyMap()).size());
        }

        if (chunksScanned > 0) {
            setDirty();
        }
    }

    private void performProgressiveScan(ServerLevel level, String dimensionKey) {
        Random random = new Random(System.currentTimeMillis() + level.getGameTime());
        int chunksScanned = 0;

        for (int i = 0; i < chunksPerTick; i++) {
            int x = random.nextInt(-PROGRESSIVE_SCAN_RANGE, PROGRESSIVE_SCAN_RANGE);
            int z = random.nextInt(-PROGRESSIVE_SCAN_RANGE, PROGRESSIVE_SCAN_RANGE);
            ChunkPos chunkPos = new ChunkPos(x, z);

            if (!isChunkScanned(dimensionKey, chunkPos)) {
                scanAndCacheChunk(level, dimensionKey, chunkPos);
                chunksScanned++;
            }
        }

        if (chunksScanned > 0) {
            setDirty();
        }
    }

    private void scanAndCacheChunk(ServerLevel level, String dimensionKey, ChunkPos chunkPos) {
        try {
            ChunkAccess chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);

            if (chunk == null) {
                chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.BIOMES, false);
            }

            if (chunk == null) {
                return;
            }

            Set<String> biomesInChunk = new HashSet<>();
            for (int x = 0; x < 16; x += 8) {
                for (int z = 0; z < 16; z += 8) {
                    BlockPos samplePos = new BlockPos(
                            chunkPos.getMinBlockX() + x,
                            64,
                            chunkPos.getMinBlockZ() + z
                    );
                    Holder<Biome> biomeHolder = level.getBiome(samplePos);
                    String biomeTheme = getBiomeTheme(biomeHolder);
                    if (biomeTheme != null) {
                        biomesInChunk.add(biomeTheme);
                    }
                }
            }

            for (String biomeTheme : biomesInChunk) {
                addToCache(dimensionKey, biomeTheme, chunkPos);
            }

            markChunkScanned(dimensionKey, chunkPos);
        } catch (Exception e) {
        }
    }

    private String getBiomeTheme(Holder<Biome> biomeHolder) {
        String biomeName = biomeHolder.unwrapKey().orElse(null) != null
                ? biomeHolder.unwrapKey().get().location().getPath()
                : "";

        if (biomeName.contains("plains")) return "plains";
        if (biomeName.contains("desert")) return "desert";
        if (biomeName.contains("savanna")) return "savanna";
        if (biomeName.contains("taiga") || biomeName.contains("snowy")) return "snow";
        if (biomeName.contains("forest") || biomeName.contains("birch") || biomeName.contains("dark_forest")) return "forest";
        if (biomeName.contains("jungle")) return "jungle";
        if (biomeName.contains("mountain") || biomeName.contains("peak") || biomeName.contains("grove")) return "mountain";
        if (biomeName.contains("badlands") || biomeName.contains("mesa")) return "badlands";
        if (biomeName.contains("swamp") || biomeName.contains("mangrove")) return "swamp";
        if (biomeName.contains("mushroom")) return "mushroom";
        if (biomeName.contains("cherry")) return "cherryblossum";
        if (biomeName.contains("stony") || biomeName.contains("stone") || biomeName.contains("windswept")) return "rocky";

        return null;
    }

    private void addToCache(String dimensionKey, String biomeTheme, ChunkPos chunkPos) {
        List<ChunkPos> cached = biomeCache.computeIfAbsent(dimensionKey, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(biomeTheme, k -> Collections.synchronizedList(new ArrayList<>()));

        if (!cached.contains(chunkPos)) {
            cached.add(chunkPos);
        }

        if (cached.size() > MAX_CACHED_CHUNKS_PER_BIOME) {
            cached.remove(0);
        }
    }

    private void markChunkScanned(String dimensionKey, ChunkPos chunkPos) {
        scannedChunks.computeIfAbsent(dimensionKey, k -> ConcurrentHashMap.newKeySet()).add(chunkPos);
    }

    private boolean isChunkScanned(String dimensionKey, ChunkPos chunkPos) {
        return scannedChunks.getOrDefault(dimensionKey, Collections.emptySet()).contains(chunkPos);
    }

    public ChunkPos getRandomCachedChunk(String dimensionKey, String biomeTheme, Random random) {
        List<ChunkPos> chunks = biomeCache.getOrDefault(dimensionKey, Collections.emptyMap())
                .getOrDefault(biomeTheme, Collections.emptyList());

        if (chunks.isEmpty()) {
            return null;
        }

        return chunks.get(random.nextInt(chunks.size()));
    }

    public boolean hasCachedChunks(String dimensionKey, String biomeTheme) {
        List<ChunkPos> chunks = biomeCache.getOrDefault(dimensionKey, Collections.emptyMap())
                .getOrDefault(biomeTheme, Collections.emptyList());
        return !chunks.isEmpty();
    }

    public int getCachedChunkCount(String dimensionKey, String biomeTheme) {
        return biomeCache.getOrDefault(dimensionKey, Collections.emptyMap())
                .getOrDefault(biomeTheme, Collections.emptyList())
                .size();
    }

    public float getScanProgress(String dimensionKey) {
        if (initialScanComplete.getOrDefault(dimensionKey, false)) {
            return 100.0f;
        }
        return (scanProgress.getOrDefault(dimensionKey, 0) / (float) INITIAL_SCAN_RADIUS) * 100.0f;
    }
}