package com.ryvione.gatheringchunks.client.screens;

import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.config.*;
import com.ryvione.gatheringchunks.config.system.ConfigSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Paths;

public class GatheringChunksConfigScreen extends Screen {

    private final Screen parentScreen;
    private final GatheringChunksConfig config;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;
    private static final int SECTION_SPACING = 10;
    private int scrollOffset = 0;
    private static final int SCROLL_SPEED = 20;
    private static final ConfigSystem CONFIG_SYSTEM = new ConfigSystem();

    public GatheringChunksConfigScreen(Screen parentScreen) {
        super(Component.literal("Gathering Chunks Configuration"));
        this.parentScreen = parentScreen;
        this.config = ChunkByChunkConfig.get().getGatheringChunksConfig();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int centerX = this.width / 2;
        int startY = 40 - scrollOffset;
        int currentY = startY;

        currentY = addSectionLabel(centerX, currentY, "Hard Mode", 0xFFFF5555);

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().getHardMode().isEnabled())
                .withTooltip(value -> Tooltip.create(Component.literal("Enable Hard Mode - Enables all Hard Mode features at once")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Enable Hard Mode"),
                        (button, value) -> {
                            ChunkByChunkConfig.get().getDifficulty().getHardMode().setEnabled(value);
                            this.rebuildWidgets();
                        }));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().getHardMode().isEnforceChunkBoundaries())
                .withTooltip(value -> Tooltip.create(Component.literal("Prevent players from leaving spawned chunks")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Enforce Chunk Boundaries"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().getHardMode().setEnforceChunkBoundaries(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().getHardMode().isDisableVillages())
                .withTooltip(value -> Tooltip.create(Component.literal("Prevent villages from spawning")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Disable Villages"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().getHardMode().setDisableVillages(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().getHardMode().isSpawnInitialEngine())
                .withTooltip(value -> Tooltip.create(Component.literal("Spawn a Chunk Engine in the first chunk")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Spawn Initial Engine"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().getHardMode().setSpawnInitialEngine(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().getHardMode().isInitialEngineFuel())
                .withTooltip(value -> Tooltip.create(Component.literal("Give the initial Chunk Engine starting fuel")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Initial Engine Fuel"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().getHardMode().setInitialEngineFuel(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().getHardMode().isDisableChestsCommand())
                .withTooltip(value -> Tooltip.create(Component.literal("Disable the /chests command")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Disable /chests Command"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().getHardMode().setDisableChestsCommand(value)));
        currentY += SPACING;

        currentY += SECTION_SPACING;

        currentY = addSectionLabel(centerX, currentY, "Difficulty", 0xFFFFAA00);

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().isEngineRequiresFuel())
                .withTooltip(value -> Tooltip.create(Component.literal("Does the Chunk Engine require fuel to operate?")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Engine Requires Fuel"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().setEngineRequiresFuel(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().isExperimentalChunkLimit())
                .withTooltip(value -> Tooltip.create(Component.literal("Start with 9 chunks max, upgrade via Chunk Engine")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Experimental Chunk Limit"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().setExperimentalChunkLimit(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().isEnableProgressionHelper())
                .withTooltip(value -> Tooltip.create(Component.literal("Automatically give chunk spawner if player gets stuck")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Enable Progression Helper"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().setEnableProgressionHelper(value)));
        currentY += SPACING;

        this.addRenderableWidget(Button.builder(
                        Component.literal("Start Restriction: " + ChunkByChunkConfig.get().getDifficulty().getStartRestriction().name()),
                        button -> {
                            GameplayConfig.StartRestriction current = ChunkByChunkConfig.get().getDifficulty().getStartRestriction();
                            GameplayConfig.StartRestriction[] values = GameplayConfig.StartRestriction.values();
                            int nextIndex = (current.ordinal() + 1) % values.length;
                            ChunkByChunkConfig.get().getDifficulty().setStartRestriction(values[nextIndex]);
                            button.setMessage(Component.literal("Start Restriction: " + values[nextIndex].name()));
                            this.rebuildWidgets();
                        })
                .bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("What restriction to place on starting location (None, Village, Biome)")))
                .build());
        currentY += SPACING;

        if (ChunkByChunkConfig.get().getDifficulty().getStartRestriction() == GameplayConfig.StartRestriction.Biome) {
            EditBox biomeBox = new EditBox(this.font, centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                    Component.literal("Starting Biome"));
            biomeBox.setValue(ChunkByChunkConfig.get().getDifficulty().getStartingBiome());
            biomeBox.setResponder(value -> ChunkByChunkConfig.get().getDifficulty().setStartingBiome(value));
            biomeBox.setTooltip(Tooltip.create(Component.literal("The tag or name of the biome for starting spawn (e.g. #minecraft:is_forest)")));
            this.addRenderableWidget(biomeBox);
            currentY += SPACING;
        }

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().isAlwaysSpawnVillage())
                .withTooltip(value -> Tooltip.create(Component.literal("Always attempt to spawn the initial chunk in a village")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Always Spawn Village"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().setAlwaysSpawnVillage(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().spawnNewChunkChest())
                .withTooltip(value -> Tooltip.create(Component.literal("Should chunks include a chest with materials?")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Spawn New Chunk Chest"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().setSpawnNewChunkChest(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().spawnChestInInitialChunkOnly())
                .withTooltip(value -> Tooltip.create(Component.literal("Should the chest spawn in the initial chunk only?")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Chest in Initial Chunk Only"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().setSpawnChestInInitialChunkOnly(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getDifficulty().isSpawnChunkStrip())
                .withTooltip(value -> Tooltip.create(Component.literal("Spawn a full strip of chunks along an axis")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Spawn Chunk Strip"),
                        (button, value) -> ChunkByChunkConfig.get().getDifficulty().setSpawnChunkStrip(value)));
        currentY += SPACING;

        currentY += SECTION_SPACING;

        currentY = addSectionLabel(centerX, currentY, "Generation", 0xFF55FF55);

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getGeneration().isEnabled())
                .withTooltip(value -> Tooltip.create(Component.literal("Enable ChunkByChunk generation")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("CBC Generation Enabled"),
                        (button, value) -> ChunkByChunkConfig.get().getGeneration().setEnabled(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getGeneration().sealWorld())
                .withTooltip(value -> Tooltip.create(Component.literal("Generate empty chunks as bedrock")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Seal World (Bedrock)"),
                        (button, value) -> ChunkByChunkConfig.get().getGeneration().setSealWorld(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getGeneration().isSynchNether())
                .withTooltip(value -> Tooltip.create(Component.literal("Nether chunks spawn in response to overworld spawns")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Synch Nether Chunk Spawn"),
                        (button, value) -> ChunkByChunkConfig.get().getGeneration().setSynchNether(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getGeneration().useBedrockChest())
                .withTooltip(value -> Tooltip.create(Component.literal("Use indestructible bedrock chest")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Use Bedrock Chest"),
                        (button, value) -> ChunkByChunkConfig.get().getGeneration().setUseBedrockChest(value)));
        currentY += SPACING;

        this.addRenderableWidget(new AbstractSliderButton(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Initial Chunks: " + ChunkByChunkConfig.get().getGeneration().getInitialChunks()),
                (ChunkByChunkConfig.get().getGeneration().getInitialChunks() - 1) / 99.0) {
            {
                this.setTooltip(Tooltip.create(Component.literal("Number of chunks to spawn initially (1-100)")));
            }
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 99) + 1;
                setMessage(Component.literal("Initial Chunks: " + value));
            }
            @Override
            protected void applyValue() {
                int value = (int) (this.value * 99) + 1;
                ChunkByChunkConfig.get().getGeneration().setInitialChunks(value);
            }
        });
        currentY += SPACING;

        this.addRenderableWidget(new AbstractSliderButton(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Chunk Layer Spawn Rate: " + ChunkByChunkConfig.get().getGeneration().getChunkLayerSpawnRate()),
                (ChunkByChunkConfig.get().getGeneration().getChunkLayerSpawnRate() - 1) / 511.0) {
            {
                this.setTooltip(Tooltip.create(Component.literal("Chunk layers spawned per tick (1-512)")));
            }
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 511) + 1;
                setMessage(Component.literal("Chunk Layer Spawn Rate: " + value));
            }
            @Override
            protected void applyValue() {
                int value = (int) (this.value * 511) + 1;
                ChunkByChunkConfig.get().getGeneration().setChunkLayerSpawnRate(value);
            }
        });
        currentY += SPACING;

        this.addRenderableWidget(Button.builder(
                        Component.literal("Chest Contents: " + ChunkByChunkConfig.get().getGeneration().getChestContents().name()),
                        button -> {
                            ChunkRewardChestContent current = ChunkByChunkConfig.get().getGeneration().getChestContents();
                            ChunkRewardChestContent[] values = ChunkRewardChestContent.values();
                            int nextIndex = (current.ordinal() + 1) % values.length;
                            ChunkByChunkConfig.get().getGeneration().setChestContents(values[nextIndex]);
                            button.setMessage(Component.literal("Chest Contents: " + values[nextIndex].name()));
                        })
                .bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Type of items in reward chest")))
                .build());
        currentY += SPACING;

        this.addRenderableWidget(Button.builder(
                        Component.literal("Spawner Mode: " + ChunkByChunkConfig.get().getGeneration().getChunkSpawnerMode().name()),
                        button -> {
                            ChunkSpawnerMode current = ChunkByChunkConfig.get().getGeneration().getChunkSpawnerMode();
                            ChunkSpawnerMode[] values = ChunkSpawnerMode.values();
                            int nextIndex = (current.ordinal() + 1) % values.length;
                            ChunkByChunkConfig.get().getGeneration().setChunkSpawnerMode(values[nextIndex]);
                            button.setMessage(Component.literal("Spawner Mode: " + values[nextIndex].name()));
                        })
                .bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Edge = spawn adjacent chunks, Void = spawn in void, Both = allow both")))
                .build());
        currentY += SPACING;

        this.addRenderableWidget(new AbstractSliderButton(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Chest Quantity: " + ChunkByChunkConfig.get().getGeneration().getChestQuantity()),
                (ChunkByChunkConfig.get().getGeneration().getChestQuantity() - 1) / 63.0) {
            {
                this.setTooltip(Tooltip.create(Component.literal("Number of items in chest (1-64)")));
            }
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 63) + 1;
                setMessage(Component.literal("Chest Quantity: " + value));
            }
            @Override
            protected void applyValue() {
                int value = (int) (this.value * 63) + 1;
                ChunkByChunkConfig.get().getGeneration().setChestQuantity(value);
            }
        });
        currentY += SPACING;

        this.addRenderableWidget(new AbstractSliderButton(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Min Chest Depth: " + ChunkByChunkConfig.get().getGeneration().getMinChestSpawnDepth()),
                (ChunkByChunkConfig.get().getGeneration().getMinChestSpawnDepth() + 64) / 192.0) {
            {
                this.setTooltip(Tooltip.create(Component.literal("Minimum Y level for chest spawn (-64 to 128)")));
            }
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 192) - 64;
                setMessage(Component.literal("Min Chest Depth: " + value));
            }
            @Override
            protected void applyValue() {
                int value = (int) (this.value * 192) - 64;
                ChunkByChunkConfig.get().getGeneration().setMinChestSpawnDepth(value);
            }
        });
        currentY += SPACING;

        this.addRenderableWidget(new AbstractSliderButton(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Max Chest Depth: " + ChunkByChunkConfig.get().getGeneration().getMaxChestSpawnDepth()),
                (ChunkByChunkConfig.get().getGeneration().getMaxChestSpawnDepth() + 64) / 192.0) {
            {
                this.setTooltip(Tooltip.create(Component.literal("Maximum Y level for chest spawn (-64 to 128)")));
            }
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 192) - 64;
                setMessage(Component.literal("Max Chest Depth: " + value));
            }
            @Override
            protected void applyValue() {
                int value = (int) (this.value * 192) - 64;
                ChunkByChunkConfig.get().getGeneration().setMaxChestSpawnDepth(value);
            }
        });
        currentY += SPACING;

        currentY += SECTION_SPACING;

        currentY = addSectionLabel(centerX, currentY, "Gameplay", 0xFF5555FF);

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getGameplayConfig().isBlockPlacementAllowedOutsideSpawnedChunks())
                .withTooltip(value -> Tooltip.create(Component.literal("Allow block placement outside spawned chunks")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Block Placement Outside Chunks"),
                        (button, value) -> ChunkByChunkConfig.get().getGameplayConfig().setBlockPlacementAllowedOutsideSpawnedChunks(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getGameplayConfig().isChunkSpawnLeafDecayDisabled())
                .withTooltip(value -> Tooltip.create(Component.literal("Prevent leaves from chunk spawners from decaying")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Disable Leaf Decay"),
                        (button, value) -> ChunkByChunkConfig.get().getGameplayConfig().setChunkSpawnLeafDecayDisabled(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getGameplayConfig().isEnableChunkBarriers())
                .withTooltip(value -> Tooltip.create(Component.literal("Place barriers around chunks to prevent liquid flow")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Enable Chunk Barriers"),
                        (button, value) -> ChunkByChunkConfig.get().getGameplayConfig().setEnableChunkBarriers(value)));
        currentY += SPACING;

        this.addRenderableWidget(new AbstractSliderButton(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Unstable Chunk Chance: " + ChunkByChunkConfig.get().getGameplayConfig().getUnstableChunkChance() + "%"),
                ChunkByChunkConfig.get().getGameplayConfig().getUnstableChunkChance() / 100.0) {
            {
                this.setTooltip(Tooltip.create(Component.literal("Percentage chance for unstable chunks to spawn in chests (0-100%)")));
            }
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 100);
                setMessage(Component.literal("Unstable Chunk Chance: " + value + "%"));
            }
            @Override
            protected void applyValue() {
                int value = (int) (this.value * 100);
                ChunkByChunkConfig.get().getGameplayConfig().setUnstableChunkChance(value);
            }
        });
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isAutoSpawnTrees())
                .withTooltip(value -> Tooltip.create(Component.literal("Automatically spawn trees in chunks without wood")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Auto-Spawn Trees"),
                        (button, value) -> config.setAutoSpawnTrees(value)));
        currentY += SPACING;

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isPreventFluidFlowIntoVoid())
                .withTooltip(value -> Tooltip.create(Component.literal("Prevent fluids from flowing into empty chunks")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Prevent Fluid Flow Into Void"),
                        (button, value) -> config.setPreventFluidFlowIntoVoid(value)));
        currentY += SPACING;

        currentY += SECTION_SPACING;

        currentY = addSectionLabel(centerX, currentY, "Other", 0xFFFFFFFF);

        this.addRenderableWidget(CycleButton.onOffBuilder(config.isMobsDropFragments())
                .withTooltip(value -> Tooltip.create(Component.literal("Allow mobs to drop world fragments when killed")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Mobs Drop Fragments"),
                        (button, value) -> config.setMobsDropFragments(value)));
        currentY += SPACING;

        this.addRenderableWidget(new AbstractSliderButton(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Fragment Drop Chance: " + config.getFragmentDropChance() + "%"),
                config.getFragmentDropChance() / 100.0) {
            {
                this.setTooltip(Tooltip.create(Component.literal("Percentage chance for mobs to drop fragments (0-100%)")));
            }
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 100);
                setMessage(Component.literal("Fragment Drop Chance: " + value + "%"));
            }
            @Override
            protected void applyValue() {
                int value = (int) (this.value * 100);
                config.setFragmentDropChance(value);
            }
        });
        currentY += SPACING;

        this.addRenderableWidget(new AbstractSliderButton(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Min Fragment Drop: " + config.getMinFragmentDrop()),
                (config.getMinFragmentDrop() - 1) / 15.0) {
            {
                this.setTooltip(Tooltip.create(Component.literal("Minimum fragments dropped by mobs (1-16)")));
            }
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 15) + 1;
                setMessage(Component.literal("Min Fragment Drop: " + value));
            }
            @Override
            protected void applyValue() {
                int value = (int) (this.value * 15) + 1;
                config.setMinFragmentDrop(value);
            }
        });
        currentY += SPACING;

        this.addRenderableWidget(new AbstractSliderButton(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("Max Fragment Drop: " + config.getMaxFragmentDrop()),
                (config.getMaxFragmentDrop() - 1) / 15.0) {
            {
                this.setTooltip(Tooltip.create(Component.literal("Maximum fragments dropped by mobs (1-16)")));
            }
            @Override
            protected void updateMessage() {
                int value = (int) (this.value * 15) + 1;
                setMessage(Component.literal("Max Fragment Drop: " + value));
            }
            @Override
            protected void applyValue() {
                int value = (int) (this.value * 15) + 1;
                config.setMaxFragmentDrop(value);
            }
        });
        currentY += SPACING;

        currentY += SECTION_SPACING;

        currentY = addSectionLabel(centerX, currentY, "Experimental", 0xFFFF5555);

        this.addRenderableWidget(CycleButton.onOffBuilder(ChunkByChunkConfig.get().getWorldScannerConfig().isExperimentalMode())
                .withTooltip(value -> Tooltip.create(Component.literal("Enable experimental block highlighting for world scanner")))
                .create(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("World Scanner: Experimental Mode"),
                        (button, value) -> ChunkByChunkConfig.get().getWorldScannerConfig().setExperimentalMode(value)));
        currentY += SPACING;

        currentY += SECTION_SPACING + 10;

        this.addRenderableWidget(Button.builder(
                        Component.literal("Done"),
                        button -> this.onClose())
                .bounds(centerX - BUTTON_WIDTH / 2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private int addSectionLabel(int centerX, int y, String text, int color) {
        return y + 15;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        int centerX = this.width / 2;
        int startY = 40 - scrollOffset;
        int currentY = startY;

        graphics.drawCenteredString(this.font, Component.literal("Hard Mode"), centerX, currentY, 0xFFFF5555);
        currentY += 15 + SPACING * 6;
        currentY += SECTION_SPACING;

        graphics.drawCenteredString(this.font, Component.literal("Difficulty"), centerX, currentY, 0xFFFFAA00);
        currentY += 15 + SPACING * 3;
        currentY += SPACING;
        if (ChunkByChunkConfig.get().getDifficulty().getStartRestriction() == GameplayConfig.StartRestriction.Biome) {
            currentY += SPACING;
        }
        currentY += SPACING * 4 + SECTION_SPACING;

        graphics.drawCenteredString(this.font, Component.literal("Generation"), centerX, currentY, 0xFF55FF55);
        currentY += 15 + SPACING * 10 + SECTION_SPACING;

        graphics.drawCenteredString(this.font, Component.literal("Gameplay"), centerX, currentY, 0xFF5555FF);
        currentY += 15 + SPACING * 7 + SECTION_SPACING;

        graphics.drawCenteredString(this.font, Component.literal("Other"), centerX, currentY, 0xFFFFFFFF);
        currentY += 15 + SPACING * 4 + SECTION_SPACING;

        graphics.drawCenteredString(this.font, Component.literal("Experimental"), centerX, currentY, 0xFFFF5555);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) (scrollY * SCROLL_SPEED);
        scrollOffset = Math.max(0, scrollOffset);
        this.rebuildWidgets();
        return true;
    }

    @Override
    public void onClose() {
        CONFIG_SYSTEM.write(Paths.get("defaultconfigs", GatheringChunksConstants.MOD_ID + ".toml"), ChunkByChunkConfig.get());
        if (this.minecraft != null) {
            this.minecraft.setScreen(parentScreen);
        }
    }
}