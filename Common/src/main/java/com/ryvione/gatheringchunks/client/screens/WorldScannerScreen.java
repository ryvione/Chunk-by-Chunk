package com.ryvione.gatheringchunks.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.blockEntities.WorldScannerBlockEntity;
import com.ryvione.gatheringchunks.common.menus.WorldScannerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;

public class WorldScannerScreen extends AbstractContainerScreen<WorldScannerMenu> {

    public static final ResourceLocation CONTAINER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            GatheringChunksConstants.MOD_ID, "textures/gui/container/worldscanner.png"
    );

    private static final int MAIN_TEXTURE_DIM = 512;
    private static final int MAP_DIMENSIONS = 128;
    private static final float TICKS_PER_FRAME = 4f;
    private static final int NUM_FRAMES = 8;

    private float animCounter = 0f;
    private Button espButton;

    public WorldScannerScreen(WorldScannerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 310;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        if (ChunkByChunkConfig.get().getWorldScannerConfig().isExperimentalMode()) {
            espButton = this.addRenderableWidget(Button.builder(getEspMessage(), (button) -> {
                this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }).bounds(leftPos + 80, topPos + 20, 60, 20).build());
        }
    }

    private Component getEspMessage() {
        return Component.literal("ESP: ").append(menu.isEspEnabled() ? 
                Component.literal("ON").withStyle(ChatFormatting.GREEN) : 
                Component.literal("OFF").withStyle(ChatFormatting.RED));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (espButton != null) {
            espButton.setMessage(getEspMessage());
        }
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int mapX = mouseX - 174 - leftPos;
        int mapY = mouseY - 18 - topPos;

        if (mapX >= 0 && mapY >= 0 && mapX < MAP_DIMENSIONS && mapY < MAP_DIMENSIONS) {
            int chunkX = mapX / WorldScannerBlockEntity.SCAN_ZOOM - WorldScannerBlockEntity.SCAN_CENTER;
            int chunkZ = mapY / WorldScannerBlockEntity.SCAN_ZOOM - WorldScannerBlockEntity.SCAN_CENTER;

            guiGraphics.renderTooltip(font, Component.literal("Chunk: [" + chunkX + ", " + chunkZ + "]"), mouseX, mouseY);
        }

        int helpX = 285;
        int helpY = 6;
        if (mouseX >= leftPos + helpX && mouseX <= leftPos + helpX + 10 && 
            mouseY >= topPos + helpY && mouseY <= topPos + helpY + 10) {
            guiGraphics.renderTooltip(font, java.util.List.of(
                Component.literal("§6Resource Density:").getVisualOrderText(),
                Component.literal("§f- §fWhite: §7Extremely High (16k+)").getVisualOrderText(),
                Component.literal("§f- §6Gold: §7High (8k+)").getVisualOrderText(),
                Component.literal("§f- §eYellow: §7Common (2k+)").getVisualOrderText(),
                Component.literal("§f- §6Orange: §7Uncommon (512+)").getVisualOrderText(),
                Component.literal("§f- §cRed: §7Rare (32+)").getVisualOrderText(),
                Component.literal("§f- §4Dark Red: §7Very Rare (1-16)").getVisualOrderText(),
                Component.literal("§f- §0Black: §7None").getVisualOrderText()
            ), mouseX, topPos + helpY + 12);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        animCounter += partialTick;
        while (animCounter > TICKS_PER_FRAME * NUM_FRAMES) {
            animCounter -= TICKS_PER_FRAME * NUM_FRAMES;
        }
        int frame = Mth.floor(animCounter / TICKS_PER_FRAME);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(CONTAINER_TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight, MAIN_TEXTURE_DIM, MAIN_TEXTURE_DIM);

        if (menu.getEnergy() > 0) {
            int display = Mth.ceil(7.f * menu.getEnergy() / menu.getMaxEnergy());
            guiGraphics.blit(CONTAINER_TEXTURE, leftPos + 54, topPos + 56, 128 + 12 * display, 166 + 12 * frame, 13, 13, MAIN_TEXTURE_DIM, MAIN_TEXTURE_DIM);
        }

        if (menu.isMapAvailable()) {
            renderMap(guiGraphics);
        }

        guiGraphics.blit(CONTAINER_TEXTURE, leftPos + 234, topPos + 78, 124, 166 + frame * 4, 4, 4, MAIN_TEXTURE_DIM, MAIN_TEXTURE_DIM);
    }

    private void renderMap(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + 174, topPos + 18, 1.0D);

        var mapId = menu.getMapId();
        if (mapId != null) {
            var mapData = minecraft.level.getMapData(mapId);
            if (mapData != null) {
                minecraft.gameRenderer.getMapRenderer().render(
                        guiGraphics.pose(),
                        guiGraphics.bufferSource(),
                        mapId,
                        mapData,
                        true,
                        0xFFFFFF
                );
            }
        }

        guiGraphics.flush();
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        
        // Render help icon '?' (relative coordinates)
        guiGraphics.drawString(font, "?", 285, 6, 0xAAAAAA, false);
    }
}