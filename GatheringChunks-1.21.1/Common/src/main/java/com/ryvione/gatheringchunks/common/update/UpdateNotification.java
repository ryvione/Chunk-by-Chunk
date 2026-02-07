package com.ryvione.gatheringchunks.common.update;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class UpdateNotification {

    public static void sendUpdateNotification(ServerPlayer player) {
        GatheringChunksConstants.LOGGER.info("[UpdateNotification] Checking if should send notification to player: {}", player.getName().getString());
        GatheringChunksConstants.LOGGER.info("[UpdateNotification] Update available: {}", UpdateChecker.isUpdateAvailable());

        if (!UpdateChecker.isUpdateAvailable()) {
            GatheringChunksConstants.LOGGER.info("[UpdateNotification] No update available, skipping notification");
            return;
        }

        VersionInfo versionInfo = UpdateChecker.getLatestVersionInfo();
        GatheringChunksConstants.LOGGER.info("[UpdateNotification] Version info: {}", versionInfo != null ? versionInfo.getVersion() : "null");

        if (versionInfo == null) {
            GatheringChunksConstants.LOGGER.warn("[UpdateNotification] Version info is null, cannot send notification");
            return;
        }

        GatheringChunksConstants.LOGGER.info("[UpdateNotification] Sending update notification to player: {}", player.getName().getString());
        GatheringChunksConstants.LOGGER.info("[UpdateNotification] Current: {} | Latest: {} | IsBeta: {}",
                UpdateChecker.getCurrentVersion(), versionInfo.getVersion(), versionInfo.isBeta());

        player.sendSystemMessage(Component.literal("========================================").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("Gathering Chunks Update Available!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("Current Version: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(UpdateChecker.getCurrentVersion()).withStyle(ChatFormatting.RED)));
        player.sendSystemMessage(Component.literal("Latest Version: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(versionInfo.getVersion()).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(versionInfo.isBeta() ? " (Beta)" : "").withStyle(ChatFormatting.AQUA)));

        if (!versionInfo.getChangelog().isEmpty()) {
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("Changelog:").withStyle(ChatFormatting.GOLD));
            String[] changelogLines = versionInfo.getChangelog().split("\n");
            for (String line : changelogLines) {
                if (line.trim().isEmpty()) continue;
                player.sendSystemMessage(Component.literal("  " + line).withStyle(ChatFormatting.WHITE));
            }
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("Please update to the latest version!").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("========================================").withStyle(ChatFormatting.GOLD));

        GatheringChunksConstants.LOGGER.info("[UpdateNotification] Notification sent successfully");
    }
}