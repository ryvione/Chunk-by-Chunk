/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.common.menus;

import com.google.common.base.Preconditions;
import com.ryvione.gatheringchunks.common.blockEntities.WorldMenderBlockEntity;
import com.ryvione.gatheringchunks.common.blocks.SpawnChunkBlock;
import com.ryvione.gatheringchunks.interop.Services;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
public class WorldMenderMenu extends BaseInventoryContainerMenu {
    private final ContainerData containerData;
    public WorldMenderMenu(int menuId, Inventory inventory) {
        this(menuId, inventory, new SimpleContainer(WorldMenderBlockEntity.NUM_ITEM_SLOTS), new SimpleContainerData(WorldMenderBlockEntity.NUM_DATA_ITEMS));
    }
    public WorldMenderMenu(int menuId, Inventory inventory, Container container, ContainerData containerData) {
        super(Services.PLATFORM.worldMenderMenu(), menuId, container, inventory, 8, 153);
        this.containerData = containerData;
        Preconditions.checkArgument(container.getContainerSize() >= WorldMenderBlockEntity.NUM_ITEM_SLOTS, "Expected " + WorldMenderBlockEntity.NUM_ITEM_SLOTS + " item slots, but entity has " + container.getContainerSize());
        Preconditions.checkArgument(containerData.getCount() >= WorldMenderBlockEntity.NUM_DATA_ITEMS, "Expected " + WorldMenderBlockEntity.NUM_DATA_ITEMS + " data items, but entity has " + containerData.getCount());
        addSlot(new FilteredSlot(container, WorldMenderBlockEntity.SLOT_INPUT, 80, 69, x -> x.getItem() instanceof BlockItem bi && (bi.getBlock().equals(Services.PLATFORM.worldCoreBlock()) || bi.getBlock() instanceof SpawnChunkBlock)));
        addDataSlots(containerData);
    }
    @Override
    protected boolean quickMoveToContainer(ItemStack stack) {
        return this.moveItemStackToContainerSlot(stack, WorldMenderBlockEntity.SLOT_INPUT, WorldMenderBlockEntity.SLOT_INPUT + 1, false);
    }
    public int getChunksSpawned() {
        return containerData.get(WorldMenderBlockEntity.DATA_CHUNKS_SPAWNED);
    }
}
