/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.fabric;

import com.ryvione.gatheringchunks.common.CommonEventHandler;
import com.ryvione.gatheringchunks.common.CommonRegistry;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.util.ConfigUtil;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.config.system.ConfigSystem;
import com.ryvione.gatheringchunks.server.ChunkBoundaryEnforcer;
import com.ryvione.gatheringchunks.server.MobLootHandler;
import com.ryvione.gatheringchunks.server.ServerEventHandler;
import com.ryvione.gatheringchunks.server.commands.ChestsCommand;
import com.ryvione.gatheringchunks.server.commands.GatheringChunksCommand;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import com.ryvione.gatheringchunks.common.network.S2COpenConfigPacket;
import com.ryvione.gatheringchunks.common.network.S2CSyncConfigPacket;
import com.ryvione.gatheringchunks.interop.Services;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionResult;
import com.ryvione.gatheringchunks.common.util.ChunkUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChunkByChunkMod implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    private static final Set<UUID> INITIAL_SPAWNED_PLAYERS = new HashSet<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(S2COpenConfigPacket.TYPE, S2COpenConfigPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(S2CSyncConfigPacket.TYPE, S2CSyncConfigPacket.CODEC);

        LOGGER.info("Fabric mod initializing");

        Path gameDir = FabricLoader.getInstance().getGameDir();
        ConfigSystem.initCentralConfigDir(gameDir);
        LOGGER.info("[ChunkByChunkMod] Centralized config directory initialized");

        CommonRegistry.registerAll();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            INITIAL_SPAWNED_PLAYERS.clear();
            ServerEventHandler.onServerStarted(server);
        });

        ServerLifecycleEvents.SERVER_STARTING.register(ServerEventHandler::onServerStarting);

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("[ChunkByChunkMod] Saving config before server shutdown...");
            ConfigUtil.saveDefaultConfig();
        });

        ServerTickEvents.END_SERVER_TICK.register(ServerEventHandler::onLevelTick);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ChunkBoundaryEnforcer.checkPlayerBoundaries(player);
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(com.ryvione.gatheringchunks.server.CauldronRainFiller::tick);

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            MobLootHandler.onMobDeath(entity, entity.level());
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof AbstractPiglin piglin)) return;
            if (!world.dimension().equals(Level.NETHER)) return;
            if (!(world.getChunkSource().getGenerator() instanceof SkyChunkGenerator)) return;
            CompoundTag tag = new CompoundTag();
            piglin.save(tag);
            if (tag.getByte("IsImmuneToZombification") != 1) {
                tag.putByte("IsImmuneToZombification", (byte) 1);
                piglin.load(tag);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            GatheringChunksCommand.register(dispatcher);
            ChestsCommand.register(dispatcher);
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            if (!world.dimension().equals(Level.OVERWORLD) && !world.dimension().equals(Level.NETHER)) return;
            if (!(world.getChunkSource().getGenerator() instanceof SkyChunkGenerator)) return;
            // Skip redirect for first-join players — scheduleSpawnTeleport handles them
            if (!INITIAL_SPAWNED_PLAYERS.contains(player.getUUID())) return;
            ServerEventHandler.onPlayerArrived(player, (ServerLevel) world);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!ChunkByChunkConfig.get().getGeneration().isEnabled())
                return;

            if (newPlayer.getRespawnPosition() != null)
                return;

            ServerLevel level = newPlayer.serverLevel();
            if (!level.dimension().equals(Level.OVERWORLD))
                return;

            if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator) {
                BlockPos spawnPos = level.getSharedSpawnPos();
                ChunkPos spawnChunk = new ChunkPos(spawnPos);

                LOGGER.debug("Forcing respawn to spawn chunk [{},{}]", spawnChunk.x, spawnChunk.z);

                int safeY = level.getMaxBuildHeight();
                LevelChunk spawnLevelChunk = level.getChunkAt(spawnChunk.getMiddleBlockPosition(0));
                if (spawnLevelChunk != null) {
                    int candidateY = ChunkUtil.getSafeSpawnHeight(spawnLevelChunk, spawnChunk.getMiddleBlockX(), spawnChunk.getMiddleBlockZ());
                    if (candidateY > level.getMinBuildHeight() + 10) {
                        safeY = candidateY;
                    } else {
                        safeY = spawnLevelChunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 8, 8) + 1;
                    }
                }

                newPlayer.teleportTo(
                        level,
                        spawnChunk.getMiddleBlockX() + 0.5,
                        safeY,
                        spawnChunk.getMiddleBlockZ() + 0.5,
                        newPlayer.getYRot(),
                        newPlayer.getXRot());
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ChunkByChunkConfig.get().getGeneration().isEnabled())
                return;

            ServerPlayer player = handler.getPlayer();

            try {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().create();
                String configJson = gson.toJson(ChunkByChunkConfig.get().getGatheringChunksConfig());
                com.ryvione.gatheringchunks.common.network.S2CSyncConfigPacket packet =
                        new com.ryvione.gatheringchunks.common.network.S2CSyncConfigPacket(configJson);
                Services.PLATFORM.sendConfigSyncPacket(player, packet);
                LOGGER.debug("[ChunkByChunkMod] Synced config to player {} on join", player.getName().getString());
            } catch (Exception e) {
                LOGGER.warn("[ChunkByChunkMod] Failed to sync config to player on join: {}", e.getMessage());
            }

            ServerLevel level = player.serverLevel();
            if (!level.dimension().equals(Level.OVERWORLD))
                return;

            if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator) {
                boolean isFirstJoin = !INITIAL_SPAWNED_PLAYERS.contains(player.getUUID());

                if (isFirstJoin) {
                    INITIAL_SPAWNED_PLAYERS.add(player.getUUID());

                    final int MAX_WAIT_TICKS = 600; 
                    scheduleSpawnTeleport(server, player.getUUID(), server.getTickCount(), MAX_WAIT_TICKS);
                }
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            BlockPos placePos = pos.relative(hitResult.getDirection());
            if (!CommonEventHandler.isBlockPlacementAllowed(placePos, player, world)) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        });

        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return ResourceLocation.fromNamespaceAndPath(GatheringChunksConstants.MOD_ID, "server_data");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager resourceManager) {
                        ServerEventHandler.onResourceManagerReload(resourceManager);
                    }
                });
    }

    private static void scheduleSpawnTeleport(net.minecraft.server.MinecraftServer server, UUID playerUUID,
            int startTick, int maxWaitTicks) {
        server.tell(new net.minecraft.server.TickTask(server.getTickCount() + 5, () -> {
            if (!server.isRunning()) return;

            ServerPlayer p = server.getPlayerList().getPlayer(playerUUID);
            if (p == null) return; 

            ServerLevel lvl = p.serverLevel();
            if (!lvl.dimension().equals(Level.OVERWORLD)) return;
            if (!(lvl.getChunkSource().getGenerator() instanceof SkyChunkGenerator)) return;

            BlockPos spawnPos = lvl.getSharedSpawnPos();
            ChunkPos spawnChunk = new ChunkPos(spawnPos);

            boolean chunkReady = !com.ryvione.gatheringchunks.server.world.SpawnChunkHelper.isEmptyChunk(lvl, spawnChunk);

            int ticksWaited = server.getTickCount() - startTick;

            if (!chunkReady && ticksWaited < maxWaitTicks) {
                if (ticksWaited % 20 == 0) {
                    LOGGER.info("[FirstJoin] Waiting for spawn chunk [{},{}] to be placed... ({}t elapsed)",
                            spawnChunk.x, spawnChunk.z, ticksWaited);
                }
                scheduleSpawnTeleport(server, playerUUID, startTick, maxWaitTicks);
                return;
            }

            if (!chunkReady) {
                LOGGER.warn("[FirstJoin] Spawn chunk still empty after {}t — teleporting anyway", ticksWaited);
            } else {
                LOGGER.info("[FirstJoin] Spawn chunk [{},{}] is ready after {}t — teleporting player",
                        spawnChunk.x, spawnChunk.z, ticksWaited);
            }

            int safeY;
            LevelChunk spawnLevelChunk = lvl.getChunkAt(spawnChunk.getMiddleBlockPosition(0));
            if (spawnLevelChunk != null) {
                int candidateY = com.ryvione.gatheringchunks.common.util.ChunkUtil.getSafeSpawnHeight(
                        spawnLevelChunk, spawnChunk.getMiddleBlockX(), spawnChunk.getMiddleBlockZ());
                if (candidateY > lvl.getMinBuildHeight() + 10) {
                    safeY = candidateY;
                } else {
                    safeY = spawnLevelChunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 8, 8) + 1;
                }
            } else {
                safeY = lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        spawnChunk.getMiddleBlockX(), spawnChunk.getMiddleBlockZ()) + 1;
            }

            p.teleportTo(lvl,
                    spawnChunk.getMiddleBlockX() + 0.5,
                    safeY,
                    spawnChunk.getMiddleBlockZ() + 0.5,
                    p.getYRot(),
                    p.getXRot());
        }));
    }
}