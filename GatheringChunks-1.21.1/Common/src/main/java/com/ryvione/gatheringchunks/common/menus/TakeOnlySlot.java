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
public class TakeOnlySlot extends Slot {
    public TakeOnlySlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }
    public boolean mayPlace(ItemStack item) {
        return false;
    }
}
