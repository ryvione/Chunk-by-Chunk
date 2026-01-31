/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.fabric;

import com.ryvione.gatheringchunks.common.CommonRegistry;
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
import com.ryvione.gatheringchunks.mixins.BucketFluidAccessor;
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

public final class FabricPlatformHelper implements CBCPlatformHelper {
    @Override
    public SpawnChunkBlock spawnChunkBlock() {
        return CommonRegistry.SPAWN_CHUNK_BLOCK;
    }

    @Override
    public Block unstableSpawnChunkBlock() {
        return CommonRegistry.UNSTABLE_SPAWN_CHUNK_BLOCK;
    }

    @Override
    public Block bedrockChestBlock() {
        return CommonRegistry.BEDROCK_CHEST_BLOCK;
    }

    @Override
    public Block worldCoreBlock() {
        return CommonRegistry.WORLD_CORE_BLOCK;
    }

    @Override
    public Block worldForgeBlock() {
        return CommonRegistry.WORLD_FORGE_BLOCK;
    }

    @Override
    public Block worldScannerBlock() {
        return CommonRegistry.WORLD_SCANNER_BLOCK;
    }

    @Override
    public Item spawnChunkBlockItem() {
        return CommonRegistry.SPAWN_CHUNK_BLOCK_ITEM;
    }

    @Override
    public Item unstableChunkSpawnBlockItem() {
        return CommonRegistry.UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM;
    }

    @Override
    public Item bedrockChestItem() {
        return CommonRegistry.BEDROCK_CHEST_BLOCK_ITEM;
    }

    @Override
    public Item worldCoreBlockItem() {
        return CommonRegistry.WORLD_CORE_BLOCK_ITEM;
    }

    @Override
    public Item worldForgeBlockItem() {
        return CommonRegistry.WORLD_FORGE_BLOCK_ITEM;
    }

    @Override
    public Item worldScannerBlockItem() {
        return CommonRegistry.WORLD_SCANNER_BLOCK_ITEM;
    }

    @Override
    public Item worldMenderBlockItem() {
        return CommonRegistry.WORLD_MENDER_BLOCK_ITEM;
    }

    @Override
    public Item worldFragmentItem() {
        return CommonRegistry.WORLD_FRAGMENT_ITEM;
    }

    @Override
    public Item worldShardItem() {
        return CommonRegistry.WORLD_SHARD_ITEM;
    }

    @Override
    public Item worldCrystalItem() {
        return CommonRegistry.WORLD_CRYSTAL_ITEM;
    }

    @Override
    public List<ItemStack> biomeThemeBlockItems() {
        return CommonRegistry.biomeThemedBlockItems;
    }

    @Override
    public BlockEntityType<BedrockChestBlockEntity> bedrockChestEntity() {
        return CommonRegistry.BEDROCK_CHEST_BLOCK_ENTITY;
    }

    @Override
    public BlockEntityType<WorldForgeBlockEntity> worldForgeEntity() {
        return CommonRegistry.WORLD_FORGE_BLOCK_ENTITY;
    }

    @Override
    public BlockEntityType<WorldScannerBlockEntity> worldScannerEntity() {
        return CommonRegistry.WORLD_SCANNER_BLOCK_ENTITY;
    }

    @Override
    public BlockEntityType<WorldMenderBlockEntity> worldMenderEntity() {
        return CommonRegistry.WORLD_MENDER_BLOCK_ENTITY;
    }

    @Override
    public SoundEvent spawnChunkSoundEffect() {
        return CommonRegistry.SPAWN_CHUNK_SOUND_EVENT;
    }

    @Override
    public MenuType<BedrockChestMenu> bedrockChestMenu() {
        return CommonRegistry.BEDROCK_CHEST_MENU;
    }

    @Override
    public MenuType<WorldForgeMenu> worldForgeMenu() {
        return CommonRegistry.WORLD_FORGE_MENU;
    }

    @Override
    public MenuType<WorldScannerMenu> worldScannerMenu() {
        return CommonRegistry.WORLD_SCANNER_MENU;
    }

    @Override
    public MenuType<WorldMenderMenu> worldMenderMenu() {
        return CommonRegistry.WORLD_MENDER_MENU;
    }

    @Override
    public Fluid getFluidContent(BucketItem bucket) {
        if (bucket instanceof BucketFluidAccessor bucketAccess) {
            return bucketAccess.getFluidContent();
        }
        return null;
    }

    @Override
    public <T> void unfreezeRegistry(MappedRegistry<T> registry) {
        try {
            java.lang.reflect.Field frozenField = MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            frozenField.setBoolean(registry, false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to unfreeze registry on Fabric", e);
        }
    }
}