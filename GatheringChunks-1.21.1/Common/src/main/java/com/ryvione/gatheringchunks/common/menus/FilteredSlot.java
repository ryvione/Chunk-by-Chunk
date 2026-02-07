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

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;
public class FilteredSlot extends Slot {
    private final Predicate<ItemStack> itemFilter;
    public FilteredSlot(Container container, int slot, int x, int y, Predicate<ItemStack> itemFilter) {
        super(container, slot, x, y);
        this.itemFilter = itemFilter;
    }
    public boolean mayPlace(ItemStack itemStack) {
        return itemFilter.test(itemStack);
    }
}
