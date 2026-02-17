package com.ryvione.gatheringchunks.server.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class TreePlacementSavedData extends SavedData {
    private static final String DATA_ID = "chunkbychunk_tree_placement";
    private final Set<Long> processedChunks = new HashSet<>();

    public TreePlacementSavedData() {
    }

    public static TreePlacementSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(TreePlacementSavedData::new, TreePlacementSavedData::load, DataFixTypes.LEVEL),
                DATA_ID);
    }

    public static TreePlacementSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TreePlacementSavedData data = new TreePlacementSavedData();
        if (tag.contains("ProcessedChunks")) {
            for (long pos : tag.getLongArray("ProcessedChunks")) {
                data.processedChunks.add(pos);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLongArray("ProcessedChunks", processedChunks.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    public boolean isChunkProcessed(ChunkPos chunkPos) {
        return processedChunks.contains(chunkPos.toLong());
    }

    public void markChunkProcessed(ChunkPos chunkPos) {
        processedChunks.add(chunkPos.toLong());
        setDirty();
    }
}