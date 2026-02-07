package com.ryvione.gatheringchunks.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ryvione.gatheringchunks.common.update.UpdateChecker;
import com.ryvione.gatheringchunks.common.update.VersionInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class UpdateNotificationOverlay {
    public static void renderUpdateWarning(GuiGraphics guiGraphics, TitleScreen screen) {
        if (!UpdateChecker.isUpdateAvailable()) {
            return;
        }

        VersionInfo versionInfo = UpdateChecker.getLatestVersionInfo();
        if (versionInfo == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        int warningY = screenHeight - 30;

        String warningText = "Update Available: " + UpdateChecker.getFormattedLatestVersion();
        Component warningComponent = Component.literal(warningText);
        int textWidth = minecraft.font.width(warningComponent);
        int warningX = (screenWidth - textWidth) / 2;

        RenderSystem.enableBlend();
        guiGraphics.fill(warningX - 5, warningY - 2, warningX + textWidth + 5, warningY + 10, 0x88000000);
        guiGraphics.drawCenteredString(minecraft.font, warningComponent, screenWidth / 2, warningY, 0xFFFF55);
        RenderSystem.disableBlend();
    }
}