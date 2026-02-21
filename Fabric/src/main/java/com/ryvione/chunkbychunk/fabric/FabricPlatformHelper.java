/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.chunkbychunk.fabric;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import com.ryvione.chunkbychunk.common.blockEntities.*;
import com.ryvione.chunkbychunk.common.blocks.SpawnChunkBlock;
import com.ryvione.chunkbychunk.common.menus.BedrockChestMenu;
import com.ryvione.chunkbychunk.common.menus.WorldForgeMenu;
import com.ryvione.chunkbychunk.common.menus.WorldMenderMenu;
import com.ryvione.chunkbychunk.common.menus.WorldScannerMenu;
import com.ryvione.chunkbychunk.interop.CBCPlatformHelper;
import com.ryvione.chunkbychunk.mixins.BucketFluidAccessor;
import java.util.List;
public final class FabricPlatformHelper implements CBCPlatformHelper {
    @Override
    public SpawnChunkBlock spawnChunkBlock() {
        return ChunkByChunkMod.SPAWN_CHUNK_BLOCK;
    }
    @Override
    public Block unstableSpawnChunkBlock() {
        return ChunkByChunkMod.UNSTABLE_SPAWN_CHUNK_BLOCK;
    }
    @Override
    public Block bedrockChestBlock() {
        return ChunkByChunkMod.BEDROCK_CHEST_BLOCK;
    }
    @Override
    public Block worldCoreBlock() { return ChunkByChunkMod.WORLD_CORE_BLOCK; }
    @Override
    public Block worldForgeBlock() { return ChunkByChunkMod.WORLD_FORGE_BLOCK; }
    @Override
    public Block worldScannerBlock() { return ChunkByChunkMod.WORLD_SCANNER_BLOCK; }
    @Override
    public Item spawnChunkBlockItem() {
        return ChunkByChunkMod.SPAWN_CHUNK_BLOCK_ITEM;
    }
    @Override
    public Item unstableChunkSpawnBlockItem() {
        return ChunkByChunkMod.UNSTABLE_SPAWN_CHUNK_BLOCK_ITEM;
    }
    @Override
    public Item bedrockChestItem() {
        return ChunkByChunkMod.BEDROCK_CHEST_BLOCK_ITEM;
    }
    @Override
    public Item worldCoreBlockItem() { return ChunkByChunkMod.WORLD_CORE_BLOCK_ITEM; }
    @Override
    public Item worldForgeBlockItem() { return ChunkByChunkMod.WORLD_FORGE_BLOCK_ITEM; }
    @Override
    public Item worldScannerBlockItem() { return ChunkByChunkMod.WORLD_SCANNER_BLOCK_ITEM; }
    @Override
    public Item worldMenderBlockItem() {
        return ChunkByChunkMod.WORLD_MENDER_BLOCK_ITEM;
    }
    @Override
    public Item worldFragmentItem() { return ChunkByChunkMod.WORLD_FRAGMENT_ITEM; }
    @Override
    public Item worldShardItem() { return ChunkByChunkMod.WORLD_SHARD_ITEM; }
    @Override
    public Item worldCrystalItem() { return ChunkByChunkMod.WORLD_CRYSTAL_ITEM; }
    @Override
    public List<ItemStack> biomeThemeBlockItems() {
        return ChunkByChunkMod.biomeThemedBlockItems;
    }
    @Override
    public BlockEntityType<BedrockChestBlockEntity> bedrockChestEntity() {
        return (BlockEntityType<BedrockChestBlockEntity>) ChunkByChunkMod.BEDROCK_CHEST_BLOCK_ENTITY;
    }
    @Override
    public BlockEntityType<WorldForgeBlockEntity> worldForgeEntity() {
        return (BlockEntityType<WorldForgeBlockEntity>) ChunkByChunkMod.WORLD_FORGE_BLOCK_ENTITY;
    }
    @Override
    public BlockEntityType<WorldScannerBlockEntity> worldScannerEntity() {
        return (BlockEntityType<WorldScannerBlockEntity>) ChunkByChunkMod.WORLD_SCANNER_BLOCK_ENTITY;
    }
    @Override
    public BlockEntityType<WorldMenderBlockEntity> worldMenderEntity() {
        return (BlockEntityType<WorldMenderBlockEntity>) ChunkByChunkMod.WORLD_MENDER_BLOCK_ENTITY;
    }
    @Override
    public SoundEvent spawnChunkSoundEffect() {
        return ChunkByChunkMod.SPAWN_CHUNK_SOUND_EVENT;
    }
    @Override
    public MenuType<BedrockChestMenu> bedrockChestMenu() {
        return ChunkByChunkMod.BEDROCK_CHEST_MENU;
    }
    @Override
    public MenuType<WorldForgeMenu> worldForgeMenu() {
        return ChunkByChunkMod.WORLD_FORGE_MENU;
    }
    @Override
    public MenuType<WorldScannerMenu> worldScannerMenu() {
        return ChunkByChunkMod.WORLD_SCANNER_MENU;
    }
    @Override
    public MenuType<WorldMenderMenu> worldMenderMenu() {
        return ChunkByChunkMod.WORLD_MENDER_MENU;
    }
    @Override
    public Fluid getFluidContent(BucketItem bucket) {
        if (bucket instanceof BucketFluidAccessor bucketAccess) {
            return bucketAccess.getFluidContent();
        }
        return null;
    }
}
