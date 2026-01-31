package com.ryvione.gatheringchunks.neoforge;

import com.ryvione.gatheringchunks.common.blockEntities.BedrockChestBlockEntity;
import com.ryvione.gatheringchunks.common.blockEntities.WorldForgeBlockEntity;
import com.ryvione.gatheringchunks.common.blockEntities.WorldMenderBlockEntity;
import com.ryvione.gatheringchunks.common.blockEntities.WorldScannerBlockEntity;
import com.ryvione.gatheringchunks.common.blocks.SpawnChunkBlock;
import com.ryvione.gatheringchunks.common.menus.BedrockChestMenu;
import com.ryvione.gatheringchunks.common.menus.WorldForgeMenu;
import com.ryvione.gatheringchunks.common.menus.WorldMenderMenu;
import com.ryvione.gatheringchunks.common.menus.WorldScannerMenu;
import com.ryvione.gatheringchunks.interop.CBCPlatformHelper;
import net.minecraft.core.MappedRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

public class NeoForgePlatformHelper implements CBCPlatformHelper {

    @Override
    public SpawnChunkBlock spawnChunkBlock() {
        return (SpawnChunkBlock) ModRegistry.SPAWN_CHUNK_BLOCK.get();
    }

    @Override
    public Block unstableSpawnChunkBlock() {
        return ModRegistry.UNSTABLE_SPAWN_CHUNK_BLOCK.get();
    }

    @Override
    public Block bedrockChestBlock() {
        return ModRegistry.BEDROCK_CHEST_BLOCK.get();
    }

    @Override
    public Block worldCoreBlock() {
        return ModRegistry.WORLD_CORE_BLOCK.get();
    }

    @Override
    public Block worldForgeBlock() {
        return ModRegistry.WORLD_FORGE_BLOCK.get();
    }

    @Override
    public Block worldScannerBlock() {
        return ModRegistry.WORLD_SCANNER_BLOCK.get();
    }

    @Override
    public Item spawnChunkBlockItem() {
        return ModRegistry.SPAWN_CHUNK_BLOCK_ITEM.get();
    }

    @Override
    public Item unstableChunkSpawnBlockItem() {
        return ModRegistry.UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM.get();
    }

    @Override
    public Item bedrockChestItem() {
        return ModRegistry.BEDROCK_CHEST_ITEM.get();
    }

    @Override
    public Item worldCoreBlockItem() {
        return ModRegistry.WORLD_CORE_BLOCK_ITEM.get();
    }

    @Override
    public Item worldForgeBlockItem() {
        return ModRegistry.WORLD_FORGE_BLOCK_ITEM.get();
    }

    @Override
    public Item worldScannerBlockItem() {
        return ModRegistry.WORLD_SCANNER_BLOCK_ITEM.get();
    }

    @Override
    public Item worldMenderBlockItem() {
        return ModRegistry.WORLD_MENDER_BLOCK_ITEM.get();
    }

    @Override
    public Item worldFragmentItem() {
        return ModRegistry.WORLD_FRAGMENT_ITEM.get();
    }

    @Override
    public Item worldShardItem() {
        return ModRegistry.WORLD_SHARD_ITEM.get();
    }

    @Override
    public Item worldCrystalItem() {
        return ModRegistry.WORLD_CRYSTAL_ITEM.get();
    }

    @Override
    public List<ItemStack> biomeThemeBlockItems() {
        return ModRegistry.getBiomeThemedBlockItems();
    }

    @Override
    public BlockEntityType<BedrockChestBlockEntity> bedrockChestEntity() {
        return ModRegistry.BEDROCK_CHEST_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntityType<WorldForgeBlockEntity> worldForgeEntity() {
        return ModRegistry.WORLD_FORGE_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntityType<WorldScannerBlockEntity> worldScannerEntity() {
        return ModRegistry.WORLD_SCANNER_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntityType<WorldMenderBlockEntity> worldMenderEntity() {
        return ModRegistry.WORLD_MENDER_BLOCK_ENTITY.get();
    }

    @Override
    public SoundEvent spawnChunkSoundEffect() {
        return ModRegistry.SPAWN_CHUNK_SOUND_EVENT.get();
    }

    @Override
    public MenuType<BedrockChestMenu> bedrockChestMenu() {
        return ModRegistry.BEDROCK_CHEST_MENU.get();
    }

    @Override
    public MenuType<WorldForgeMenu> worldForgeMenu() {
        return ModRegistry.WORLD_FORGE_MENU.get();
    }

    @Override
    public MenuType<WorldScannerMenu> worldScannerMenu() {
        return ModRegistry.WORLD_SCANNER_MENU.get();
    }

    @Override
    public MenuType<WorldMenderMenu> worldMenderMenu() {
        return ModRegistry.WORLD_MENDER_MENU.get();
    }

    @Override
    public Fluid getFluidContent(BucketItem bucket) {
        return bucket.content;
    }

    @Override
    public <T> void unfreezeRegistry(MappedRegistry<T> registry) {
        try {
            java.lang.reflect.Field frozenField = null;
            try {
                frozenField = MappedRegistry.class.getDeclaredField("frozen");
            } catch (NoSuchFieldException e) {
                frozenField = MappedRegistry.class.getDeclaredField("l");
            }
            frozenField.setAccessible(true);
            frozenField.setBoolean(registry, false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to unfreeze registry on NeoForge", e);
        }
    }
}