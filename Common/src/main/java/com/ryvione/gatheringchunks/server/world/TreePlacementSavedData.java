package com.ryvione.gatheringchunks.server.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class TreePlacementSavedData extends SavedData {
    private static final String DATA_ID = "chunkbychunk_tree_placement";
    private boolean treesPlaced = false;

    public TreePlacementSavedData() {
    }

    public static TreePlacementSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(TreePlacementSavedData::new, TreePlacementSavedData::load, DataFixTypes.LEVEL), DATA_ID);
    }

    public static TreePlacementSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TreePlacementSavedData data = new TreePlacementSavedData();
        data.treesPlaced = tag.getBoolean("TreesPlaced");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("TreesPlaced", treesPlaced);
        return tag;
    }

    public boolean areTreesPlaced() {
        return treesPlaced;
    }

    public void setTreesPlaced(boolean treesPlaced) {
        this.treesPlaced = treesPlaced;
        setDirty();
    }
}
