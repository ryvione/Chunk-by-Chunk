package com.ryvione.gatheringchunks.server.world.spawning;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public record SpawnRequest(
        ChunkPos targetChunkPos,
        ResourceKey<Level> targetLevel,
        ChunkPos sourceChunkPos,
        ResourceKey<Level> sourceLevel,
        boolean immediate,
        boolean overwrite,
        boolean isMenderRequest,
        @Nullable UUID playerUUID
) {
    public SpawnRequest(ChunkPos targetChunkPos, ResourceKey<Level> targetLevel, ChunkPos sourceChunkPos,
            ResourceKey<Level> sourceLevel, boolean immediate, boolean overwrite, boolean isMenderRequest) {
        this(targetChunkPos, targetLevel, sourceChunkPos, sourceLevel, immediate, overwrite, isMenderRequest, null);
    }

    private static final String TARGET_POS = "targetPos";
    private static final String TARGET_LEVEL = "targetLevel";
    private static final String SOURCE_POS = "sourcePos";
    private static final String SOURCE_LEVEL = "sourceLevel";
    private static final String IMMEDIATE = "immediate";
    private static final String OVERWRITE = "overwrite";
    private static final String IS_INITIAL = "isInitial";

    public static SpawnRequest load(CompoundTag tag) {
        ChunkPos targetPos = new ChunkPos(tag.getLong(TARGET_POS));
        ResourceKey<Level> targetLevel = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                Identifier.of(tag.getString(TARGET_LEVEL)));
        ChunkPos sourcePos = new ChunkPos(tag.getLong(SOURCE_POS));
        ResourceKey<Level> sourceLevel = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                Identifier.of(tag.getString(SOURCE_LEVEL)));
        boolean immediate = tag.getBoolean(IMMEDIATE);
        boolean overwrite = tag.getBoolean(OVERWRITE);
        boolean isMender = tag.getBoolean(IS_INITIAL);
        UUID playerUUID = tag.hasUUID("playerUUID") ? tag.getUUID("playerUUID") : null;
        return new SpawnRequest(targetPos, targetLevel, sourcePos, sourceLevel, immediate, overwrite, isMender, playerUUID);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(TARGET_POS, targetChunkPos.toLong());
        tag.putString(TARGET_LEVEL, targetLevel.location().toString());
        tag.putLong(SOURCE_POS, sourceChunkPos.toLong());
        tag.putString(SOURCE_LEVEL, sourceLevel.location().toString());
        tag.putBoolean(IMMEDIATE, immediate);
        tag.putBoolean(OVERWRITE, overwrite);
        tag.putBoolean(IS_INITIAL, isMenderRequest);
        if (playerUUID != null) {
            tag.putUUID("playerUUID", playerUUID);
        }
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpawnRequest that = (SpawnRequest) o;
        return targetChunkPos.equals(that.targetChunkPos)
                && targetLevel.equals(that.targetLevel)
                && sourceChunkPos.equals(that.sourceChunkPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetChunkPos, targetLevel, sourceChunkPos);
    }
}
