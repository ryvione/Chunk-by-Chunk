package com.ryvione.gatheringchunks.server;

import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChunkBoundaryEnforcer {

    private static final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private static final Map<UUID, Vec3> lastValidPosition = new HashMap<>();
    private static final long WARNING_COOLDOWN = 100;

    public static void checkPlayerBoundaries(ServerPlayer player) {
        if (!ChunkByChunkConfig.get().getGatheringChunksConfig().isEnforceChunkBoundaries()) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        ChunkPos currentChunk = new ChunkPos(playerPos);

        if (SpawnChunkHelper.isEmptyChunk(player.serverLevel(), currentChunk)) {
            Vec3 lastValid = lastValidPosition.get(player.getUUID());
            if (lastValid != null) {
                player.teleportTo(lastValid.x, lastValid.y, lastValid.z);
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0;
            } else {
                BlockPos respawn = player.getRespawnPosition();
                if (respawn != null) {
                    player.teleportTo(respawn.getX() + 0.5, respawn.getY(), respawn.getZ() + 0.5);
                    player.setDeltaMovement(Vec3.ZERO);
                    player.fallDistance = 0;
                }
            }

            long currentTime = player.level().getGameTime();
            Long lastWarning = lastWarningTime.get(player.getUUID());

            if (lastWarning == null || currentTime - lastWarning >= WARNING_COOLDOWN) {
                player.sendSystemMessage(Component.literal("§c§lYou cannot leave spawned chunks in Hard Mode!"));
                player.sendSystemMessage(Component.literal("§eSpawn more chunks using a Chunk Spawner."));
                lastWarningTime.put(player.getUUID(), currentTime);
            }
        } else {
            lastValidPosition.put(player.getUUID(), player.position());
        }
    }
}