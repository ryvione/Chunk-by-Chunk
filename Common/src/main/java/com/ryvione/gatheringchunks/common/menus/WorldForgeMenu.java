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
import com.ryvione.gatheringchunks.common.blockEntities.WorldForgeBlockEntity;
import com.ryvione.gatheringchunks.interop.Services;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
public class WorldForgeMenu extends BaseInventoryContainerMenu {
    private final ContainerData containerData;
    public WorldForgeMenu(int menuId, Inventory inventory) {
        this(menuId, inventory, new SimpleContainer(WorldForgeBlockEntity.NUM_ITEM_SLOTS), new SimpleContainerData(WorldForgeBlockEntity.NUM_DATA_ITEMS));
    }
    public WorldForgeMenu(int menuId, Inventory inventory, Container container, ContainerData containerData) {
        super(Services.PLATFORM.worldForgeMenu(), menuId, container, inventory, 8, 84);
        this.containerData = containerData;
        Preconditions.checkArgument(container.getContainerSize() >= WorldForgeBlockEntity.NUM_ITEM_SLOTS, "Expected " + WorldForgeBlockEntity.NUM_ITEM_SLOTS + " item slots, but entity has " + container.getContainerSize());
        Preconditions.checkArgument(containerData.getCount() >= WorldForgeBlockEntity.NUM_DATA_ITEMS, "Expected " + WorldForgeBlockEntity.NUM_DATA_ITEMS + " data items, but entity has " + containerData.getCount());
        addSlot(new FilteredSlot(container, WorldForgeBlockEntity.SLOT_INPUT, 58, 35, WorldForgeBlockEntity::isWorldForgeFuel));
        addSlot(new TakeOnlySlot(container, WorldForgeBlockEntity.SLOT_RESULT, 116, 35));
        addDataSlots(containerData);
    }
    public int getProgress() {
        return this.containerData.get(WorldForgeBlockEntity.DATA_PROGRESS);
    }
    public int getGoal() {
        return this.containerData.get(WorldForgeBlockEntity.DATA_GOAL);
    }
    @Override
    protected boolean quickMoveToContainer(ItemStack stack) {
        if (WorldForgeBlockEntity.isWorldForgeFuel(stack)) {
            return this.moveItemStackToContainerSlot(stack, WorldForgeBlockEntity.SLOT_INPUT, WorldForgeBlockEntity.SLOT_INPUT + 1, false);
        }
        return false;
    }
}
