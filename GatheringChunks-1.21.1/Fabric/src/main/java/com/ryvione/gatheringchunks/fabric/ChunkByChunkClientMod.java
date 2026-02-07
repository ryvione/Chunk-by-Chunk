package com.ryvione.gatheringchunks.fabric;

import com.ryvione.gatheringchunks.client.screens.*;
import com.ryvione.gatheringchunks.common.CommonRegistry;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.update.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class ChunkByChunkClientMod implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    @Override
    public void onInitializeClient() {
        LOGGER.info("Client Initializing");
        MenuScreens.register(CommonRegistry.BEDROCK_CHEST_MENU, BedrockChestScreen::new);
        MenuScreens.register(CommonRegistry.WORLD_FORGE_MENU, WorldForgeScreen::new);
        MenuScreens.register(CommonRegistry.WORLD_SCANNER_MENU, WorldScannerScreen::new);
        MenuScreens.register(CommonRegistry.WORLD_MENDER_MENU, WorldMenderScreen::new);
        MenuScreens.register(CommonRegistry.CHUNK_ENGINE_MENU, ChunkEngineScreen::new);
        LOGGER.debug("Client networking disabled (API changed in 1.21.1)");

        LOGGER.info("Checking for mod updates...");
        UpdateChecker.checkForUpdates();
    }
}