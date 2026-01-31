/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.neoforge;

import com.ryvione.gatheringchunks.common.CommonEventHandler;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.data.ScannerDataLoader;
import com.ryvione.gatheringchunks.common.data.SkyDimensionDataLoader;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.config.system.ConfigSystem;
import com.ryvione.gatheringchunks.server.ChunkBoundaryEnforcer;
import com.ryvione.gatheringchunks.server.MobLootHandler;
import com.ryvione.gatheringchunks.server.ServerEventHandler;
import com.ryvione.gatheringchunks.server.commands.ChestsCommand;
import com.ryvione.gatheringchunks.server.commands.GatheringChunksCommand;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Paths;

@EventBusSubscriber(modid = GatheringChunksConstants.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class EventHandler {

    private static final ConfigSystem configSystem = new ConfigSystem();
    private static boolean dimensionsConfigured = false;

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();

        GatheringChunksConstants.LOGGER.info("ServerAboutToStartEvent - loading config & preparing dimensions");

        configSystem.synchConfig(
                server.getWorldPath(LevelResource.ROOT)
                        .resolve("serverconfig")
                        .resolve(GatheringChunksConstants.CONFIG_FILE),
                Paths.get(GatheringChunksConstants.DEFAULT_CONFIG_PATH)
                        .resolve(GatheringChunksConstants.CONFIG_FILE),
                ChunkByChunkConfig.get()
        );

        if (ChunkByChunkConfig.get().getGeneration().isEnabled()) {
            GatheringChunksConstants.LOGGER.info("Applying sky dimension configuration EARLY (before level load)");
            ServerEventHandler.applySkyDimensionConfig(server.registryAccess());
            ServerEventHandler.applyChunkByChunkWorldGeneration(server);
        } else {
            GatheringChunksConstants.LOGGER.info("Generation disabled in config - skipping dimension override");
        }

        dimensionsConfigured = true;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        GatheringChunksConstants.LOGGER.info("ServerStartedEvent - server fully started");
        ServerEventHandler.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerEventHandler.onLevelTick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChunkBoundaryEnforcer.checkPlayerBoundaries(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        MobLootHandler.onMobDeath(event.getEntity(), event.getEntity().level());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        GatheringChunksConstants.LOGGER.debug("Registering commands");
        GatheringChunksCommand.register(event.getDispatcher());
        ChestsCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(AddReloadListenerEvent event) {
        GatheringChunksConstants.LOGGER.info("Registering data reload listeners");

        event.addListener(new ScannerDataLoader(event.getRegistryAccess()));
        event.addListener(new SkyDimensionDataLoader(event.getRegistryAccess()));
        event.addListener((PreparableReloadListener.PreparationBarrier barrier,
                           ResourceManager manager,
                           net.minecraft.util.profiling.ProfilerFiller prepProfiler,
                           net.minecraft.util.profiling.ProfilerFiller reloadProfiler,
                           java.util.concurrent.Executor bgExecutor,
                           java.util.concurrent.Executor gameExecutor) ->
                barrier.wait(null).thenRunAsync(() -> {
                    GatheringChunksConstants.LOGGER.info("Resource reload - reloading dynamic data");
                    ServerEventHandler.onResourceManagerReload(manager);
                }, gameExecutor)
        );

        GatheringChunksConstants.LOGGER.info("Data reload listeners registered");
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        BlockPos placePos = pos.relative(event.getFace());

        if (!CommonEventHandler.isBlockPlacementAllowed(placePos, event.getEntity(), event.getLevel())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ChunkByChunkConfig.get().getGeneration().isEnabled()) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            if (!level.dimension().equals(Level.OVERWORLD)) return;

            if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator) {
                BlockPos spawnPos = level.getSharedSpawnPos();
                ChunkPos spawnChunk = new ChunkPos(spawnPos);
                ChunkPos playerChunk = new ChunkPos(player.blockPosition());

                if (playerChunk.x != spawnChunk.x || playerChunk.z != spawnChunk.z) {
                    GatheringChunksConstants.LOGGER.info(
                            "Correcting initial spawn position from chunk [{},{}] to spawn chunk [{},{}]",
                            playerChunk.x, playerChunk.z, spawnChunk.x, spawnChunk.z
                    );

                    player.teleportTo(
                            level,
                            spawnChunk.getMiddleBlockX() + 0.5,
                            spawnPos.getY(),
                            spawnChunk.getMiddleBlockZ() + 0.5,
                            player.getYRot(),
                            player.getXRot()
                    );
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!ChunkByChunkConfig.get().getGeneration().isEnabled()) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            if (!level.dimension().equals(Level.OVERWORLD)) return;

            if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator) {
                BlockPos spawnPos = level.getSharedSpawnPos();
                ChunkPos spawnChunk = new ChunkPos(spawnPos);

                GatheringChunksConstants.LOGGER.debug(
                        "Forcing respawn to stay inside spawn chunk [{},{}]",
                        spawnChunk.x, spawnChunk.z
                );

                player.teleportTo(
                        level,
                        spawnChunk.getMiddleBlockX() + 0.5,
                        spawnPos.getY(),
                        spawnChunk.getMiddleBlockZ() + 0.5,
                        player.getYRot(),
                        player.getXRot()
                );
            }
        }
    }
}