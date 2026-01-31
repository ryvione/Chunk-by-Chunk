/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.common.blockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
public abstract class BaseFueledBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, StackedContentsCompatible {
    private final int fuelSlot;
    private final Map<Item, FuelValueSupplier> itemFuel;
    private final Map<TagKey<Item>, FuelValueSupplier> tagFuel;
    private int remainingFuel;
    private int chargedFuel;
    private NonNullList<ItemStack> items;
    protected BaseFueledBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state, int numItemSlots, int fuelSlot, Map<Item, FuelValueSupplier> itemFuel, Map<TagKey<Item>, FuelValueSupplier> tagFuel) {
        super(blockEntityType, pos, state);
        this.items = NonNullList.withSize(numItemSlots, ItemStack.EMPTY);
        this.fuelSlot = fuelSlot;
        this.itemFuel = itemFuel;
        this.tagFuel = tagFuel;
    }
    public int getChargedFuel() {
        return chargedFuel;
    }
    public int getRemainingFuel() {
        return remainingFuel;
    }
    public void setRemainingFuel(int value) {
        this.remainingFuel = value;
    }
    protected int consumeFuel(int amount) {
        int consumed = Math.min(amount, remainingFuel);
        remainingFuel -= consumed;
        return consumed;
    }
    protected boolean checkConsumeFuelItem() {
        ItemStack fuelItem = items.get(fuelSlot);
        if (remainingFuel == 0 && isFuel(fuelItem)) {
            chargedFuel = remainingFuel = getFuelValue(fuelItem);
            fuelItem.shrink(1);
            return true;
        }
        return false;
    }
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, provider);
        this.chargedFuel = tag.getInt("ChargedFuel");
        this.remainingFuel = tag.getInt("RemainingFuel");
    }
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("ChargedFuel", this.chargedFuel);
        tag.putInt("RemainingFuel", this.remainingFuel);
        ContainerHelper.saveAllItems(tag, this.items, provider);
    }
    public boolean isFuel(ItemStack itemStack) {
        if (itemFuel.getOrDefault(itemStack.getItem(), () -> 0).get() > 0) {
            return true;
        }
        for (Map.Entry<TagKey<Item>, FuelValueSupplier> entry : tagFuel.entrySet()) {
            if (itemStack.is(entry.getKey())) {
                return true;
            }
        }
        return false;
    }
    public int getFuelValue(ItemStack itemStack) {
        FuelValueSupplier fuelValueSupplier = itemFuel.get(itemStack.getItem());
        if (fuelValueSupplier == null) {
            for (Map.Entry<TagKey<Item>, FuelValueSupplier> entry : tagFuel.entrySet()) {
                if (itemStack.is(entry.getKey())) {
                    fuelValueSupplier = entry.getValue();
                }
            }
        }
        if (fuelValueSupplier != null) {
            return fuelValueSupplier.get();
        }
        return 0;
    }
    @Override
    public int getContainerSize() {
        return this.items.size();
    }
    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }
    @Override
    public ItemStack removeItem(int slot, int split) {
        return ContainerHelper.removeItem(items, slot, split);
    }
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }
    @Override
    public void setItem(int slot, ItemStack newItem) {
        items.set(slot, newItem);
        if (newItem.getCount() > this.getMaxStackSize()) {
            newItem.setCount(this.getMaxStackSize());
        }
    }
    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    @Override
    public void clearContent() {
        items.clear();
    }
    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }
    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }
    @Override
    public void fillStackedContents(StackedContents contents) {
        for (ItemStack itemstack : items) {
            contents.accountStack(itemstack);
        }
    }
    @Override
    public boolean stillValid(Player player) {
        if (level.getBlockEntity(worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D, (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return canPlaceItem(slot, itemStack);
    }
    @Override
    public boolean canPlaceItem(int slot, ItemStack item) {
        if (slot == fuelSlot) {
            return isFuel(item);
        }
        return true;
    }
    @FunctionalInterface
    public interface FuelValueSupplier {
        int get();
    }
}
