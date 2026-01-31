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

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isHardMode())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Disables village spawning and enforces chunk boundaries")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Hard Mode"),
                        (button, value) -> config.setHardMode(value)));
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

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isEnforceChunkBoundaries())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Prevent players from leaving spawned chunks")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Enforce Chunk Boundaries"),
                        (button, value) -> config.setEnforceChunkBoundaries(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isEnableProgressionHelper())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Automatically give chunk spawner if player gets stuck")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Enable Progression Helper"),
                        (button, value) -> config.setEnableProgressionHelper(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isAutoSpawnTrees())
                .withTooltip(value -> Tooltip.create(
                        Component.literal("Automatically spawn trees in chunks without wood")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Auto-Spawn Trees"),
                        (button, value) -> config.setAutoSpawnTrees(value)));
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
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parentScreen);
        }
    }
}