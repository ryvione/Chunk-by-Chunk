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
public class WorldMenderRecipe {
    private final ItemStack input;
    public WorldMenderRecipe(ItemStack input) {
        this.input = input;
    }
    public ItemStack getInput() {
        return input;
    }
}
