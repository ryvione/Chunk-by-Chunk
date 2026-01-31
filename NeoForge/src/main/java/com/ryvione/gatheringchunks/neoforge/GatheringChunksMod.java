package com.ryvione.gatheringchunks.neoforge;

import com.ryvione.gatheringchunks.client.screens.*;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(GatheringChunksConstants.MOD_ID)
public class GatheringChunksMod {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, GatheringChunksConstants.MOD_ID);

    public static final Supplier<MapCodec<? extends ChunkGenerator>> SKY_CHUNK_CODEC =
            CHUNK_GENERATORS.register("sky_chunk_generator",
                    () -> SkyChunkGenerator.CODEC);

    public GatheringChunksMod(IEventBus modEventBus) {
        GatheringChunksConstants.LOGGER.info("Gathering Chunks (NeoForge) initializing...");

        CHUNK_GENERATORS.register(modEventBus);

        ModRegistry.BLOCKS.register(modEventBus);
        ModRegistry.ITEMS.register(modEventBus);
        ModRegistry.BLOCK_ENTITIES.register(modEventBus);
        ModRegistry.MENU_TYPES.register(modEventBus);
        ModRegistry.SOUND_EVENTS.register(modEventBus);
        ModRegistry.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        GatheringChunksConstants.LOGGER.info("Common setup complete");
    }

    @EventBusSubscriber(modid = GatheringChunksConstants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            GatheringChunksConstants.LOGGER.info("Client Initializing");
            event.register(ModRegistry.BEDROCK_CHEST_MENU.get(), BedrockChestScreen::new);
            event.register(ModRegistry.WORLD_FORGE_MENU.get(), WorldForgeScreen::new);
            event.register(ModRegistry.WORLD_SCANNER_MENU.get(), WorldScannerScreen::new);
            event.register(ModRegistry.WORLD_MENDER_MENU.get(), WorldMenderScreen::new);
        }
    }
}