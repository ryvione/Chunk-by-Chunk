package com.ryvione.gatheringchunks.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.blockEntities.ChunkEngineBlockEntity;
import com.ryvione.gatheringchunks.common.menus.ChunkEngineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ChunkEngineScreen extends AbstractContainerScreen<ChunkEngineMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(GatheringChunksConstants.MOD_ID, "textures/gui/container/chunk_engine.png");

    private static final int FUEL_BAR_X = 58;
    private static final int FUEL_BAR_Y = 35;
    private static final int FUEL_BAR_WIDTH = 16;
    private static final int FUEL_BAR_MAX_HEIGHT = 14;

    public ChunkEngineScreen(ChunkEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        int remainingFuel = this.menu.getRemainingTicks();
        int chargedFuel = this.menu.getData().get(ChunkEngineBlockEntity.DATA_CHARGED_FUEL);
        if (chargedFuel > 0 && remainingFuel > 0) {
            int barHeight = FUEL_BAR_MAX_HEIGHT * remainingFuel / chargedFuel;
            int barY = FUEL_BAR_Y + FUEL_BAR_MAX_HEIGHT - barHeight;
            guiGraphics.blit(TEXTURE, leftPos + FUEL_BAR_X, topPos + barY, 176, FUEL_BAR_MAX_HEIGHT - barHeight, FUEL_BAR_WIDTH, barHeight, 256, 256);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}
