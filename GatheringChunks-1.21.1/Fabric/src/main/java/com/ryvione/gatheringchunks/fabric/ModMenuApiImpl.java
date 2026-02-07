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
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.fabric.mixin.screen.ScreenAccessor;
import net.minecraft.client.Minecraft;
import com.ryvione.gatheringchunks.client.screens.GatheringChunksConfigScreen;
public class ModMenuApiImpl implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return lastScreen -> {
            Minecraft minecraft = ((ScreenAccessor) lastScreen).getClient();
            if (minecraft.isLocalServer() || minecraft.getConnection() != null) {
                return null;
            } else {
                return new GatheringChunksConfigScreen(lastScreen);
            }
        };
    }
}
