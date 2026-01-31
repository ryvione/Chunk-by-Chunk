/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.fabric;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ryvione.gatheringchunks.client.screens.BedrockChestScreen;
import com.ryvione.gatheringchunks.client.screens.WorldForgeScreen;
import com.ryvione.gatheringchunks.client.screens.WorldMenderScreen;
import com.ryvione.gatheringchunks.client.screens.WorldScannerScreen;
import com.ryvione.gatheringchunks.common.CommonRegistry;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
public class ChunkByChunkClientMod implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    @Override
    public void onInitializeClient() {
        LOGGER.info("Client Initializing");
        MenuScreens.register(CommonRegistry.BEDROCK_CHEST_MENU, BedrockChestScreen::new);
        MenuScreens.register(CommonRegistry.WORLD_FORGE_MENU, WorldForgeScreen::new);
        MenuScreens.register(CommonRegistry.WORLD_SCANNER_MENU, WorldScannerScreen::new);
        MenuScreens.register(CommonRegistry.WORLD_MENDER_MENU, WorldMenderScreen::new);
        LOGGER.debug("Client networking disabled (API changed in 1.21.1)");
    }
}
