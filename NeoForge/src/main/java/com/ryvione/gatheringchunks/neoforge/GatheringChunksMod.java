/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.neoforge;

import com.ryvione.gatheringchunks.client.screens.*;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.network.S2COpenConfigPacket;
import com.ryvione.gatheringchunks.common.network.S2CSyncConfigPacket;
import com.ryvione.gatheringchunks.client.ClientConfigStorage;
import com.ryvione.gatheringchunks.config.system.ConfigSystem;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

@Mod(GatheringChunksConstants.MOD_ID)
public class GatheringChunksMod {

    private static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, GatheringChunksConstants.MOD_ID);

    public static final Supplier<MapCodec<? extends ChunkGenerator>> SKY_CHUNK_CODEC =
            CHUNK_GENERATORS.register("sky_chunk_generator",
                    () -> SkyChunkGenerator.CODEC);

    public GatheringChunksMod(IEventBus modEventBus) {
        LOGGER.info("Gathering Chunks (NeoForge) initializing...");

        ConfigSystem.initCentralConfigDir(FMLPaths.GAMEDIR.get());
        LOGGER.info("[GatheringChunksMod] Centralized config directory initialized");

        CHUNK_GENERATORS.register(modEventBus);

        ModRegistry.BLOCKS.register(modEventBus);
        ModRegistry.ITEMS.register(modEventBus);
        ModRegistry.BLOCK_ENTITIES.register(modEventBus);
        ModRegistry.MENU_TYPES.register(modEventBus);
        ModRegistry.SOUND_EVENTS.register(modEventBus);
        ModRegistry.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Common setup complete");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(GatheringChunksConstants.MOD_ID).versioned("1.0");
        registrar.playToClient(
                S2COpenConfigPacket.TYPE,
                S2COpenConfigPacket.CODEC,
                ClientPacketHandler::handleOpenConfig);
        registrar.playToClient(
                S2CSyncConfigPacket.TYPE,
                S2CSyncConfigPacket.CODEC,
                ClientPacketHandler::handleSyncConfig);
        LOGGER.info("[GatheringChunksMod] Registered network payloads");
    }

    @EventBusSubscriber(modid = GatheringChunksConstants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ClientConfigStorage.init(FMLPaths.GAMEDIR.get());
            LOGGER.info("[GatheringChunksMod] ClientConfigStorage initialized");
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            LOGGER.info("Client Initializing");
            event.register(ModRegistry.BEDROCK_CHEST_MENU.get(), BedrockChestScreen::new);
            event.register(ModRegistry.WORLD_FORGE_MENU.get(), WorldForgeScreen::new);
            event.register(ModRegistry.WORLD_SCANNER_MENU.get(), WorldScannerScreen::new);
            event.register(ModRegistry.WORLD_MENDER_MENU.get(), WorldMenderScreen::new);
            event.register(ModRegistry.CHUNK_ENGINE_MENU.get(), ChunkEngineScreen::new);
        }
    }

    @EventBusSubscriber(modid = GatheringChunksConstants.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class ClientGameEvents {
        @SubscribeEvent
        public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            String address = event.getController() != null && event.getController().getConnection() != null
                    && event.getController().getConnection().getRemoteAddress() != null
                    ? event.getController().getConnection().getRemoteAddress().toString()
                    : "singleplayer";
            ClientConfigStorage.setCurrentServer(ClientConfigStorage.getServerIdFromConnection(address));
            LOGGER.info("[GatheringChunksMod] Joined server, set config scope: {}", address);
        }
    }
}