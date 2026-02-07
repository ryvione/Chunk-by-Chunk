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

    public static WorldForgeRecipe oreToFragment(ItemStack ore, int fragments) {
        return new WorldForgeRecipe(
                List.of(ore),
                0,
                new ItemStack(Services.PLATFORM.worldFragmentItem(), fragments)
        );
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