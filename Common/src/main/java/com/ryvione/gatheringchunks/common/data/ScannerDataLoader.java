/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Loads scanner data from data packs
 */
public class ScannerDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DIRECTORY = "scanner_data";

    private final RegistryAccess registryAccess;

    public ScannerDataLoader(RegistryAccess registryAccess) {
        super(GSON, DIRECTORY);
        this.registryAccess = registryAccess;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        GatheringChunksConstants.LOGGER.info("Loading scanner data configs...");
        int loaded = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                ScannerDataJson data = GSON.fromJson(json, ScannerDataJson.class);

                if (data == null || data.inputItems == null || data.targetBlocks == null) {
                    GatheringChunksConstants.LOGGER.error("Invalid scanner data format for {}", id);
                    continue;
                }

                java.util.List<String> processedInputs = new java.util.ArrayList<>();
                for (String item : data.inputItems) {
                    processedInputs.add(item.contains(":") ? item : "minecraft:" + item);
                }

                java.util.List<String> processedBlocks = new java.util.ArrayList<>();
                for (String block : data.targetBlocks) {
                    processedBlocks.add(block.contains(":") ? block : "minecraft:" + block);
                }

                ScannerData scannerData = new ScannerData(processedInputs, processedBlocks);
                scannerData.process(id, registryAccess);
                loaded++;

                GatheringChunksConstants.LOGGER.debug("Loaded scanner data: {}", id);

            } catch (Exception e) {
                GatheringChunksConstants.LOGGER.error("Failed to load scanner data {}: {}", id, e.getMessage(), e);
            }
        }

        GatheringChunksConstants.LOGGER.info("Loaded {} scanner data configs", loaded);
    }

    /**
     * Helper class for JSON deserialization
     */
    private static class ScannerDataJson {
        public java.util.List<String> inputItems;
        public java.util.List<String> targetBlocks;
    }
}