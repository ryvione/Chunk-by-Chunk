/*
 * Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.server;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.interop.Services;
import com.ryvione.gatheringchunks.server.world.ChestTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerProgressionHelper {

    private static final Map<UUID, Long> lastCheckTime = new HashMap<>();
    private static final Map<UUID, Boolean> hasBeenWarned = new HashMap<>();
    private static final long CHECK_INTERVAL = 6000; // 5 minutes
    private static final long GRACE_PERIOD = 12000;  // 10 minutes
    private static final int SEARCH_RADIUS = 32;

    public static void checkPlayers(MinecraftServer server) {
        if (!ChunkByChunkConfig.get().getGatheringChunksConfig().isEnableProgressionHelper()) {
            return;
        }

        long currentTime = server.overworld().getGameTime();

        if (currentTime < GRACE_PERIOD) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Long lastCheck = lastCheckTime.get(player.getUUID());
            if (lastCheck == null || currentTime - lastCheck >= CHECK_INTERVAL) {
                checkPlayerProgression(player, currentTime, server);
                lastCheckTime.put(player.getUUID(), currentTime);
            }
        }
    }

    private static void checkPlayerProgression(ServerPlayer player, long currentTime, MinecraftServer server) {
        Item chunkSpawnerItem = Services.PLATFORM.spawnChunkBlockItem();
        Item worldCoreItem = Services.PLATFORM.worldCoreBlockItem();
        Item worldCrystalItem = Services.PLATFORM.worldCrystalItem();
        Item worldShardItem = Services.PLATFORM.worldShardItem();
        Item worldFragmentItem = Services.PLATFORM.worldFragmentItem();

        int chunkSpawners = 0;
        int worldCores = 0;
        int worldCrystals = 0;
        int worldShards = 0;
        int worldFragments = 0;

        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(chunkSpawnerItem)) {
                chunkSpawners += stack.getCount();
            } else if (stack.is(worldCoreItem)) {
                worldCores += stack.getCount();
            } else if (stack.is(worldCrystalItem)) {
                worldCrystals += stack.getCount();
            } else if (stack.is(worldShardItem)) {
                worldShards += stack.getCount();
            } else if (stack.is(worldFragmentItem)) {
                worldFragments += stack.getCount();
            }
        }

        ChestTracker tracker = ChestTracker.get(server);
        Set<BlockPos> trackedChests = tracker.getChestPositions();

        BlockPos playerPos = player.blockPosition();

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);

                    if (trackedChests.contains(checkPos)) {
                        continue;
                    }

                    BlockEntity be = player.serverLevel().getBlockEntity(checkPos);

                    if (be instanceof ChestBlockEntity chest) {
                        for (int i = 0; i < chest.getContainerSize(); i++) {
                            ItemStack stack = chest.getItem(i);
                            if (stack.isEmpty()) continue;

                            if (stack.is(chunkSpawnerItem)) {
                                chunkSpawners += stack.getCount();
                            } else if (stack.is(worldCoreItem)) {
                                worldCores += stack.getCount();
                            } else if (stack.is(worldCrystalItem)) {
                                worldCrystals += stack.getCount();
                            } else if (stack.is(worldShardItem)) {
                                worldShards += stack.getCount();
                            } else if (stack.is(worldFragmentItem)) {
                                worldFragments += stack.getCount();
                            }
                        }
                    }
                }
            }
        }

        if (chunkSpawners > 0) {
            hasBeenWarned.put(player.getUUID(), false);
            return;
        }


        int totalFragmentEquivalents = worldFragments + (worldShards * 4) + (worldCrystals * 16) + (worldCores * 64);

        GatheringChunksConstants.LOGGER.debug("Player {} progression check: Spawners={}, Cores={}, Crystals={}, Shards={}, Fragments={}, Total Equivalents={}",
                player.getName().getString(), chunkSpawners, worldCores, worldCrystals, worldShards, worldFragments, totalFragmentEquivalents);

        if (totalFragmentEquivalents < 64) {
            Boolean warned = hasBeenWarned.get(player.getUUID());
            if (warned != null && warned) {
                return;
            }

            GatheringChunksConstants.LOGGER.info("Player {} is stuck without progression materials - giving chunk spawner", player.getName().getString());

            player.sendSystemMessage(Component.literal("§c§l[Progression Helper]"));
            player.sendSystemMessage(Component.literal("§eIt looks like you can't craft a chunk spawner!"));
            player.sendSystemMessage(Component.literal("§7You need §664 fragment equivalents §7to craft a §bWorld Core§7."));
            player.sendSystemMessage(Component.literal("§7You currently have: §a" + totalFragmentEquivalents + " §7fragment equivalents."));
            player.sendSystemMessage(Component.literal("§6Giving you a chunk spawner to help you progress!"));

            ItemStack spawnerStack = new ItemStack(chunkSpawnerItem, 1);
            if (!player.addItem(spawnerStack)) {
                player.drop(spawnerStack, false);
            }

            hasBeenWarned.put(player.getUUID(), true);
        } else {
            hasBeenWarned.put(player.getUUID(), false);
        }
    }
}