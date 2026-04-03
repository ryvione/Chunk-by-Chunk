package com.ryvione.gatheringchunks.common.menus;

import com.ryvione.gatheringchunks.common.blockEntities.CaveScannerBlockEntity;
import com.ryvione.gatheringchunks.common.blockEntities.WorldScannerBlockEntity;
import com.ryvione.gatheringchunks.interop.Services;
import com.google.common.base.Preconditions;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class CaveScannerMenu extends WorldScannerMenu {

    public CaveScannerMenu(int menuId, Inventory inventory) {
        this(menuId, inventory, new SimpleContainer(CaveScannerBlockEntity.NUM_ITEM_SLOTS), new SimpleContainerData(CaveScannerBlockEntity.NUM_DATA_ITEMS));
    }

    public CaveScannerMenu(int menuId, Inventory inventory, Container container, ContainerData containerData) {
        super(menuId, inventory, container, containerData, Services.PLATFORM.caveScannerMenu());
    }

    @Override
    protected void addScannerSlots(Container container) {
        Preconditions.checkArgument(container.getContainerSize() >= CaveScannerBlockEntity.NUM_ITEM_SLOTS, "Expected " + CaveScannerBlockEntity.NUM_ITEM_SLOTS + " item slots, but entity has " + container.getContainerSize());
        addSlot(new FilteredSlot(container, CaveScannerBlockEntity.SLOT_FUEL, 27, 50, CaveScannerBlockEntity::isCaveScannerFuel));
    }

    @Override
    protected boolean quickMoveToContainer(ItemStack stack) {
        if (CaveScannerBlockEntity.isCaveScannerFuel(stack)) {
            return this.moveItemStackToContainerSlot(stack, CaveScannerBlockEntity.SLOT_FUEL, CaveScannerBlockEntity.SLOT_FUEL + 1, false);
        }
        return false;
    }
}
