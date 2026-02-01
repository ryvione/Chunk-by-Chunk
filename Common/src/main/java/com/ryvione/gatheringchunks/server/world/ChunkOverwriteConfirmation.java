package com.ryvione.gatheringchunks.server.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks pending chunk overwrite confirmations from players.
 * When a player tries to spawn a chunk on an occupied location, they must confirm the action.
 */
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

    /**
     * Adds a pending overwrite confirmation for a player.
     */
    public static void addPendingOverwrite(ServerPlayer player, ChunkPos targetChunk, String biomeTheme, boolean random) {
        pendingOverwrites.put(player.getUUID(), new PendingOverwrite(targetChunk, biomeTheme, random));
    }

    /**
     * Checks if a player has a pending overwrite confirmation for the given chunk.
     * Returns the pending overwrite if it exists and is not expired, null otherwise.
     */
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

    /**
     * Removes a pending overwrite confirmation for a player.
     */
    public static void removePendingOverwrite(ServerPlayer player) {
        pendingOverwrites.remove(player.getUUID());
    }

    /**
     * Clears all expired confirmations.
     */
    public static void cleanupExpired() {
        pendingOverwrites.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
