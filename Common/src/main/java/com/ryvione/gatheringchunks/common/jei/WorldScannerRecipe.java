/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.common.jei;
import net.minecraft.world.item.ItemStack;
public class WorldScannerRecipe {
    private final ItemStack item;
    private final int value;
    public WorldScannerRecipe(ItemStack item, int value) {
        this.item = item;
        this.value = value;
    }
    public ItemStack getItem() {
        return item;
    }
    public int getValue() {
        return value;
    }
}
