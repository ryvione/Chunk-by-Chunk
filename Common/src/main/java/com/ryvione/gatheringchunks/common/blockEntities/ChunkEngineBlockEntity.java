package com.ryvione.gatheringchunks.common.blockEntities;

import com.ryvione.gatheringchunks.common.blocks.ChunkEngineBlock;
import com.ryvione.gatheringchunks.common.menus.ChunkEngineMenu;
import com.ryvione.gatheringchunks.interop.Services;
import com.ryvione.gatheringchunks.server.world.ChunkEngineManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class ChunkEngineBlockEntity extends BaseFueledBlockEntity {
    public static final int SLOT_FUEL = 0;
    public static final int SLOT_STORAGE_START = 1;
    public static final int NUM_SLOTS = 6;

    public static final int DATA_REMAINING_FUEL = 0;
    public static final int DATA_CHARGED_FUEL = 1;
    public static final int DATA_MAX_CHUNKS = 2;
    public static final int DATA_SPAWNED_CHUNKS = 3;
    public static final int DATA_STORED_FRAGMENTS = 4;
    public static final int NUM_DATA = 5;

    private int storedFragments = 0;

    private static final Map<Item, FuelValueSupplier> ITEM_FUEL;

    static {
        ITEM_FUEL = Map.of(
                Services.PLATFORM.worldShardItem(), () -> 1200,
                Services.PLATFORM.worldCrystalItem(), () -> 6000
        );
    }

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_REMAINING_FUEL -> getRemainingFuel();
                case DATA_CHARGED_FUEL -> getChargedFuel();
                case DATA_MAX_CHUNKS -> {
                    if (level instanceof ServerLevel serverLevel) {
                         yield com.ryvione.gatheringchunks.server.world.ChunkSpawnController.get(serverLevel.getServer()).getMaxChunks(serverLevel.dimension().location().toString());
                    }
                    yield 4;
                }
                case DATA_SPAWNED_CHUNKS -> {
                    if (level instanceof ServerLevel serverLevel) {
                         yield com.ryvione.gatheringchunks.server.world.ChunkSpawnController.get(serverLevel.getServer()).getSpawnedChunkCount(serverLevel.dimension().location().toString());
                    }
                    yield 0;
                }
                case DATA_STORED_FRAGMENTS -> storedFragments;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_REMAINING_FUEL) {
                setRemainingFuel(value);
            } else if (index == DATA_STORED_FRAGMENTS) {
                storedFragments = value;
            }
        }

        @Override
        public int getCount() {
            return NUM_DATA;
        }
    };

    public ChunkEngineBlockEntity(BlockPos pos, BlockState state) {
        this(Services.PLATFORM.chunkEngineEntity(), pos, state);
    }

    public ChunkEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, NUM_SLOTS, SLOT_FUEL, ITEM_FUEL, Map.of());
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.gatheringchunks.chunkengine");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        return new ChunkEngineMenu(id, playerInventory, this, dataAccess);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChunkEngineBlockEntity engine) {
        boolean requiresFuel = com.ryvione.gatheringchunks.config.ChunkByChunkConfig.get().getDifficulty().isEngineRequiresFuel();
        boolean wasLit = state.getValue(ChunkEngineBlock.LIT);
        boolean isLit = !requiresFuel || engine.getRemainingFuel() > 0;

        if (isLit) {
            if (requiresFuel) {
                engine.consumeFuel(1);
            }
            
            if (level instanceof ServerLevel serverLevel) {
                ChunkEngineManager.get(serverLevel.getServer()).registerEngine(serverLevel, pos);
            }
        }

        if (requiresFuel && !isLit && engine.isFuel(engine.getItem(SLOT_FUEL))) {
            engine.checkConsumeFuelItem();
            isLit = true;
        }

        if (wasLit != isLit) {
            level.setBlock(pos, state.setValue(ChunkEngineBlock.LIT, isLit), 3);
            engine.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[]{SLOT_FUEL, 1, 2, 3, 4, 5};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return canPlaceItem(slot, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return true;
    }

    public void addStoredFragments(int amount) {
        this.storedFragments += amount;
        setChanged();
    }
    
    public int getStoredFragments() {
        return storedFragments;
    }
    
    public void consumeStoredFragments(int amount) {
        this.storedFragments = Math.max(0, this.storedFragments - amount);
        setChanged();
    }
    
    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.storedFragments = tag.getInt("StoredFragments");
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("StoredFragments", this.storedFragments);
    }
}