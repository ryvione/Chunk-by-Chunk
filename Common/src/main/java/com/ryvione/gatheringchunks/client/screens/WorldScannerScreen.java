package com.ryvione.gatheringchunks.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.blockEntities.WorldScannerBlockEntity;
import com.ryvione.gatheringchunks.common.menus.WorldScannerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class WorldScannerScreen extends AbstractContainerScreen<WorldScannerMenu> {

    public static final ResourceLocation CONTAINER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            GatheringChunksConstants.MOD_ID, "textures/gui/container/worldscanner.png"
    );

    private static final int MAIN_TEXTURE_DIM = 512;
    private static final int MAP_DIMENSIONS = 128;
    private static final float TICKS_PER_FRAME = 4f;
    private static final int NUM_FRAMES = 8;

    private float animCounter = 0f;

    public WorldScannerScreen(WorldScannerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 310;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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

            StringBuilder builder = new StringBuilder();
            if (chunkZ < 0) {
                builder.append(-chunkZ);
                builder.append(" N ");
            } else if (chunkZ > 0) {
                builder.append(chunkZ);
                builder.append(" S ");
            }
            if (chunkX < 0) {
                builder.append(-chunkX);
                builder.append(" W");
            } else if (chunkX > 0) {
                builder.append(chunkX);
                builder.append(" E");
            }

            if (!builder.isEmpty()) {
                guiGraphics.renderTooltip(font, Component.literal(builder.toString()), mouseX, mouseY);
            }
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
    }
}