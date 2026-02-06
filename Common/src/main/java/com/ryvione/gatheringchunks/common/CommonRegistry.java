package com.ryvione.gatheringchunks.common;

import com.google.common.collect.ImmutableList;
import com.ryvione.gatheringchunks.common.blockEntities.*;
import com.ryvione.gatheringchunks.common.blocks.*;
import com.ryvione.gatheringchunks.common.menus.*;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

public class CommonRegistry {

    public static SpawnChunkBlock SPAWN_CHUNK_BLOCK;
    public static Block UNSTABLE_SPAWN_CHUNK_BLOCK;
    public static BedrockChestBlock BEDROCK_CHEST_BLOCK;
    public static Block WORLD_CORE_BLOCK;
    public static WorldForgeBlock WORLD_FORGE_BLOCK;
    public static WorldScannerBlock WORLD_SCANNER_BLOCK;
    public static WorldMenderBlock WORLD_MENDER_BLOCK;
    public static ChunkEngineBlock CHUNK_ENGINE_BLOCK;
    public static ChunkEraserBlock CHUNK_ERASER_BLOCK;

    public static BlockItem SPAWN_CHUNK_BLOCK_ITEM;
    public static BlockItem UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM;
    public static BlockItem BEDROCK_CHEST_BLOCK_ITEM;
    public static BlockItem WORLD_CORE_BLOCK_ITEM;
    public static BlockItem WORLD_FORGE_BLOCK_ITEM;
    public static BlockItem WORLD_SCANNER_BLOCK_ITEM;
    public static BlockItem WORLD_MENDER_BLOCK_ITEM;
    public static BlockItem CHUNK_ENGINE_BLOCK_ITEM;
    public static BlockItem CHUNK_ERASER_BLOCK_ITEM;
    public static Item WORLD_FRAGMENT_ITEM;
    public static Item WORLD_SHARD_ITEM;
    public static Item WORLD_CRYSTAL_ITEM;

    public static BlockEntityType<BedrockChestBlockEntity> BEDROCK_CHEST_BLOCK_ENTITY;
    public static BlockEntityType<WorldForgeBlockEntity> WORLD_FORGE_BLOCK_ENTITY;
    public static BlockEntityType<WorldScannerBlockEntity> WORLD_SCANNER_BLOCK_ENTITY;
    public static BlockEntityType<WorldMenderBlockEntity> WORLD_MENDER_BLOCK_ENTITY;
    public static BlockEntityType<ChunkEngineBlockEntity> CHUNK_ENGINE_BLOCK_ENTITY;

    public static MenuType<BedrockChestMenu> BEDROCK_CHEST_MENU;
    public static MenuType<WorldForgeMenu> WORLD_FORGE_MENU;
    public static MenuType<WorldScannerMenu> WORLD_SCANNER_MENU;
    public static MenuType<WorldMenderMenu> WORLD_MENDER_MENU;
    public static MenuType<ChunkEngineMenu> CHUNK_ENGINE_MENU;

    public static SoundEvent SPAWN_CHUNK_SOUND_EVENT;

    public static CreativeModeTab CHUNK_BY_CHUNK_TAB;

    public static List<ItemStack> biomeThemedBlockItems;

    public static void registerBlocks() {
        SPAWN_CHUNK_BLOCK = new SpawnChunkBlock("", false, BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
        Registry.register(BuiltInRegistries.BLOCK, id("chunkspawner"), SPAWN_CHUNK_BLOCK);

        UNSTABLE_SPAWN_CHUNK_BLOCK = new SpawnChunkBlock("", true, BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
        Registry.register(BuiltInRegistries.BLOCK, id("unstablechunkspawner"), UNSTABLE_SPAWN_CHUNK_BLOCK);

        BEDROCK_CHEST_BLOCK = new BedrockChestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1, 3600000.0F).isValidSpawn((state, getter, pos, arg) -> false));
        Registry.register(BuiltInRegistries.BLOCK, id("bedrockchest"), BEDROCK_CHEST_BLOCK);

        WORLD_CORE_BLOCK = new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F).lightLevel(state -> 7));
        Registry.register(BuiltInRegistries.BLOCK, id("worldcore"), WORLD_CORE_BLOCK);

        WORLD_FORGE_BLOCK = new WorldForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).lightLevel(state -> 7));
        Registry.register(BuiltInRegistries.BLOCK, id("worldforge"), WORLD_FORGE_BLOCK);

        WORLD_SCANNER_BLOCK = new WorldScannerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).lightLevel(state -> 4));
        Registry.register(BuiltInRegistries.BLOCK, id("worldscanner"), WORLD_SCANNER_BLOCK);

        WORLD_MENDER_BLOCK = new WorldMenderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).lightLevel(state -> 4));
        Registry.register(BuiltInRegistries.BLOCK, id("worldmender"), WORLD_MENDER_BLOCK);

        CHUNK_ENGINE_BLOCK = new ChunkEngineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F));
        Registry.register(BuiltInRegistries.BLOCK, id("chunkengine"), CHUNK_ENGINE_BLOCK);

        CHUNK_ERASER_BLOCK = new ChunkEraserBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F));
        Registry.register(BuiltInRegistries.BLOCK, id("chunkeraser"), CHUNK_ERASER_BLOCK);

        List<String> biomeThemesList = new ArrayList<>(GatheringChunksConstants.BIOME_THEMES);
        for (String biomeTheme : biomeThemesList) {
            SpawnChunkBlock spawnBlock = new SpawnChunkBlock(biomeTheme, false, BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
            Registry.register(BuiltInRegistries.BLOCK, id(biomeTheme + GatheringChunksConstants.BIOME_CHUNK_BLOCK_SUFFIX), spawnBlock);
        }
    }

    public static void registerItems() {
        SPAWN_CHUNK_BLOCK_ITEM = new BlockItem(SPAWN_CHUNK_BLOCK, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("chunkspawner"), SPAWN_CHUNK_BLOCK_ITEM);

        UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM = new BlockItem(UNSTABLE_SPAWN_CHUNK_BLOCK, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("unstablechunkspawner"), UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM);

        BEDROCK_CHEST_BLOCK_ITEM = new BlockItem(BEDROCK_CHEST_BLOCK, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("bedrockchest"), BEDROCK_CHEST_BLOCK_ITEM);

        WORLD_CORE_BLOCK_ITEM = new BlockItem(WORLD_CORE_BLOCK, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("worldcore"), WORLD_CORE_BLOCK_ITEM);

        WORLD_FORGE_BLOCK_ITEM = new BlockItem(WORLD_FORGE_BLOCK, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("worldforge"), WORLD_FORGE_BLOCK_ITEM);

        WORLD_SCANNER_BLOCK_ITEM = new BlockItem(WORLD_SCANNER_BLOCK, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("worldscanner"), WORLD_SCANNER_BLOCK_ITEM);

        WORLD_MENDER_BLOCK_ITEM = new BlockItem(WORLD_MENDER_BLOCK, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("worldmender"), WORLD_MENDER_BLOCK_ITEM);

        CHUNK_ENGINE_BLOCK_ITEM = new BlockItem(CHUNK_ENGINE_BLOCK, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("chunkengine"), CHUNK_ENGINE_BLOCK_ITEM);

        CHUNK_ERASER_BLOCK_ITEM = new BlockItem(CHUNK_ERASER_BLOCK, new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("chunkeraser"), CHUNK_ERASER_BLOCK_ITEM);

        WORLD_FRAGMENT_ITEM = new Item(new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("worldfragment"), WORLD_FRAGMENT_ITEM);

        WORLD_SHARD_ITEM = new Item(new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("worldshard"), WORLD_SHARD_ITEM);

        WORLD_CRYSTAL_ITEM = new Item(new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id("worldcrystal"), WORLD_CRYSTAL_ITEM);

        List<ItemStack> themeSpawnBlockItems = new ArrayList<>();
        List<String> biomeThemesList = new ArrayList<>(GatheringChunksConstants.BIOME_THEMES);
        for (String biomeTheme : biomeThemesList) {
            Block spawnBlock = BuiltInRegistries.BLOCK.get(id(biomeTheme + GatheringChunksConstants.BIOME_CHUNK_BLOCK_SUFFIX));
            BlockItem item = new BlockItem(spawnBlock, new Item.Properties());
            Registry.register(BuiltInRegistries.ITEM, id(biomeTheme + GatheringChunksConstants.BIOME_CHUNK_BLOCK_ITEM_SUFFIX), item);
            themeSpawnBlockItems.add(item.getDefaultInstance());
        }
        biomeThemedBlockItems = ImmutableList.copyOf(themeSpawnBlockItems);
    }

    public static void registerBlockEntities() {
        BEDROCK_CHEST_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("bedrockchestentity"),
                BlockEntityType.Builder.of(BedrockChestBlockEntity::new, BEDROCK_CHEST_BLOCK).build(null));

        WORLD_FORGE_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("worldforgeentity"),
                BlockEntityType.Builder.of(WorldForgeBlockEntity::new, WORLD_FORGE_BLOCK).build(null));

        WORLD_SCANNER_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("worldscannerentity"),
                BlockEntityType.Builder.of(WorldScannerBlockEntity::new, WORLD_SCANNER_BLOCK).build(null));

        WORLD_MENDER_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("worldmenderentity"),
                BlockEntityType.Builder.of(WorldMenderBlockEntity::new, WORLD_MENDER_BLOCK).build(null));

        CHUNK_ENGINE_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("chunkengineentity"),
                BlockEntityType.Builder.of(ChunkEngineBlockEntity::new, CHUNK_ENGINE_BLOCK).build(null));
    }

    public static void registerMenus() {
        BEDROCK_CHEST_MENU = Registry.register(BuiltInRegistries.MENU, id("bedrockchestmenu"),
                new MenuType<>(BedrockChestMenu::new, FeatureFlags.VANILLA_SET));

        WORLD_FORGE_MENU = Registry.register(BuiltInRegistries.MENU, id("worldforgemenu"),
                new MenuType<>(WorldForgeMenu::new, FeatureFlags.VANILLA_SET));

        WORLD_SCANNER_MENU = Registry.register(BuiltInRegistries.MENU, id("worldscannermenu"),
                new MenuType<>(WorldScannerMenu::new, FeatureFlags.VANILLA_SET));

        WORLD_MENDER_MENU = Registry.register(BuiltInRegistries.MENU, id("worldmendermenu"),
                new MenuType<>(WorldMenderMenu::new, FeatureFlags.VANILLA_SET));

        CHUNK_ENGINE_MENU = Registry.register(BuiltInRegistries.MENU, id("chunkenginemenu"),
                new MenuType<>(ChunkEngineMenu::new, FeatureFlags.VANILLA_SET));
    }

    public static void registerSounds() {
        SPAWN_CHUNK_SOUND_EVENT = Registry.register(BuiltInRegistries.SOUND_EVENT, id("spawnchunkevent"),
                SoundEvent.createVariableRangeEvent(id("chunk_spawn_sound")));
    }

    public static void registerChunkGenerators() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("skychunkgenerator"), SkyChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("netherchunkgenerator"), SkyChunkGenerator.OLD_NETHER_CODEC);
    }

    public static void registerCreativeTab() {
        CHUNK_BY_CHUNK_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("gatheringchunks_tab"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup.gatheringchunks"))
                        .icon(() -> new ItemStack(SPAWN_CHUNK_BLOCK_ITEM))
                        .displayItems((parameters, output) -> {
                            output.accept(SPAWN_CHUNK_BLOCK_ITEM);
                            output.accept(UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM);
                            output.accept(BEDROCK_CHEST_BLOCK_ITEM);
                            output.accept(WORLD_CORE_BLOCK_ITEM);
                            output.accept(WORLD_FORGE_BLOCK_ITEM);
                            output.accept(WORLD_SCANNER_BLOCK_ITEM);
                            output.accept(WORLD_MENDER_BLOCK_ITEM);
                            output.accept(CHUNK_ENGINE_BLOCK_ITEM);
                            output.accept(CHUNK_ERASER_BLOCK_ITEM);
                            output.accept(WORLD_FRAGMENT_ITEM);
                            output.accept(WORLD_SHARD_ITEM);
                            output.accept(WORLD_CRYSTAL_ITEM);

                            if (biomeThemedBlockItems != null) {
                                for (ItemStack stack : biomeThemedBlockItems) {
                                    output.accept(stack);
                                }
                            }
                        })
                        .build()
        );
    }

    public static void registerAll() {
        registerBlocks();
        registerItems();
        registerBlockEntities();
        registerMenus();
        registerSounds();
        registerChunkGenerators();
        registerCreativeTab();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(GatheringChunksConstants.MOD_ID, path);
    }
}