package com.ryvione.gatheringchunks.neoforge;

import com.google.common.collect.ImmutableList;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.blockEntities.*;
import com.ryvione.gatheringchunks.common.blocks.*;
import com.ryvione.gatheringchunks.common.menus.*;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public class ModRegistry {
    
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, GatheringChunksConstants.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, GatheringChunksConstants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, GatheringChunksConstants.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, GatheringChunksConstants.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, GatheringChunksConstants.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GatheringChunksConstants.MOD_ID);
    
    // ========== BLOCKS ==========
    public static final DeferredHolder<Block, SpawnChunkBlock> SPAWN_CHUNK_BLOCK = BLOCKS.register("chunkspawner",
            () -> new SpawnChunkBlock("", false, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    
    public static final DeferredHolder<Block, SpawnChunkBlock> UNSTABLE_SPAWN_CHUNK_BLOCK = BLOCKS.register("unstablechunkspawner",
            () -> new SpawnChunkBlock("", true, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    
    public static final DeferredHolder<Block, BedrockChestBlock> BEDROCK_CHEST_BLOCK = BLOCKS.register("bedrockchest",
            () -> new BedrockChestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(-1, 3600000.0F).isValidSpawn(((state, getter, pos, arg) -> false))));
    
    public static final DeferredHolder<Block, Block> WORLD_CORE_BLOCK = BLOCKS.register("worldcore",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F).lightLevel((state) -> 7)));
    
    public static final DeferredHolder<Block, WorldForgeBlock> WORLD_FORGE_BLOCK = BLOCKS.register("worldforge",
            () -> new WorldForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).lightLevel((state) -> 7)));
    
    public static final DeferredHolder<Block, WorldScannerBlock> WORLD_SCANNER_BLOCK = BLOCKS.register("worldscanner",
            () -> new WorldScannerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).lightLevel((state) -> 4)));
    
    public static final DeferredHolder<Block, WorldMenderBlock> WORLD_MENDER_BLOCK = BLOCKS.register("worldmender",
            () -> new WorldMenderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).lightLevel((state) -> 4)));
    
    // Biome-themed spawn blocks
    private static final List<DeferredHolder<Block, SpawnChunkBlock>> BIOME_SPAWN_BLOCKS = new ArrayList<>();
    
    static {
        for (String biomeTheme : GatheringChunksConstants.BIOME_THEMES) {
            DeferredHolder<Block, SpawnChunkBlock> block = BLOCKS.register(
                    biomeTheme + GatheringChunksConstants.BIOME_CHUNK_BLOCK_SUFFIX,
                    () -> new SpawnChunkBlock(biomeTheme, false, BlockBehaviour.Properties.of().mapColor(MapColor.STONE))
            );
            BIOME_SPAWN_BLOCKS.add(block);
        }
    }
    
    // ========== ITEMS ==========
    public static final DeferredHolder<Item, BlockItem> SPAWN_CHUNK_BLOCK_ITEM = ITEMS.register("chunkspawner",
            () -> new BlockItem(SPAWN_CHUNK_BLOCK.get(), new Item.Properties()));
    
    public static final DeferredHolder<Item, BlockItem> UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM = ITEMS.register("unstablechunkspawner",
            () -> new BlockItem(UNSTABLE_SPAWN_CHUNK_BLOCK.get(), new Item.Properties()));
    
    public static final DeferredHolder<Item, BlockItem> BEDROCK_CHEST_ITEM = ITEMS.register("bedrockchest",
            () -> new BlockItem(BEDROCK_CHEST_BLOCK.get(), new Item.Properties()));
    
    public static final DeferredHolder<Item, BlockItem> WORLD_CORE_BLOCK_ITEM = ITEMS.register("worldcore",
            () -> new BlockItem(WORLD_CORE_BLOCK.get(), new Item.Properties()));
    
    public static final DeferredHolder<Item, BlockItem> WORLD_FORGE_BLOCK_ITEM = ITEMS.register("worldforge",
            () -> new BlockItem(WORLD_FORGE_BLOCK.get(), new Item.Properties()));
    
    public static final DeferredHolder<Item, BlockItem> WORLD_SCANNER_BLOCK_ITEM = ITEMS.register("worldscanner",
            () -> new BlockItem(WORLD_SCANNER_BLOCK.get(), new Item.Properties()));
    
    public static final DeferredHolder<Item, BlockItem> WORLD_MENDER_BLOCK_ITEM = ITEMS.register("worldmender",
            () -> new BlockItem(WORLD_MENDER_BLOCK.get(), new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> WORLD_FRAGMENT_ITEM = ITEMS.register("worldfragment",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> WORLD_SHARD_ITEM = ITEMS.register("worldshard",
            () -> new Item(new Item.Properties()));
    
    public static final DeferredHolder<Item, Item> WORLD_CRYSTAL_ITEM = ITEMS.register("worldcrystal",
            () -> new Item(new Item.Properties()));
    
    // Biome-themed items
    private static final List<DeferredHolder<Item, BlockItem>> BIOME_SPAWN_BLOCK_ITEMS = new ArrayList<>();
    
    static {
        int index = 0;
        for (String biomeTheme : GatheringChunksConstants.BIOME_THEMES) {
            final int finalIndex = index;
            DeferredHolder<Item, BlockItem> item = ITEMS.register(
                    biomeTheme + GatheringChunksConstants.BIOME_CHUNK_BLOCK_ITEM_SUFFIX,
                    () -> new BlockItem(BIOME_SPAWN_BLOCKS.get(finalIndex).get(), new Item.Properties())
            );
            BIOME_SPAWN_BLOCK_ITEMS.add(item);
            index++;
        }
    }
    
    public static List<ItemStack> getBiomeThemedBlockItems() {
        return BIOME_SPAWN_BLOCK_ITEMS.stream()
                .map(holder -> holder.get().getDefaultInstance())
                .collect(ImmutableList.toImmutableList());
    }
    
    // ========== BLOCK ENTITIES ==========
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BedrockChestBlockEntity>> BEDROCK_CHEST_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("bedrockchestentity",
                    () -> BlockEntityType.Builder.of(BedrockChestBlockEntity::new, BEDROCK_CHEST_BLOCK.get()).build(null));
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldForgeBlockEntity>> WORLD_FORGE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("worldforgeentity",
                    () -> BlockEntityType.Builder.of(WorldForgeBlockEntity::new, WORLD_FORGE_BLOCK.get()).build(null));
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldScannerBlockEntity>> WORLD_SCANNER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("worldscannerentity",
                    () -> BlockEntityType.Builder.of(WorldScannerBlockEntity::new, WORLD_SCANNER_BLOCK.get()).build(null));
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorldMenderBlockEntity>> WORLD_MENDER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("worldmenderentity",
                    () -> BlockEntityType.Builder.of(WorldMenderBlockEntity::new, WORLD_MENDER_BLOCK.get()).build(null));
    
    // ========== MENUS ==========
    public static final DeferredHolder<MenuType<?>, MenuType<BedrockChestMenu>> BEDROCK_CHEST_MENU =
            MENU_TYPES.register("bedrockchestmenu",
                    () -> new MenuType<>(BedrockChestMenu::new, FeatureFlags.DEFAULT_FLAGS));
    
    public static final DeferredHolder<MenuType<?>, MenuType<WorldForgeMenu>> WORLD_FORGE_MENU =
            MENU_TYPES.register("worldforgemenu",
                    () -> new MenuType<>(WorldForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));
    
    public static final DeferredHolder<MenuType<?>, MenuType<WorldScannerMenu>> WORLD_SCANNER_MENU =
            MENU_TYPES.register("worldscannermenu",
                    () -> new MenuType<>(WorldScannerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    
    public static final DeferredHolder<MenuType<?>, MenuType<WorldMenderMenu>> WORLD_MENDER_MENU =
            MENU_TYPES.register("worldmendermenu",
                    () -> new MenuType<>(WorldMenderMenu::new, FeatureFlags.DEFAULT_FLAGS));
    
    // ========== SOUNDS ==========
    public static final DeferredHolder<SoundEvent, SoundEvent> SPAWN_CHUNK_SOUND_EVENT =
            SOUND_EVENTS.register("spawnchunkevent",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(GatheringChunksConstants.MOD_ID, "chunk_spawn_sound")));
    
    
    // ========== CREATIVE TAB ==========
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CHUNK_BY_CHUNK_TAB =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(WORLD_CORE_BLOCK.get()))
                    .title(Component.translatable("itemGroup.gatheringchunks.main"))
                    .displayItems((parameters, output) -> {
                        output.accept(SPAWN_CHUNK_BLOCK_ITEM.get());
                        output.accept(UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM.get());
                        for (DeferredHolder<Item, BlockItem> item : BIOME_SPAWN_BLOCK_ITEMS) {
                            output.accept(item.get());
                        }
                        output.accept(BEDROCK_CHEST_ITEM.get());
                        output.accept(WORLD_CORE_BLOCK_ITEM.get());
                        output.accept(WORLD_FORGE_BLOCK_ITEM.get());
                        output.accept(WORLD_SCANNER_BLOCK_ITEM.get());
                        output.accept(WORLD_MENDER_BLOCK_ITEM.get());
                        output.accept(WORLD_FRAGMENT_ITEM.get());
                        output.accept(WORLD_SHARD_ITEM.get());
                        output.accept(WORLD_CRYSTAL_ITEM.get());
                    })
                    .build());
}