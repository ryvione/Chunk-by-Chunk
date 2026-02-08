/*
 * Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ClientConfigStorage {
    private static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String STORAGE_FILE = "client_server_configs.json";

    private static Path storagePath;
    private static Map<String, ServerConfigOverride> serverConfigs = new HashMap<>();

    public static class ServerConfigOverride {
        public Boolean preventFluidFlowIntoVoid;
        public Boolean autoSpawnTrees;
        public Boolean mobsDropFragments;
        public Integer fragmentDropChance;

        public void applyToConfig(ChunkByChunkConfig config) {
            if (preventFluidFlowIntoVoid != null) {
                config.setPreventFluidFlowIntoVoid(preventFluidFlowIntoVoid);
            }
            if (autoSpawnTrees != null) {
                config.setAutoSpawnTrees(autoSpawnTrees);
            }
            if (mobsDropFragments != null) {
                config.setMobsDropFragments(mobsDropFragments);
            }
            if (fragmentDropChance != null) {
                config.setFragmentDropChance(fragmentDropChance);
            }
        }
    }

    public static void init(Path gameDir) {
        storagePath = gameDir.resolve("config").resolve("GatheringChunks").resolve(STORAGE_FILE);
        try {
            Files.createDirectories(storagePath.getParent());
            loadFromDisk();
            LOGGER.info("[ClientConfigStorage] Initialized at: {}", storagePath);
        } catch (IOException e) {
            LOGGER.error("[ClientConfigStorage] Failed to initialize", e);
        }
    }


    public static String getServerIdFromConnection(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            return "singleplayer";
        }
        return serverAddress.toLowerCase();
    }


    public static void rememberServerConfig(String serverId, ChunkByChunkConfig config) {
        ServerConfigOverride override = new ServerConfigOverride();
        override.preventFluidFlowIntoVoid = config.isPreventFluidFlowIntoVoid();
        override.autoSpawnTrees = config.isAutoSpawnTrees();
        override.mobsDropFragments = config.isMobsDropFragments();
        override.fragmentDropChance = config.getFragmentDropChance();

        serverConfigs.put(serverId, override);
        saveToDisk();

        LOGGER.info("[ClientConfigStorage] Remembered config for server: {}", serverId);
    }


    public static void applyServerConfig(String serverId, ChunkByChunkConfig config) {
        ServerConfigOverride override = serverConfigs.get(serverId);
        if (override != null) {
            override.applyToConfig(config);
            LOGGER.info("[ClientConfigStorage] Applied remembered config for server: {}", serverId);
        } else {
            LOGGER.info("[ClientConfigStorage] No remembered config for server: {}", serverId);
        }
    }

    public static boolean hasConfigForServer(String serverId) {
        return serverConfigs.containsKey(serverId);
    }

    private static void loadFromDisk() {
        if (!Files.exists(storagePath)) {
            LOGGER.info("[ClientConfigStorage] No storage file found, starting fresh");
            return;
        }

        try {
            String json = Files.readString(storagePath);
            @SuppressWarnings("unchecked")
            Map<String, ServerConfigOverride> loaded = GSON.fromJson(json, Map.class);
            if (loaded != null) {
                serverConfigs = loaded;
                LOGGER.info("[ClientConfigStorage] Loaded configs for {} servers", serverConfigs.size());
            }
        } catch (IOException e) {
            LOGGER.error("[ClientConfigStorage] Failed to load from disk", e);
        }
    }

    private static void saveToDisk() {
        try {
            String json = GSON.toJson(serverConfigs);
            Files.writeString(storagePath, json);
            LOGGER.debug("[ClientConfigStorage] Saved to disk");
        } catch (IOException e) {
            LOGGER.error("[ClientConfigStorage] Failed to save to disk", e);
        }
    }
}