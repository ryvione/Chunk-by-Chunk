/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.interop;

import com.ryvione.gatheringchunks.common.blockEntities.BedrockChestBlockEntity;
import com.ryvione.gatheringchunks.common.blockEntities.WorldForgeBlockEntity;
import com.ryvione.gatheringchunks.common.blockEntities.WorldMenderBlockEntity;
import com.ryvione.gatheringchunks.common.blockEntities.WorldScannerBlockEntity;
import com.ryvione.gatheringchunks.common.blocks.SpawnChunkBlock;
import com.ryvione.gatheringchunks.common.menus.BedrockChestMenu;
import com.ryvione.gatheringchunks.common.menus.WorldForgeMenu;
import com.ryvione.gatheringchunks.common.menus.WorldMenderMenu;
import com.ryvione.gatheringchunks.common.menus.WorldScannerMenu;
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

public interface CBCPlatformHelper {
    SpawnChunkBlock spawnChunkBlock();
    Block unstableSpawnChunkBlock();
    Block bedrockChestBlock();
    Block worldCoreBlock();
    Block worldForgeBlock();
    Block worldScannerBlock();
    Item spawnChunkBlockItem();
    Item unstableChunkSpawnBlockItem();
    Item bedrockChestItem();
    Item worldCoreBlockItem();
    Item worldForgeBlockItem();
    Item worldScannerBlockItem();
    Item worldMenderBlockItem();
    Item worldFragmentItem();
    Item worldShardItem();
    Item worldCrystalItem();
    List<ItemStack> biomeThemeBlockItems();
    BlockEntityType<BedrockChestBlockEntity> bedrockChestEntity();
    BlockEntityType<WorldForgeBlockEntity> worldForgeEntity();
    BlockEntityType<WorldScannerBlockEntity> worldScannerEntity();
    BlockEntityType<WorldMenderBlockEntity> worldMenderEntity();
    SoundEvent spawnChunkSoundEffect();
    MenuType<BedrockChestMenu> bedrockChestMenu();
    MenuType<WorldForgeMenu> worldForgeMenu();
    MenuType<WorldScannerMenu> worldScannerMenu();
    MenuType<WorldMenderMenu> worldMenderMenu();
    Fluid getFluidContent(BucketItem bucketItem);

    <T> void unfreezeRegistry(MappedRegistry<T> registry);
}