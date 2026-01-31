/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.mixins;

import com.mojang.datafixers.DataFixer;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.server.world.ChunkSpawnController;
import com.ryvione.gatheringchunks.server.world.ControllableChunkMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
@Mixin(targets = "net.minecraft.server.level.ChunkMap")
public abstract class ChunkMapMixin extends ChunkStorage implements ControllableChunkMap {
    public ChunkMapMixin(RegionStorageInfo $$0, Path $$1, DataFixer $$2, boolean $$3) {
        super($$0, $$1, $$2, $$3);
    }
    @Final
    @Shadow
    ServerLevel level;
    @Shadow
    protected abstract Iterable<ChunkHolder> getChunks();
    @Shadow
    @Nullable
    protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);
    public void forceReloadChunk(ChunkPos chunkPos) {
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(chunkPos.toLong());
        if (chunkHolder != null) {
            ChunkMap thisMap = (ChunkMap) (Object) this;
            List<ServerPlayer> players = thisMap.getPlayers(chunkPos, false);
            chunkHolder.getFullChunkFuture().thenAccept(chunkResult -> {
                chunkResult.ifSuccess(levelChunk -> {
                    ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                            levelChunk,
                            this.level.getLightEngine(),
                            null,
                            null
                    );
                    for (ServerPlayer player : players) {
                        player.connection.send(packet);
                    }
                });
            });
        }
    }
    @Inject(method = "onFullChunkStatusChange", at = @At("HEAD"))
    public void onFullStatusChange(ChunkPos pos, FullChunkStatus status, CallbackInfo ci) {
        if (ChunkByChunkConfig.get().getGeneration().isSpawnChunkStrip()
                && status.isOrAfter(FullChunkStatus.ENTITY_TICKING)
                && level.dimension().equals(Level.OVERWORLD)
                && new ChunkPos(level.getSharedSpawnPos()).x == pos.x) {
            BlockPos blockPos = pos.getMiddleBlockPosition(level.getMaxBuildHeight() - 1);
            ChunkSpawnController.get(level.getServer()).request(level, "", false, blockPos);
        }
    }
}
