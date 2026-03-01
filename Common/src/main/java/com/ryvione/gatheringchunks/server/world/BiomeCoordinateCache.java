/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.server.world;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BiomeCoordinateCache extends SavedData {
    private final MinecraftServer server;
    private final Map<String, Map<String, List<ChunkPos>>> biomeCache = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> scannedChunks = new ConcurrentHashMap<>();
    private final Map<String, Integer> scanProgress = new ConcurrentHashMap<>();
    private final Map<String, Boolean> initialScanComplete = new ConcurrentHashMap<>();

    private final Map<String, BiomeSource> dimBiomeSources = new ConcurrentHashMap<>();

    private int chunksPerTick;
    private long lastTickTime = System.currentTimeMillis();
    private final Deque<Long> tickTimes = new ArrayDeque<>();
    private long tickCounter = 0;
    private static final int TICK_HISTORY_SIZE = 100;

    private static final int INITIAL_SCAN_RADIUS = 150;
    private static final int MAX_CACHED_CHUNKS_PER_BIOME = 500;
    private static final int PROGRESSIVE_SCAN_RANGE = 2000;
    private static final long TARGET_TICK_TIME_MS = 30;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int RINGS_PER_SCAN = 4;

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
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores <= 2) {
            this.chunksPerTick = 8;
        } else if (cores <= 4) {
            this.chunksPerTick = 16;
        } else if (cores <= 8) {
            this.chunksPerTick = 24;
        } else {
            this.chunksPerTick = 32;
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

            scanProgress.put(dimensionKey, dimensionData.getInt("scanProgress"));
            initialScanComplete.put(dimensionKey, dimensionData.getBoolean("initialScanComplete"));

            if (dimensionData.contains("scannedChunks")) {
                ListTag scannedTag = dimensionData.getList("scannedChunks", net.minecraft.nbt.LongTag.TAG_LONG);
                Set<Long> scanned = ConcurrentHashMap.newKeySet();
                for (int i = 0; i < scannedTag.size(); i++) {
                    scanned.add(((net.minecraft.nbt.LongTag) scannedTag.get(i)).getAsLong());
                }
                scannedChunks.put(dimensionKey, scanned);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag dimensionsTag = new CompoundTag();

        Set<String> allDimKeys = new java.util.HashSet<>();
        allDimKeys.addAll(biomeCache.keySet());
        allDimKeys.addAll(scanProgress.keySet());

        for (String dimensionKey : allDimKeys) {
            CompoundTag dimensionData = new CompoundTag();

            CompoundTag biomesTag = new CompoundTag();
            Map<String, List<ChunkPos>> biomesForDim = biomeCache.getOrDefault(dimensionKey, Collections.emptyMap());
            for (Map.Entry<String, List<ChunkPos>> biomeEntry : biomesForDim.entrySet()) {
                ListTag chunksTag = new ListTag();
                for (ChunkPos pos : biomeEntry.getValue()) {
                    chunksTag.add(net.minecraft.nbt.LongTag.valueOf(pos.toLong()));
                }
                biomesTag.put(biomeEntry.getKey(), chunksTag);
            }
            dimensionData.put("biomes", biomesTag);

            dimensionData.putInt("scanProgress", scanProgress.getOrDefault(dimensionKey, 0));
            dimensionData.putBoolean("initialScanComplete", initialScanComplete.getOrDefault(dimensionKey, false));

            Set<Long> scanned = scannedChunks.getOrDefault(dimensionKey, Collections.emptySet());
            ListTag scannedTag = new ListTag();
            for (long chunkLong : scanned) {
                scannedTag.add(net.minecraft.nbt.LongTag.valueOf(chunkLong));
            }
            dimensionData.put("scannedChunks", scannedTag);

            dimensionsTag.put(dimensionKey, dimensionData);
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

        Map<String, ServerLevel> themeDimLevels = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator skyGen) {
                for (Map.Entry<String, ResourceKey<Level>> entry : skyGen.getBiomeDimensions().entrySet()) {
                    ServerLevel themeLevel = server.getLevel(entry.getValue());
                    if (themeLevel != null) {
                        themeDimLevels.put(themeLevel.dimension().location().toString(), themeLevel);
                    }
                }
                String dimensionKey = level.dimension().location().toString();
                if (!initialScanComplete.getOrDefault(dimensionKey, false)) {
                    performInitialScan(level, dimensionKey);
                } else {
                    performProgressiveScan(level, dimensionKey);
                }
            }
        }

        for (Map.Entry<String, ServerLevel> entry : themeDimLevels.entrySet()) {
            String dimKey = entry.getKey();
            ServerLevel themeLevel = entry.getValue();
            if (!initialScanComplete.getOrDefault(dimKey, false)) {
                performInitialScan(themeLevel, dimKey);
            } else {
                performProgressiveScan(themeLevel, dimKey);
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

        if (avgTickTime > TARGET_TICK_TIME_MS * 2 && chunksPerTick > 4) {
            chunksPerTick = Math.max(4, chunksPerTick - 4);
        } else if (avgTickTime < TARGET_TICK_TIME_MS && chunksPerTick < 64) {
            chunksPerTick += 4;
        }
    }

    private void performInitialScan(ServerLevel level, String dimensionKey) {
        int progress = scanProgress.getOrDefault(dimensionKey, 0);
        int chunksScanned = 0;

        int spawnChunkX = level.getSharedSpawnPos().getX() >> 4;
        int spawnChunkZ = level.getSharedSpawnPos().getZ() >> 4;

        int startRadius = progress;
        int endRadius = Math.min(startRadius + RINGS_PER_SCAN, INITIAL_SCAN_RADIUS);

        outer:
        for (int r = startRadius; r < endRadius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) != r && Math.abs(z) != r) continue;
                    if (chunksScanned >= chunksPerTick) break outer;

                    ChunkPos chunkPos = new ChunkPos(spawnChunkX + x, spawnChunkZ + z);
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
            GatheringChunksConstants.LOGGER.info(
                    "Initial biome scan complete for {}: cached {} chunks across {} biomes",
                    dimensionKey, totalCached,
                    biomeCache.getOrDefault(dimensionKey, Collections.emptyMap()).size());
        }

        if (chunksScanned > 0) {
            setDirty();
        }
    }

    private void performProgressiveScan(ServerLevel level, String dimensionKey) {
        Map<String, List<ChunkPos>> dimCache = biomeCache.getOrDefault(dimensionKey, Collections.emptyMap());
        int totalCached = dimCache.values().stream().mapToInt(List::size).sum();
        if (totalCached >= MAX_CACHED_CHUNKS_PER_BIOME * dimCache.size() && !dimCache.isEmpty()) {
            return;
        }

        Random random = new Random(System.currentTimeMillis() ^ level.getGameTime());
        int chunksScanned = 0;
        int maxScanThisCall = Math.min(chunksPerTick, 16);

        for (int i = 0; i < maxScanThisCall; i++) {
            int x = random.nextInt(-PROGRESSIVE_SCAN_RANGE, PROGRESSIVE_SCAN_RANGE);
            int z = random.nextInt(-PROGRESSIVE_SCAN_RANGE, PROGRESSIVE_SCAN_RANGE);
            ChunkPos chunkPos = new ChunkPos(x, z);

            if (!isChunkScanned(dimensionKey, chunkPos)) {
                scanAndCacheChunk(level, dimensionKey, chunkPos);
                chunksScanned++;
            }
        }

        Set<Long> scanned = scannedChunks.getOrDefault(dimensionKey, Collections.emptySet());
        if (scanned.size() > 100000) {
            Set<Long> trimmed = ConcurrentHashMap.newKeySet();
            int keep = 50000;
            int skip = scanned.size() - keep;
            int i = 0;
            for (long v : scanned) {
                if (i++ >= skip) trimmed.add(v);
            }
            scannedChunks.put(dimensionKey, trimmed);
        }

        if (chunksScanned > 0) {
            setDirty();
        }
    }


    private void scanAndCacheChunk(ServerLevel level, String dimensionKey, ChunkPos chunkPos) {
        try {
            BiomeSource biomeSource = getBiomeSourceForDim(level, dimensionKey);
            if (biomeSource == null) {
                return;
            }

            if (biomeSource instanceof FixedBiomeSource) {
                Holder<Biome> biomeHolder = biomeSource.getNoiseBiome(0, 16, 0, null);
                String theme = getBiomeTheme(biomeHolder);
                if (theme != null) {
                    addToCache(dimensionKey, theme, chunkPos);
                }
                markChunkScanned(dimensionKey, chunkPos);
                return;
            }

            Set<String> biomesInChunk = new HashSet<>();
            int baseQuartX = chunkPos.getMinBlockX() >> 2;
            int baseQuartZ = chunkPos.getMinBlockZ() >> 2;
            for (int dx = 0; dx <= 3; dx += 3) {
                for (int dz = 0; dz <= 3; dz += 3) {
                    Holder<Biome> biomeHolder = biomeSource.getNoiseBiome(baseQuartX + dx, 16, baseQuartZ + dz, null);
                    String theme = getBiomeTheme(biomeHolder);
                    if (theme != null) {
                        biomesInChunk.add(theme);
                    }
                }
            }

            for (String theme : biomesInChunk) {
                addToCache(dimensionKey, theme, chunkPos);
            }
            markChunkScanned(dimensionKey, chunkPos);
        } catch (Exception e) {
        }
    }

    private BiomeSource getBiomeSourceForDim(ServerLevel level, String dimensionKey) {
        return dimBiomeSources.computeIfAbsent(dimensionKey, k -> {
            var gen = level.getChunkSource().getGenerator();
            if (gen instanceof SkyChunkGenerator skyGen) {
                return skyGen.getBiomeSource();
            }
            return gen.getBiomeSource();
        });
    }

    private String getBiomeTheme(Holder<Biome> biomeHolder) {
        if (biomeHolder == null) return null;
        var keyOpt = biomeHolder.unwrapKey();
        if (keyOpt.isEmpty()) return null;
        String biomeName = keyOpt.get().location().getPath();

        if (biomeName.equals("nether_wastes")) return "netherwastes";
        if (biomeName.equals("soul_sand_valley")) return "soulsandvalley";
        if (biomeName.equals("crimson_forest")) return "crimsonforest";
        if (biomeName.equals("warped_forest")) return "warpedforest";
        if (biomeName.equals("basalt_deltas")) return "basaltdeltas";

        if (biomeName.equals("dark_forest")) return "darkforest";
        if (biomeName.equals("mangrove_swamp")) return "mangroveswamp";
        if (biomeName.equals("ice_spikes")) return "icespikes";
        if (biomeName.contains("ocean")) return "ocean";
        if (biomeName.contains("taiga") && !biomeName.contains("snowy")) return "taiga";
        if (biomeName.contains("snowy") || biomeName.contains("frozen") || biomeName.contains("ice")) return "snow";
        if (biomeName.contains("cherry")) return "cherryblossum";
        if (biomeName.contains("mushroom")) return "mushroom";
        if (biomeName.contains("jungle")) return "jungle";
        if (biomeName.contains("desert")) return "desert";
        if (biomeName.contains("badlands") || biomeName.contains("mesa")) return "badlands";
        if (biomeName.contains("savanna")) return "savanna";
        if (biomeName.contains("swamp")) return "swamp";
        if (biomeName.contains("plains")) return "plains";
        if (biomeName.contains("peak") || biomeName.contains("grove") || (biomeName.contains("windswept") && biomeName.contains("hill"))) return "mountain";
        if (biomeName.contains("stony") || biomeName.contains("windswept")) return "rocky";
        if (biomeName.contains("forest") || biomeName.contains("birch")) return "forest";

        return null;
    }

    private void addToCache(String dimensionKey, String biomeTheme, ChunkPos chunkPos) {
        List<ChunkPos> cached = biomeCache
                .computeIfAbsent(dimensionKey, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(biomeTheme, k -> Collections.synchronizedList(new ArrayList<>()));

        if (!cached.contains(chunkPos)) {
            cached.add(chunkPos);
        }

        if (cached.size() > MAX_CACHED_CHUNKS_PER_BIOME) {
            cached.remove(0);
        }
    }

    private void markChunkScanned(String dimensionKey, ChunkPos chunkPos) {
        scannedChunks.computeIfAbsent(dimensionKey, k -> ConcurrentHashMap.newKeySet()).add(chunkPos.toLong());
    }

    private boolean isChunkScanned(String dimensionKey, ChunkPos chunkPos) {
        return scannedChunks.getOrDefault(dimensionKey, Collections.emptySet()).contains(chunkPos.toLong());
    }

    public ChunkPos getRandomCachedChunk(String dimensionKey, String biomeTheme, Random random) {
        List<ChunkPos> chunks = biomeCache.getOrDefault(dimensionKey, Collections.emptyMap())
                .getOrDefault(biomeTheme, Collections.emptyList());
        if (chunks.isEmpty()) return null;
        return chunks.get(random.nextInt(chunks.size()));
    }

    public boolean hasCachedChunks(String dimensionKey, String biomeTheme) {
        return !biomeCache.getOrDefault(dimensionKey, Collections.emptyMap())
                .getOrDefault(biomeTheme, Collections.emptyList()).isEmpty();
    }

    public int getCachedChunkCount(String dimensionKey, String biomeTheme) {
        return biomeCache.getOrDefault(dimensionKey, Collections.emptyMap())
                .getOrDefault(biomeTheme, Collections.emptyList()).size();
    }

    public float getScanProgress(String dimensionKey) {
        if (initialScanComplete.getOrDefault(dimensionKey, false)) return 100.0f;
        return (scanProgress.getOrDefault(dimensionKey, 0) / (float) INITIAL_SCAN_RADIUS) * 100.0f;
    }
}