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

                int safeY = spawnPos.getY();
                LevelChunk spawnLevelChunk = level.getChunkAt(spawnChunk.getMiddleBlockPosition(0));
                if (spawnLevelChunk != null) {
                    safeY = ChunkUtil.getSafeSpawnHeight(spawnLevelChunk, spawnChunk.getMiddleBlockX(), spawnChunk.getMiddleBlockZ());
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
            ServerLevel level = player.serverLevel();
            if (!level.dimension().equals(Level.OVERWORLD))
                return;

            if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator) {
                boolean isFirstJoin = !INITIAL_SPAWNED_PLAYERS.contains(player.getUUID());

                if (isFirstJoin) {
                    BlockPos playerPos = player.blockPosition();
                    ChunkPos playerChunk = new ChunkPos(playerPos);

                    if (SpawnChunkHelper.isEmptyChunk(level, playerChunk)) {
                        BlockPos spawnPos = level.getSharedSpawnPos();
                        ChunkPos spawnChunk = new ChunkPos(spawnPos);

                        LOGGER.info("First join: Correcting spawn from empty chunk [{},{}] to spawn chunk [{},{}]",
                                playerChunk.x, playerChunk.z, spawnChunk.x, spawnChunk.z);

                        int safeY = spawnPos.getY();
                        LevelChunk spawnLevelChunk = level.getChunkAt(spawnChunk.getMiddleBlockPosition(0));
                        if (spawnLevelChunk != null) {
                            safeY = ChunkUtil.getSafeSpawnHeight(spawnLevelChunk, spawnChunk.getMiddleBlockX(), spawnChunk.getMiddleBlockZ());
                        }

                        player.teleportTo(
                                level,
                                spawnChunk.getMiddleBlockX() + 0.5,
                                safeY,
                                spawnChunk.getMiddleBlockZ() + 0.5,
                                player.getYRot(),
                                player.getXRot());
                    }

                    INITIAL_SPAWNED_PLAYERS.add(player.getUUID());
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
}