/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.common.menus;

import com.ryvione.gatheringchunks.common.blockEntities.ChunkEngineBlockEntity;
import com.ryvione.gatheringchunks.interop.Services;
import com.ryvione.gatheringchunks.server.world.ChunkSpawnController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ChunkEngineMenu extends BaseInventoryContainerMenu {
    private final ContainerData data;

    public ChunkEngineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(ChunkEngineBlockEntity.NUM_SLOTS), new SimpleContainerData(ChunkEngineBlockEntity.NUM_DATA));
    }

    public ChunkEngineMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(Services.PLATFORM.chunkEngineMenu(), containerId, container, playerInventory, 8, 84);
        checkContainerSize(container, ChunkEngineBlockEntity.NUM_SLOTS);
        checkContainerDataCount(data, ChunkEngineBlockEntity.NUM_DATA);
        this.data = data;
        this.addDataSlots(data);

        this.addSlot(new Slot(container, ChunkEngineBlockEntity.SLOT_FUEL, 80, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (container instanceof ChunkEngineBlockEntity engine) {
                    return engine.isFuel(stack);
                }
                return true;
            }
        });

        for (int i = 0; i < 5; ++i) {
            this.addSlot(new Slot(container, ChunkEngineBlockEntity.SLOT_STORAGE_START + i, 44 + i * 18, 53));
        }
    }

    public int getFuelProgress() {
        int remainingFuel = this.data.get(ChunkEngineBlockEntity.DATA_REMAINING_FUEL);
        int chargedFuel = this.data.get(ChunkEngineBlockEntity.DATA_CHARGED_FUEL);
        if (chargedFuel != 0 && remainingFuel != 0) {
            return remainingFuel * 13 / chargedFuel;
        } else {
            return 0;
        }
    }

    public int getRemainingTicks() {
        return this.data.get(ChunkEngineBlockEntity.DATA_REMAINING_FUEL);
    }

    public ContainerData getData() {
        return this.data;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;

        ChunkEngineBlockEntity engine = null;
        if (getContainer() instanceof ChunkEngineBlockEntity be) {
            engine = be;
        } else {
            return false;
        }

        ChunkSpawnController controller = ChunkSpawnController.get(serverLevel.getServer());
        String dim = serverLevel.dimension().location().toString();
        int currentMax = controller.getMaxChunks(dim);

        if (id == 99) {
            int added = 0;
            for (int i = ChunkEngineBlockEntity.SLOT_STORAGE_START; i < ChunkEngineBlockEntity.NUM_SLOTS; i++) {
                ItemStack stack = getContainer().getItem(i);
                if (stack.is(Services.PLATFORM.worldFragmentItem())) {
                    added += stack.getCount();
                    stack.setCount(0);
                } else if (stack.is(Services.PLATFORM.worldShardItem())) {
                    added += stack.getCount() * 4;
                    stack.setCount(0);
                } else if (stack.is(Services.PLATFORM.worldCrystalItem())) {
                    added += stack.getCount() * 16;
                    stack.setCount(0);
                }
            }
            if (added > 0) {
                engine.addStoredFragments(added);
                return true;
            }
            return false;
        }

        if (id == 100) {
            int nextMax = currentMax + 1;

            Item requiredUpgradeItem = null;
            if (currentMax == 4) requiredUpgradeItem = Items.COPPER_INGOT;
            else if (currentMax == 8) requiredUpgradeItem = Items.IRON_INGOT;
            else if (currentMax == 16) requiredUpgradeItem = Items.GOLD_INGOT;
            else if (currentMax == 24) requiredUpgradeItem = Items.DIAMOND;
            else if (currentMax == 32) requiredUpgradeItem = Items.NETHERITE_INGOT;
            else if (currentMax == 48) requiredUpgradeItem = Items.NETHER_STAR;

            if (requiredUpgradeItem != null) {
                if (!hasItems(1, requiredUpgradeItem)) return false;
                consumeItems(1, requiredUpgradeItem);
                controller.setMaxChunks(dim, nextMax);
                return true;
            }

            double cost = 16 * Math.pow(1.2, nextMax - 5);
            int fragmentCost = (int) cost;

            if (engine.getStoredFragments() >= fragmentCost) {
                engine.consumeStoredFragments(fragmentCost);
                controller.setMaxChunks(dim, nextMax);
                return true;
            }

            return false;
        }

        return false;
    }

    private boolean hasItems(int count, Item item) {
        if (count <= 0) return true;
        int found = 0;
        for (int i = ChunkEngineBlockEntity.SLOT_STORAGE_START; i < ChunkEngineBlockEntity.NUM_SLOTS; i++) {
            ItemStack stack = getContainer().getItem(i);
            if (stack.is(item)) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }

    private void consumeItems(int count, Item item) {
        if (count <= 0) return;
        int remaining = count;
        for (int i = ChunkEngineBlockEntity.SLOT_STORAGE_START; i < ChunkEngineBlockEntity.NUM_SLOTS; i++) {
            ItemStack stack = getContainer().getItem(i);
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                getContainer().setItem(i, stack);
                if (remaining <= 0) break;
            }
        }
    }
}