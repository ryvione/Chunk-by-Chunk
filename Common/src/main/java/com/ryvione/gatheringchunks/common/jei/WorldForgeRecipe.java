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

import com.ryvione.gatheringchunks.interop.Services;
import net.minecraft.world.item.ItemStack;

import java.util.List;
public class WorldForgeRecipe {
    private final int fuelValue;
    private final List<ItemStack> inputItems;
    private final ItemStack output;
    public WorldForgeRecipe(List<ItemStack> inputs, int value) {
        this(inputs, value, Services.PLATFORM.worldFragmentItem().getDefaultInstance());
    }
    public WorldForgeRecipe(List<ItemStack> inputItems, int fuelValue, ItemStack outputItem) {
        this.inputItems = inputItems;
        this.fuelValue = fuelValue;
        this.output = outputItem;
    }
    public int getFuelValue() {
        return fuelValue;
    }
    public List<ItemStack> getInputItems() {
        return inputItems;
    }
    public ItemStack getOutput() {
        return output;
    }
}
