package com.ryvione.gatheringchunks.server.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ChunkOverwriteConfirmation {
    private static final Map<UUID, PendingOverwrite> pendingOverwrites = new HashMap<>();
    private static final long CONFIRMATION_TIMEOUT_MS = 30000; // 30 seconds

    public static class PendingOverwrite {
        public final ChunkPos targetChunk;
        public final String biomeTheme;
        public final boolean random;
        public final long timestamp;

        public PendingOverwrite(ChunkPos targetChunk, String biomeTheme, boolean random) {
            this.targetChunk = targetChunk;
            this.biomeTheme = biomeTheme;
            this.random = random;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CONFIRMATION_TIMEOUT_MS;
        }
    }


    public static void addPendingOverwrite(ServerPlayer player, ChunkPos targetChunk, String biomeTheme, boolean random) {
        pendingOverwrites.put(player.getUUID(), new PendingOverwrite(targetChunk, biomeTheme, random));
    }


    public static PendingOverwrite getPendingOverwrite(ServerPlayer player, ChunkPos targetChunk) {
        UUID playerId = player.getUUID();
        PendingOverwrite pending = pendingOverwrites.get(playerId);
        
        if (pending != null) {
            if (pending.isExpired()) {
                pendingOverwrites.remove(playerId);
                return null;
            }
            if (pending.targetChunk.equals(targetChunk)) {
                return pending;
            }
        }
        return null;
    }


    public static void removePendingOverwrite(ServerPlayer player) {
        pendingOverwrites.remove(player.getUUID());
    }

    public static void cleanupExpired() {
        pendingOverwrites.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
