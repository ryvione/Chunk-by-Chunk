package com.ryvione.gatheringchunks.server.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class ChestTracker extends SavedData {
    private final Set<BlockPos> chestPositions = new HashSet<>();
    private final Map<UUID, Boolean> playerTrackerEnabled = new HashMap<>();
    private final MinecraftServer server;

    public static ChestTracker get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return new ChestTracker(server);
        }
        return overworld.getChunkSource().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        () -> new ChestTracker(server),
                        (tag, provider) -> ChestTracker.load(server, tag, provider),
                        DataFixTypes.LEVEL
                ),
                "chunkbychunk_chest_tracker"
        );
    }

    private static ChestTracker load(MinecraftServer server, CompoundTag tag, HolderLookup.Provider provider) {
        ChestTracker tracker = new ChestTracker(server);
        ListTag positionsTag = tag.getList("chests", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < positionsTag.size(); i++) {
            CompoundTag posTag = positionsTag.getCompound(i);
            int x = posTag.getInt("x");
            int y = posTag.getInt("y");
            int z = posTag.getInt("z");
            tracker.chestPositions.add(new BlockPos(x, y, z).immutable());
        }

        ListTag playerSettingsTag = tag.getList("playerSettings", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < playerSettingsTag.size(); i++) {
            CompoundTag playerTag = playerSettingsTag.getCompound(i);
            UUID playerUUID = playerTag.getUUID("uuid");
            boolean enabled = playerTag.getBoolean("enabled");
            tracker.playerTrackerEnabled.put(playerUUID, enabled);
        }
        return tracker;
    }

    private ChestTracker(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag positionsTag = new ListTag();
        for (BlockPos pos : chestPositions) {
            CompoundTag posTag = new CompoundTag();
            BlockPos immutablePos = pos.immutable();
            posTag.putInt("x", immutablePos.getX());
            posTag.putInt("y", immutablePos.getY());
            posTag.putInt("z", immutablePos.getZ());
            positionsTag.add(posTag);
        }
        tag.put("chests", positionsTag);

        ListTag playerSettingsTag = new ListTag();
        for (Map.Entry<UUID, Boolean> entry : playerTrackerEnabled.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", entry.getKey());
            playerTag.putBoolean("enabled", entry.getValue());
            playerSettingsTag.add(playerTag);
        }
        tag.put("playerSettings", playerSettingsTag);
        return tag;
    }

    public void addChest(BlockPos pos) {
        chestPositions.add(pos.immutable());
        setDirty();
    }

    public void removeChest(BlockPos pos) {
        boolean removed = chestPositions.remove(pos);
        if (!removed) {
            removed = chestPositions.remove(pos.immutable());
        }
        if (removed) {
            setDirty();
        }
    }

    public Set<BlockPos> getChestPositions() {
        Set<BlockPos> result = new HashSet<>();
        for (BlockPos pos : chestPositions) {
            result.add(pos.immutable());
        }
        return result;
    }

    public void checkAndRemoveIfEmpty(BlockPos pos, ServerLevel level) {
        BlockPos immutablePos = pos.immutable();
        if (!chestPositions.contains(immutablePos)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(immutablePos);
        if (blockEntity instanceof RandomizableContainerBlockEntity chest) {
            boolean isEmpty = true;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (!chest.getItem(i).isEmpty()) {
                    isEmpty = false;
                    break;
                }
            }
            if (isEmpty) {
                removeChest(immutablePos);
            }
        } else {
            removeChest(immutablePos);
        }
    }

    public boolean isTracked(BlockPos pos) {
        return chestPositions.contains(pos.immutable());
    }

    public void setTrackerEnabled(UUID playerUUID, boolean enabled) {
        playerTrackerEnabled.put(playerUUID, enabled);
        setDirty();
    }

    public boolean isTrackerEnabled(UUID playerUUID) {
        return false;
    }
}