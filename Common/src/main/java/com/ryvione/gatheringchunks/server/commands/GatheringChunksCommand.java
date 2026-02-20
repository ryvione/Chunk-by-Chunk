/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.server.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.network.S2COpenConfigPacket;
import com.ryvione.gatheringchunks.common.network.S2CSyncConfigPacket;
import com.ryvione.gatheringchunks.common.util.ConfigUtil;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.interop.Services;
import com.ryvione.gatheringchunks.server.world.ChunkSpawnController;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class GatheringChunksCommand {

    private static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    private static final Gson GSON = new GsonBuilder().create();

    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(
            Component.translatable("commands.gatheringchunks.spawnchunk.invalidPosition"));
    private static final SimpleCommandExceptionType INVALID_LEVEL = new SimpleCommandExceptionType(
            Component.translatable("commands.gatheringchunks.spawnchunk.invalidlevel"));
    private static final SimpleCommandExceptionType INVALID_THEME = new SimpleCommandExceptionType(
            Component.translatable("commands.gatheringchunks.spawnchunk.invalidtheme"));
    private static final SimpleCommandExceptionType NON_EMPTY_CHUNK = new SimpleCommandExceptionType(
            Component.translatable("commands.gatheringchunks.spawnchunk.nonemptychunk"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gatheringchunks")
                .then(Commands.literal("help")
                        .executes(GatheringChunksCommand::showHelp)
                        .then(Commands.argument("topic", StringArgumentType.word())
                                .suggests(new HelpTopicSuggestionProvider())
                                .executes(GatheringChunksCommand::showSpecificHelp)))

                .then(Commands.literal("spawnChunk")
                        .requires(x -> x.hasPermission(2))
                        .then(Commands.argument("location", Vec3Argument.vec3())
                                .executes((cmd) -> spawnChunk(cmd.getSource(), cmd.getSource().getLevel(),
                                        Vec3Argument.getCoordinates(cmd, "location"), false))))

                .then(Commands.literal("spawnRandomChunk")
                        .requires(x -> x.hasPermission(2))
                        .then(Commands.argument("location", Vec3Argument.vec3())
                                .executes((cmd) -> spawnChunk(cmd.getSource(), cmd.getSource().getLevel(),
                                        Vec3Argument.getCoordinates(cmd, "location"), true))))

                .then(Commands.literal("spawnThemedChunk")
                        .requires(x -> x.hasPermission(2))
                        .then(Commands.argument("theme", StringArgumentType.word())
                                .suggests(new BiomeThemeSuggestionProvider())
                                .then(Commands.argument("location", Vec3Argument.vec3())
                                        .executes((cmd) -> spawnThemedChunk(cmd.getSource(), cmd.getSource().getLevel(),
                                                StringArgumentType.getString(cmd, "theme"),
                                                Vec3Argument.getCoordinates(cmd, "location"))))))

                .then(Commands.literal("config")
                        .requires(x -> x.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(GatheringChunksCommand::reloadConfig))
                        .then(Commands.literal("modify")
                                .executes(GatheringChunksCommand::modifyConfig))));
    }

    private static class BiomeThemeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                             SuggestionsBuilder builder) {
            GatheringChunksConstants.BIOME_THEMES.forEach(builder::suggest);
            return builder.buildFuture();
        }
    }

    private static class HelpTopicSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                             SuggestionsBuilder builder) {
            builder.suggest("tree");
            builder.suggest("selflocked");
            builder.suggest("commands");
            return builder.buildFuture();
        }
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("§6===== Gathering Chunks Help ====="), false);
        source.sendSuccess(() -> Component.literal("§e/gatheringchunks help §7- Show this help"), false);
        source.sendSuccess(() -> Component.literal("§e/gatheringchunks help tree §7- Tree spawning help"), false);
        source.sendSuccess(() -> Component.literal("§e/gatheringchunks help selflocked §7- Self-locked help"), false);
        source.sendSuccess(() -> Component.literal("§e/gatheringchunks spawnChunk <pos> §7- Spawn a chunk (OP)"),
                false);
        source.sendSuccess(
                () -> Component.literal("§e/gatheringchunks spawnRandomChunk <pos> §7- Spawn random chunk (OP)"),
                false);
        source.sendSuccess(
                () -> Component
                        .literal("§e/gatheringchunks spawnThemedChunk <theme> <pos> §7- Spawn themed chunk (OP)"),
                false);
        source.sendSuccess(() -> Component.literal("§e/gatheringchunks config reload §7- Reload config from disk (OP)"),
                false);
        source.sendSuccess(() -> Component.literal("§e/gatheringchunks config modify §7- Open config menu UI (OP)"),
                false);
        source.sendSuccess(() -> Component.literal("§e/chests §7- List nearby chests"), false);
        source.sendSuccess(() -> Component.literal("§e/chests tracker enable/disable §7- Toggle chest notifications"),
                false);
        return 1;
    }

    private static int showSpecificHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String topic = StringArgumentType.getString(context, "topic").toLowerCase();

        switch (topic) {
            case "tree" -> {
                source.sendSuccess(() -> Component.literal("§6===== Tree Spawning Help ====="), false);
                source.sendSuccess(() -> Component.literal("§7Trees automatically spawn in chunks without wood."),
                        false);
                source.sendSuccess(() -> Component.literal("§7This can be toggled in the config menu (pause menu)."),
                        false);
                source.sendSuccess(
                        () -> Component.literal("§7If disabled, you may need to craft saplings or find villages."),
                        false);
                return 1;
            }
            case "selflocked" -> {
                source.sendSuccess(() -> Component.literal("§6===== Self-Locked Help ====="), false);
                source.sendSuccess(() -> Component.literal("§7If you're stuck without resources:"), false);
                source.sendSuccess(() -> Component.literal("§71. §eProgression Helper §7will give you a chunk spawner"),
                        false);
                source.sendSuccess(() -> Component.literal("§72. Enable in config if disabled"), false);
                source.sendSuccess(() -> Component.literal("§73. Check nearby chests with §e/chests"), false);
                source.sendSuccess(() -> Component.literal("§74. Break blocks to get fragments from mobs"), false);
                return 1;
            }
            case "commands" -> {
                return showHelp(context);
            }
            default -> {
                source.sendFailure(Component.literal("§cUnknown help topic: " + topic));
                source.sendSuccess(() -> Component.literal("§7Available topics: tree, selflocked, commands"), false);
                return 0;
            }
        }
    }

    private static int spawnChunk(CommandSourceStack stack, ServerLevel level, Coordinates specifiedCoords,
                                  boolean random) throws CommandSyntaxException {
        Vec3 vec3 = specifiedCoords.getPosition(stack);
        BlockPos pos = new BlockPos((int) vec3.x, level.getMaxBuildHeight() - 1, (int) vec3.z);
        ChunkPos chunkPos = new ChunkPos(pos);

        if (!(level.getChunkSource().getGenerator() instanceof SkyChunkGenerator)) {
            throw INVALID_LEVEL.create();
        }

        if (!Level.isInSpawnableBounds(pos)) {
            throw INVALID_POSITION.create();
        }

        if (!SpawnChunkHelper.isEmptyChunk(level, chunkPos)) {
            throw NON_EMPTY_CHUNK.create();
        }

        ChunkSpawnController.get(level.getServer()).request(level, "", random, pos);
        return 1;
    }

    private static int spawnThemedChunk(CommandSourceStack stack, ServerLevel level, String biome,
                                        Coordinates specifiedCoords) throws CommandSyntaxException {
        Vec3 vec3 = specifiedCoords.getPosition(stack);
        BlockPos pos = new BlockPos((int) vec3.x, level.getMaxBuildHeight() - 1, (int) vec3.z);
        ChunkPos chunkPos = new ChunkPos(pos);

        if (level.getChunkSource().getGenerator() instanceof SkyChunkGenerator skyChunkGenerator) {
            ResourceKey<Level> biomeDimension = skyChunkGenerator.getBiomeDimension(biome);
            if (biomeDimension == null) {
                throw INVALID_THEME.create();
            }

            if (!Level.isInSpawnableBounds(pos)) {
                throw INVALID_POSITION.create();
            }

            if (!SpawnChunkHelper.isEmptyChunk(level, chunkPos)) {
                throw NON_EMPTY_CHUNK.create();
            }

            ServerLevel sourceLevel = level.getServer().getLevel(biomeDimension);
            if (sourceLevel == null) {
                throw INVALID_THEME.create();
            }

            ChunkSpawnController.get(level.getServer()).request(level, biome, false, pos);
            return 1;
        } else {
            throw INVALID_LEVEL.create();
        }
    }


    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        LOGGER.info("[Command] Config reload requested by {}", context.getSource().getTextName());
        context.getSource().sendSuccess(() -> Component.literal("§e[Gathering Chunks] Reloading config..."), true);

        try {
            ConfigUtil.reloadConfig();
            LOGGER.info("[Command] Config successfully reloaded!");
            context.getSource().sendSuccess(() -> Component.literal("§a[Gathering Chunks] Config reloaded successfully!"), true);

            try {
                syncConfigToAllClients(context.getSource().getServer());
            } catch (Exception syncError) {
                LOGGER.error("[Command] Failed to sync config to clients, but config was reloaded on server", syncError);
                context.getSource().sendSuccess(() -> Component.literal("§6[Gathering Chunks] Config reloaded but client sync had errors. Check logs."), true);
                return 1;
            }

            return 1;
        } catch (Exception e) {
            LOGGER.error("[Command] Failed to reload config", e);
            context.getSource().sendFailure(Component.literal("§c[Gathering Chunks] Failed to reload config: " + e.getMessage()));
            return 0;
        }
    }

    private static void syncConfigToAllClients(net.minecraft.server.MinecraftServer server) throws Exception {
        try {
            String configJson = GSON.toJson(ChunkByChunkConfig.get().getGatheringChunksConfig());
            if (configJson == null || configJson.isEmpty()) {
                throw new RuntimeException("Failed to serialize config to JSON");
            }
            
            S2CSyncConfigPacket packet = new S2CSyncConfigPacket(configJson);

            int syncCount = 0;
            int failCount = 0;
            
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                try {
                    Services.PLATFORM.sendConfigSyncPacket(player, packet);
                    syncCount++;
                } catch (Exception playerSyncError) {
                    LOGGER.warn("[Command] Failed to sync config to player {}: {}", player.getName().getString(), playerSyncError.getMessage());
                    failCount++;
                }
            }

            LOGGER.info("[Command] Config synced to {}/{} online players (failed: {})", 
                    syncCount, server.getPlayerList().getPlayerCount(), failCount);
            
            if (failCount > 0) {
                LOGGER.warn("[Command] Some players failed to receive config sync, but config was reloaded on server");
            }
        } catch (Exception e) {
            LOGGER.error("[Command] Error during config sync: {}", e.getMessage(), e);
            throw e;
        }
    }


    private static int modifyConfig(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            LOGGER.info("[Command] Config modify requested by player: {}", player.getName().getString());

            try {
                Services.PLATFORM.sendConfigOpenPacket(player, new S2COpenConfigPacket());

                context.getSource().sendSuccess(() -> Component.literal("§a[Gathering Chunks] Opening config menu..."), false);
                LOGGER.info("[Command] Config screen packet sent to player: {}", player.getName().getString());
                return 1;
            } catch (Exception e) {
                LOGGER.error("[Command] Failed to open config screen for player: {}", player.getName().getString(), e);
                context.getSource().sendFailure(Component.literal("§c[Gathering Chunks] Failed to open config menu"));
                return 0;
            }
        } else {
            context.getSource().sendFailure(Component.literal("§cThis command must be run by a player."));
            return 0;
        }
    }
}