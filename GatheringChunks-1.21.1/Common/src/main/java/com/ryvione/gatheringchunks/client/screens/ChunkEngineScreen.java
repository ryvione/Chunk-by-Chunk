package com.ryvione.gatheringchunks.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.blockEntities.ChunkEngineBlockEntity;
import com.ryvione.gatheringchunks.common.menus.ChunkEngineMenu;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ChunkEngineScreen extends AbstractContainerScreen<ChunkEngineMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GatheringChunksConstants.MOD_ID, "textures/gui/container/chunk_engine.png");

    private Button upgradeButton;
    private Button submitButton;

    public ChunkEngineScreen(ChunkEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        if (ChunkByChunkConfig.get().getDifficulty().isExperimentalChunkLimit()) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;

            this.submitButton = Button.builder(Component.literal("Submit Fragments"), button -> {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 99);
                }
            }).bounds(x + 50, y + 35, 100, 20).build();
            this.addRenderableWidget(this.submitButton);

            this.upgradeButton = Button.builder(Component.literal("Upgrade Limit"), button -> {
                 if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 100);
                }
            }).bounds(x + 50, y + 60, 100, 20).build();
            this.addRenderableWidget(this.upgradeButton);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (ChunkByChunkConfig.get().getDifficulty().isExperimentalChunkLimit()) {
            int current = this.menu.getData().get(ChunkEngineBlockEntity.DATA_MAX_CHUNKS);
            int spawned = this.menu.getData().get(ChunkEngineBlockEntity.DATA_SPAWNED_CHUNKS);
            int stored = this.menu.getData().get(ChunkEngineBlockEntity.DATA_STORED_FRAGMENTS);
            
            guiGraphics.drawString(this.font, "Generated Chunks: " + spawned + "/" + current, this.leftPos + 8, this.topPos + 8, 0x404040, false);
            guiGraphics.drawString(this.font, "Stored Fragments: " + stored, this.leftPos + 8, this.topPos + 20, 0x404040, false);

            int nextMax = current + 1;
            Item requiredItem = null;
            if (current == 4) requiredItem = Items.COPPER_INGOT;
            else if (current == 8) requiredItem = Items.IRON_INGOT;
            else if (current == 16) requiredItem = Items.GOLD_INGOT;
            else if (current == 24) requiredItem = Items.DIAMOND;
            else if (current == 32) requiredItem = Items.NETHERITE_INGOT;
            else if (current == 48) requiredItem = Items.NETHER_STAR;

            if (requiredItem != null) {
                guiGraphics.drawString(this.font, "Engine Upgrade Required", this.leftPos + 8, this.topPos + 85, 0xFF5555, false);
                guiGraphics.drawString(this.font, "Cost: 1 " + requiredItem.getName(new ItemStack(requiredItem)).getString(), this.leftPos + 8, this.topPos + 95, 0x404040, false);
                 this.upgradeButton.active = true;
            } else {
                double cost = 16 * Math.pow(1.2, nextMax - 5);
                int fragCost = (int) cost;
                guiGraphics.drawString(this.font, "Upgrade Cost: " + fragCost + " fragments", this.leftPos + 8, this.topPos + 85, 0x404040, false);
                 this.upgradeButton.active = stored >= fragCost;
            }
        } else {
             int remainingTicks = this.menu.getRemainingTicks();
            if (remainingTicks > 0) {
                int seconds = remainingTicks / 20;
                String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);
                guiGraphics.drawString(this.font, Component.literal("Time: " + timeStr), this.leftPos + 40, this.topPos + 72, 0x404040, false);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
       if (!ChunkByChunkConfig.get().getDifficulty().isExperimentalChunkLimit()) {
            guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
            guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
       }
    }
}
