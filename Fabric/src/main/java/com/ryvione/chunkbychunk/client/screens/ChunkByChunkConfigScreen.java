/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.chunkbychunk.client.screens;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.ryvione.chunkbychunk.client.uielements.SettingListWidget;
import com.ryvione.chunkbychunk.common.util.ConfigUtil;
public class ChunkByChunkConfigScreen extends Screen {
    private final Screen lastScreen;
    private SettingListWidget settingsList;
    private Button cancelButton;
    private Button resetButton;
    private Button saveButton;
    public ChunkByChunkConfigScreen(Screen lastScreen) {
        super(Component.translatable("config.chunkbychunk.title"));
        this.lastScreen = lastScreen;
    }
    @Override
    protected void init() {
        settingsList = new SettingListWidget(minecraft, this, width, 22, height - 44, (int) (0.9f * width));
        int w = (width / 3 - 60) / 2;
        resetButton = Button.builder(Component.translatable("controls.reset"), button -> {
            settingsList.reset();
        }).bounds(w, height - 32, 60, 20).build();
        cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> {
            ConfigUtil.loadDefaultConfig();
            this.minecraft.setScreen(lastScreen);
        }).bounds(width / 3 + w, height - 32, 60, 20).build();
        saveButton = Button.builder(Component.translatable("selectWorld.edit.save"), button -> {
            ConfigUtil.saveDefaultConfig();
            this.minecraft.setScreen(lastScreen);
        }).bounds(2 * width / 3 + w, height - 32, 60, 20).build();
        this.addWidget(settingsList);
        this.addWidget(cancelButton);
        this.addWidget(saveButton);
        this.addWidget(resetButton);
    }
    @Override
    public void onClose() {
        ConfigUtil.loadDefaultConfig();
        super.onClose();
    }
    @Override
    public void tick() {
        settingsList.tick();
    }
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        this.settingsList.render(guiGraphics, mouseX, mouseY, delta);
        int titleWidth = font.width(title);
        int titleX = (width - titleWidth) / 2;
        guiGraphics.drawString(this.font, title, titleX, 8, 0xFFFFFF);
        this.cancelButton.render(guiGraphics, mouseX, mouseY, delta);
        this.saveButton.render(guiGraphics, mouseX, mouseY, delta);
        this.resetButton.render(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }
}
