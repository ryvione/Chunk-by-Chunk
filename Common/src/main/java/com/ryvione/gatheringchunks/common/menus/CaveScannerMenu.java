package com.ryvione.gatheringchunks.common.menus;

import com.ryvione.gatheringchunks.common.blockEntities.CaveScannerBlockEntity;
import com.ryvione.gatheringchunks.common.blockEntities.WorldScannerBlockEntity;
import com.ryvione.gatheringchunks.interop.Services;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class CaveScannerMenu extends WorldScannerMenu {

    public CaveScannerMenu(int menuId, Inventory inventory) {
        this(menuId, inventory, new SimpleContainer(CaveScannerBlockEntity.NUM_ITEM_SLOTS), new SimpleContainerData(CaveScannerBlockEntity.NUM_DATA_ITEMS));
    }

    public CaveScannerMenu(int menuId, Inventory inventory, Container container, ContainerData containerData) {
        super(menuId, inventory, container, containerData, Services.PLATFORM.caveScannerMenu());
    }
}
