/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.config;

import com.google.common.collect.ImmutableList;
import com.ryvione.gatheringchunks.interop.Services;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public enum ChunkRewardChestContent {
    WorldCore(Services.PLATFORM::worldCoreBlockItem),
    WorldCrystal(Services.PLATFORM::worldCrystalItem),
    WorldFragment(Services.PLATFORM::worldFragmentItem),
    WorldForge(Services.PLATFORM::worldForgeBlockItem),
    Random(null) {
        private static final List<RNGSlot> additionalSlot = ImmutableList.of(
                new RNGSlot(Services.PLATFORM::worldForgeBlockItem, 1, 1, 1),
                new RNGSlot(Services.PLATFORM::worldScannerBlockItem, 1, 1, 1),
                new RNGSlot(Services.PLATFORM::worldMenderBlockItem, 1, 1, 1)
        );
        private static final int additionalSlotRange = additionalSlot.stream().mapToInt(x -> x.chance).sum();

        @Override
        public List<ItemStack> getItems(Random random, int quantity) {
            List<ItemStack> result = new ArrayList<>();

            int totalFragmentsNeeded = 64;
            int crystalsGiven = random.nextInt(0, 5);
            int shardsGiven = random.nextInt(0, 17 - (crystalsGiven * 4));
            int fragmentsGiven = totalFragmentsNeeded - (crystalsGiven * 16) - (shardsGiven * 4);

            if (fragmentsGiven < 0) {
                fragmentsGiven = 0;
            }

            if (crystalsGiven > 0) {
                ItemStack crystalStack = Services.PLATFORM.worldCrystalItem().getDefaultInstance();
                crystalStack.setCount(crystalsGiven);
                result.add(crystalStack);
            }

            if (shardsGiven > 0) {
                ItemStack shardStack = Services.PLATFORM.worldShardItem().getDefaultInstance();
                shardStack.setCount(shardsGiven);
                result.add(shardStack);
            }

            if (fragmentsGiven > 0) {
                ItemStack fragmentStack = Services.PLATFORM.worldFragmentItem().getDefaultInstance();
                fragmentStack.setCount(fragmentsGiven);
                result.add(fragmentStack);
            }

            if (random.nextFloat() < 0.03f) {
                int roll = random.nextInt(additionalSlotRange);
                int addSlotChance = 0;
                for (RNGSlot rngSlot : additionalSlot) {
                    addSlotChance += rngSlot.chance;
                    if (roll < addSlotChance) {
                        ItemStack stack = rngSlot.item.get().getDefaultInstance();
                        stack.setCount(1);
                        result.add(stack);
                        break;
                    }
                }
            }

            return result;
        }
    };

    private final Supplier<Item> item;

    ChunkRewardChestContent(Supplier<Item> item) {
        this.item = item;
    }

    public List<ItemStack> getItems(Random random, int quantity) {
        ItemStack result = item.get().getDefaultInstance();
        result.setCount(quantity);
        return Collections.singletonList(result);
    }

    private record RNGSlot(Supplier<Item> item, int minQuanity, int maxQuantity, int chance) {}
}