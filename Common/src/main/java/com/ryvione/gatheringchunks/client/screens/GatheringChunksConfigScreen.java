/*
 * Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.client.screens;

import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.config.GatheringChunksConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GatheringChunksConfigScreen extends Screen {

    private final Screen parentScreen;
    private final GatheringChunksConfig config;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;
    private int experimentalLabelY;

    public GatheringChunksConfigScreen(Screen parentScreen) {
        super(Component.literal("Gathering Chunks Configuration"));
        this.parentScreen = parentScreen;
        this.config = ChunkByChunkConfig.get().getGatheringChunksConfig();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 40;
        int currentY = startY;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().getHardMode().isEnabled())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Disables village spawning and enforces chunk boundaries")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Hard Mode"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().getHardMode().setEnabled(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isMobsDropFragments())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Allow mobs to drop world fragments when killed")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Mobs Drop Fragments"),
                        (button, value) -> config.setMobsDropFragments(value)));
        currentY += SPACING;

        this.addRenderableWidget(Button.builder(
                        Component.literal("Fragment Drop Chance: " + config.getFragmentDropChance() + "%"),
                        button -> {
                            int newChance = (config.getFragmentDropChance() + 5) % 105;
                            if (newChance == 0) newChance = 5;
                            config.setFragmentDropChance(newChance);
                            button.setMessage(Component.literal("Fragment Drop Chance: " + config.getFragmentDropChance() + "%"));
                        })
                .bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(
                        Component.literal("Percentage chance for mobs to drop fragments (5-100%)")))
                .build());
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().getHardMode().isEnforceChunkBoundaries())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Prevent players from leaving spawned chunks")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Enforce Chunk Boundaries"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().getHardMode().setEnforceChunkBoundaries(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().isEnableProgressionHelper())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Automatically give chunk spawner if player gets stuck")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Enable Progression Helper"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().setEnableProgressionHelper(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isAutoSpawnTrees())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Automatically spawn trees in chunks without wood")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Auto-Spawn Trees"),
                        (button, value) -> config.setAutoSpawnTrees(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isPreventFluidFlowIntoVoid())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Prevent fluids (water/lava) from flowing into empty/void chunks")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Prevent Fluid Flow Into Void"),
                        (button, value) -> config.setPreventFluidFlowIntoVoid(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().isAlwaysSpawnVillage())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Always attempt to spawn the initial chunk in a village")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Always Spawn Village"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().setAlwaysSpawnVillage(value)));
        currentY += SPACING + 10;

        this.experimentalLabelY = currentY;
        currentY += 15;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getWorldScannerConfig().isExperimentalMode())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Enable experimental mode for the world scanner which highlights blocks in the world")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("World Scanner: Experimental Mode"),
                        (button, value) -> ChunkByChunkConfig.get().getWorldScannerConfig().setExperimentalMode(value)));
        currentY += SPACING + 10;

        this.addRenderableWidget(Button.builder(
                        Component.literal("Done"),
                        button -> this.onClose())
                .bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Experimental"), this.width / 2, experimentalLabelY, 0xFF5555);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parentScreen);
        }
    }
}