package com.ryvione.gatheringchunks.common.update;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class UpdateNotification {

    public static void sendUpdateNotification(ServerPlayer player) {
        if (!UpdateChecker.isUpdateAvailable()) {
            return;
        }

        VersionInfo versionInfo = UpdateChecker.getLatestVersionInfo();
        if (versionInfo == null) {
            return;
        }

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
    }
}