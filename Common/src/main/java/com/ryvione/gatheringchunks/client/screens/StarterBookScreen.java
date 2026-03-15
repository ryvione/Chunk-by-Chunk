package com.ryvione.gatheringchunks.client.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class StarterBookScreen extends Screen {
    private static final ResourceLocation BOOK_TEXTURE =
        ResourceLocation.withDefaultNamespace("textures/gui/book.png");

    private final int imageWidth = 192;
    private final int imageHeight = 192;
    private int currPage = 0;
    private final int maxPages = 6;
    private int previousBlur;

    public StarterBookScreen() {
        super(Component.literal("Gathering Chunks Guide"));
    }

    @Override
    protected void init() {
        super.init();

        previousBlur = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(0);
        Minecraft.getInstance().options.save();

        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        this.addRenderableWidget(Button.builder(
            Component.literal("<"),
            btn -> { if (currPage > 0) currPage--; }
        ).bounds(x + 40, y + 160, 20, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal(">"),
            btn -> { if (currPage < maxPages - 1) currPage++; }
        ).bounds(x + 130, y + 160, 20, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            btn -> this.onClose()
        ).bounds(this.width / 2 - 100, y + imageHeight + 10, 200, 20).build());
    }

    @Override
    public void removed() {
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(previousBlur);
        Minecraft.getInstance().options.save();
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        guiGraphics.blit(BOOK_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        String title = "";
        String content = "";

        switch (currPage) {
            case 0 -> {
                title = "§lWelcome!";
                content = "Gathering Chunks is a survival experience where the world is yours to build, chunk by chunk.\n\nUse §6World Shards §0to spawn new land and expand your reach!";
            }
            case 1 -> {
                title = "§lExpansion";
                content = "Craft §6World Shards §0and §6Crystals §0from fragments. Use them in a §bWorld Forge §0to create Spawner Blocks.\n\nPlace Spawners at world edges to grow.";
            }
            case 2 -> {
                title = "§lScanning";
                content = "The §bWorld Scanner §0reveals resource density in nearby chunks.\n\nCheck the §6'?' §0icon in the scanner UI for color info and density levels.";
            }
            case 3 -> {
                title = "§lSyncing";
                content = "The §5Nether §0and §6Overworld §0are linked! Spawning a chunk in one will often sync to the other.\n\n§8Note: Bedrock ceilings are automatically managed.";
            }
            case 4 -> {
                title = "§lMaintenance";
                content = "In §cHard Mode§0, chunks are unstable! Place a §bChunk Engine §0and fuel it with Shards.\n\nWithout an engine, land will eventually §4Reset to Air§0!";
            }
            case 5 -> {
                title = "§lThemed Chunks";
                content = "Special Spawners allow you to choose specific biomes like §2Forest§0, §eDesert§0, or §bOcean§0.\n\nThey find matching biomes to keep the world beautiful.";
            }
        }

        guiGraphics.drawString(font, title, x + 45, y + 25, 0x000000, false);

        guiGraphics.drawWordWrap(
            font,
            Component.literal(content),
            x + 45,
            y + 42,
            100,
            0x000000
        );

        String pageStr = (currPage + 1) + "/" + maxPages;
        int pageW = font.width(pageStr);
        guiGraphics.drawString(font, pageStr, x + imageWidth / 2 - pageW / 2, y + 156, 0x404040, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
