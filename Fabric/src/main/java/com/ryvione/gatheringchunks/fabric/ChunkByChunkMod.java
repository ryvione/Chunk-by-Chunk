/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.fabric;

import com.ryvione.gatheringchunks.common.CommonEventHandler;
import com.ryvione.gatheringchunks.common.CommonRegistry;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.config.system.ConfigSystem;
import com.ryvione.gatheringchunks.server.ChunkBoundaryEnforcer;
import com.ryvione.gatheringchunks.server.MobLootHandler;
import com.ryvione.gatheringchunks.server.ServerEventHandler;
import com.ryvione.gatheringchunks.server.commands.ChestsCommand;
import com.ryvione.gatheringchunks.server.commands.GatheringChunksCommand;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;

public class ChunkByChunkMod implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Fabric mod initializing");
        CommonRegistry.registerAll();

        ServerLifecycleEvents.SERVER_STARTED.register(ServerEventHandler::onServerStarted);
        ServerLifecycleEvents.SERVER_STARTING.register(ServerEventHandler::onServerStarting);
        ServerTickEvents.END_SERVER_TICK.register(ServerEventHandler::onLevelTick);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ChunkBoundaryEnforcer.checkPlayerBoundaries(player);
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            MobLootHandler.onMobDeath(entity, entity.level());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            GatheringChunksCommand.register(dispatcher);
            ChestsCommand.register(dispatcher);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!ChunkByChunkConfig.get().getGeneration().isEnabled()) return;

            ServerLevel level = newPlayer.serverLevel();
            if (!level.dimension().equals(Level.OVERWORLD)) return;

            if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator) {
                BlockPos spawnPos = level.getSharedSpawnPos();
                ChunkPos spawnChunk = new ChunkPos(spawnPos);

                LOGGER.debug("Forcing respawn to spawn chunk [{},{}]", spawnChunk.x, spawnChunk.z);

                newPlayer.teleportTo(
                        level,
                        spawnChunk.getMiddleBlockX() + 0.5,
                        spawnPos.getY(),
                        spawnChunk.getMiddleBlockZ() + 0.5,
                        newPlayer.getYRot(),
                        newPlayer.getXRot()
                );
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!ChunkByChunkConfig.get().getGeneration().isEnabled()) return;

            ServerPlayer player = handler.getPlayer();
            ServerLevel level = player.serverLevel();
            if (!level.dimension().equals(Level.OVERWORLD)) return;

            if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator) {
                BlockPos spawnPos = level.getSharedSpawnPos();
                ChunkPos spawnChunk = new ChunkPos(spawnPos);
                ChunkPos playerChunk = new ChunkPos(player.blockPosition());

                if (playerChunk.x != spawnChunk.x || playerChunk.z != spawnChunk.z) {
                    LOGGER.info("Correcting initial spawn from chunk [{},{}] to spawn chunk [{},{}]",
                            playerChunk.x, playerChunk.z, spawnChunk.x, spawnChunk.z);

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
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos pos = hitResult.getBlockPos();
            BlockPos placePos = pos.relative(hitResult.getDirection());
            if (!CommonEventHandler.isBlockPlacementAllowed(placePos, player, world)) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(GatheringChunksConstants.MOD_ID, "server_data");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                ServerEventHandler.onResourceManagerReload(resourceManager);
            }
        });

        new ConfigSystem().synchConfig(Paths.get("defaultconfigs", GatheringChunksConstants.MOD_ID + ".toml"), ChunkByChunkConfig.get());
    }
}