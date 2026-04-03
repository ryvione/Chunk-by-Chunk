/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.common.blockEntities;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import com.ryvione.gatheringchunks.common.menus.WorldScannerMenu;
import com.ryvione.gatheringchunks.common.util.ChunkUtil;
import com.ryvione.gatheringchunks.common.util.SpiralIterator;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import com.ryvione.gatheringchunks.interop.Services;
import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.*;

public class CaveScannerBlockEntity extends BaseFueledBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int NUM_ITEM_SLOTS = 2;
    public static final int DATA_MAP = 0;
    public static final int DATA_ENERGY = 1;
    public static final int DATA_MAX_ENERGY = 2;
    public static final int DATA_SCANNING_X = 3;
    public static final int DATA_SCANNING_Z = 4;
    public static final int DATA_ESP_ENABLED = 5;
    public static final int NUM_DATA_ITEMS = 6;
    public static final int SCAN_CENTER = 15;
    public static final int SCAN_ZOOM = 4;
    public static final int NO_MAP = -1;
    private static final int TICKS_BETWEEN_REPLICATES = 11;
    public static final Map<Item, FuelValueSupplier> FUEL;
    private static final int[] SLOTS_FOR_UP = new int[] { SLOT_INPUT };
    private static final int[] SLOTS_FOR_SIDES = new int[] { SLOT_FUEL };
    private static final int[] SLOTS_FOR_DOWN = new int[] { SLOT_FUEL };
    public static final byte[] SCAN_COLOR_PALETTE = {
            MapColor.COLOR_BLACK.getPackedId(MapColor.Brightness.NORMAL),
            MapColor.COLOR_BROWN.getPackedId(MapColor.Brightness.LOWEST),
            MapColor.COLOR_BROWN.getPackedId(MapColor.Brightness.LOW),
            MapColor.COLOR_BROWN.getPackedId(MapColor.Brightness.NORMAL),
            MapColor.TERRACOTTA_BROWN.getPackedId(MapColor.Brightness.HIGH),
            MapColor.COLOR_ORANGE.getPackedId(MapColor.Brightness.LOWEST),
            MapColor.COLOR_ORANGE.getPackedId(MapColor.Brightness.LOW),
            MapColor.COLOR_ORANGE.getPackedId(MapColor.Brightness.NORMAL),
            MapColor.COLOR_ORANGE.getPackedId(MapColor.Brightness.HIGH),
            MapColor.COLOR_YELLOW.getPackedId(MapColor.Brightness.HIGH),
            MapColor.COLOR_LIGHT_GRAY.getPackedId(MapColor.Brightness.HIGH),
            MapColor.COLOR_GRAY.getPackedId(MapColor.Brightness.HIGH),
            MapColor.SNOW.getPackedId(MapColor.Brightness.HIGH)
    };
    public static final Multimap<Item, Block> scanItemMappings = ArrayListMultimap.create();
    public static final int[] SCAN_COLOR_THRESHOLD = { 0, 1, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096 };
    private MapId map = null;
    private int scanCharge = 0;
    private final SpiralIterator scanIterator = new SpiralIterator();
    private int tickUntilReplicate = 0;
    private boolean espEnabled = false;
    private boolean manualScanPending = false;

    public final ContainerData dataAccess = new ContainerData() {
        public int get(int id) {
            return switch (id) {
                case DATA_MAP -> map != null ? map.id() : NO_MAP;
                case DATA_ENERGY -> getRemainingFuel();
                case DATA_MAX_ENERGY -> getChargedFuel();
                case DATA_SCANNING_X -> scanIterator.getX();
                case DATA_SCANNING_Z -> scanIterator.getY();
                case DATA_ESP_ENABLED -> espEnabled ? 1 : 0;
                default -> 0;
            };
        }

        public void set(int id, int value) {
            switch (id) {
                case DATA_ESP_ENABLED -> setEspEnabled(value != 0);
                case DATA_MAP -> {
                    if (value == NO_MAP) {
                        map = null;
                    } else {
                        map = new MapId(value);
                    }
                }
            }
        }

        public int getCount() {
            return NUM_DATA_ITEMS;
        }
    };

    static {
        FUEL = ImmutableMap.<Item, FuelValueSupplier>builder()
                .put(Services.PLATFORM.worldFragmentItem(),
                        () -> ChunkByChunkConfig.get().getWorldScannerConfig().getFuelPerFragment())
                .put(Services.PLATFORM.worldShardItem(),
                        () -> 4 * ChunkByChunkConfig.get().getWorldScannerConfig().getFuelPerFragment())
                .put(Services.PLATFORM.worldCrystalItem(),
                        () -> 16 * ChunkByChunkConfig.get().getWorldScannerConfig().getFuelPerFragment())
                .put(Services.PLATFORM.worldCoreBlockItem(),
                        () -> 64 * ChunkByChunkConfig.get().getWorldScannerConfig().getFuelPerFragment())
                .build();
    }

    public CaveScannerBlockEntity(BlockPos pos, BlockState state) {
        super(Services.PLATFORM.caveScannerEntity(), pos, state, NUM_ITEM_SLOTS, SLOT_FUEL, FUEL,
                Collections.emptyMap());
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.gatheringchunks.cavescanner");
    }

    @Override
    protected AbstractContainerMenu createMenu(int menuId, Inventory inventory) {
        return new com.ryvione.gatheringchunks.common.menus.CaveScannerMenu(menuId, inventory, this, this.dataAccess);
    }

    @Override
    public boolean isFuel(ItemStack itemStack) {
        return isCaveScannerFuel(itemStack);
    }

    @Override
    public int getFuelValue(ItemStack itemStack) {
        FuelValueSupplier supplier = FUEL.get(itemStack.getItem());
        return supplier != null ? supplier.get() : 0;
    }

    public static boolean isCaveScannerFuel(ItemStack itemStack) {
        return FUEL.containsKey(itemStack.getItem());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        int mapId = tag.getInt("Map");
        map = mapId == NO_MAP ? null : new MapId(mapId);
        scanIterator.load(tag.getCompound("ScanIterator"));
        scanCharge = tag.getInt("ScanCharge");
        espEnabled = tag.getBoolean("EspEnabled");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Map", map != null ? map.id() : NO_MAP);
        tag.put("ScanIterator", scanIterator.createTag());
        tag.putInt("ScanCharge", scanCharge);
        tag.putBoolean("EspEnabled", espEnabled);
    }

    private boolean validTarget() {
        ItemStack targetItem = getItem(SLOT_INPUT);
        if (targetItem.isEmpty()) {
            return true; 
        }
        if (targetItem.getItem() instanceof BucketItem bucket) {
            return Services.PLATFORM.getFluidContent(bucket) instanceof FlowingFluid;
        } else if (Items.SLIME_BALL.equals(targetItem.getItem())) {
            return true;
        }
        return targetItem.getItem() instanceof BlockItem || scanItemMappings.keySet().contains(targetItem.getItem());
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState,
            CaveScannerBlockEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean changed = false;

        try {
            com.ryvione.gatheringchunks.config.WorldScannerConfig.WorldScannerMode scanMode = ChunkByChunkConfig.get()
                    .getWorldScannerConfig().getWorldScannerScanMode();

            boolean shouldScan;
            if (scanMode == com.ryvione.gatheringchunks.config.WorldScannerConfig.WorldScannerMode.Manual) {
                shouldScan = entity.manualScanPending && entity.validTarget();
            } else {
                shouldScan = entity.scanIterator.getX() >= 0 && entity.validTarget();
            }

            if (shouldScan) {
                ItemStack targetItem = entity.getItem(SLOT_INPUT);

                if (entity.getRemainingFuel() > 0) {
                    int consumeAmount = entity
                            .consumeFuel(ChunkByChunkConfig.get().getWorldScannerConfig().getFuelConsumedPerTick());
                    entity.scanCharge += consumeAmount;
                }

                changed = entity.checkConsumeFuelItem();

                int chunkCost = ChunkByChunkConfig.get().getWorldScannerConfig().getFuelRequiredPerChunk();
                if (entity.scanCharge >= chunkCost) {
                    if (entity.map == null) {
                        entity.createMap();
                        if (entity.map == null) {
                            return;
                        }
                    }

                    ChunkPos originChunkPos = new ChunkPos(blockPos);
                    int chunkX, chunkZ;

                    if (scanMode == com.ryvione.gatheringchunks.config.WorldScannerConfig.WorldScannerMode.Manual) {
                        chunkX = originChunkPos.x
                                + ChunkByChunkConfig.get().getWorldScannerConfig().getManualChunkOffsetX();
                        chunkZ = originChunkPos.z
                                + ChunkByChunkConfig.get().getWorldScannerConfig().getManualChunkOffsetZ();
                    } else {
                        chunkX = entity.scanIterator.getX() + originChunkPos.x - SCAN_CENTER;
                        chunkZ = entity.scanIterator.getY() + originChunkPos.z - SCAN_CENTER;
                    }

                    ServerLevel scanLevel = serverLevel;
                    try {
                        if (serverLevel.getChunkSource().getGenerator() instanceof SkyChunkGenerator skyGenerator) {
                            ChunkPos targetPos = new ChunkPos(chunkX, chunkZ);
                            if (SpawnChunkHelper.isEmptyChunk(serverLevel, targetPos)) {
                                ServerLevel genLevel = serverLevel.getServer()
                                        .getLevel(skyGenerator.getGenerationLevel());
                                if (genLevel != null)
                                    scanLevel = genLevel;
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    ChunkAccess chunk = scanLevel.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                    if (chunk == null) {
                        if (scanMode != com.ryvione.gatheringchunks.config.WorldScannerConfig.WorldScannerMode.Manual) {
                        }
                        return;
                    }

                    int blockCount;
                    if (targetItem.isEmpty()) {
                        blockCount = countCaveBlocks(chunk);
                    } else if (targetItem.getItem().equals(Items.SLIME_BALL)
                            || targetItem.getItem().equals(Items.SLIME_BLOCK)) {
                        if (WorldgenRandom
                                .seedSlimeChunk(chunkX, chunkZ, ((WorldGenLevel) scanLevel).getSeed(), 987234911L)
                                .nextInt(10) == 0) {
                            blockCount = 20000;
                        } else {
                            blockCount = 0;
                        }
                    } else {
                        Set<Block> scanForBlocks = new HashSet<>();
                        Collection<Block> mappings = scanItemMappings.get(targetItem.getItem());
                        if (!mappings.isEmpty()) {
                            scanForBlocks.addAll(mappings);
                        } else if (targetItem.getItem() instanceof BucketItem bucket) {
                            Fluid fluid = Services.PLATFORM.getFluidContent(bucket);
                            if (fluid != null)
                                scanForBlocks.add(fluid.defaultFluidState().createLegacyBlock().getBlock());
                        } else if (targetItem.getItem() instanceof BlockItem blockItem) {
                            scanForBlocks.add(blockItem.getBlock());
                        }
                        blockCount = scanForBlocks.isEmpty() ? 0 : ChunkUtil.countBlocks(chunk, scanForBlocks);
                    }

                    byte color = MapColor.COLOR_BLACK.getPackedId(MapColor.Brightness.NORMAL);
                    for (int i = 0; i < SCAN_COLOR_THRESHOLD.length; i++) {
                        color = SCAN_COLOR_PALETTE[i];
                        if (blockCount <= SCAN_COLOR_THRESHOLD[i]) {
                            break;
                        }
                    }

                    MapItemSavedData data = serverLevel.getMapData(entity.map);
                    if (data != null) {
                        if (scanMode == com.ryvione.gatheringchunks.config.WorldScannerConfig.WorldScannerMode.Manual) {
                            int pixelStartX = (SCAN_CENTER
                                    + ChunkByChunkConfig.get().getWorldScannerConfig().getManualChunkOffsetX())
                                    * SCAN_ZOOM;
                            int pixelStartZ = (SCAN_CENTER
                                    + ChunkByChunkConfig.get().getWorldScannerConfig().getManualChunkOffsetZ())
                                    * SCAN_ZOOM;
                            for (int ix = 0; ix < SCAN_ZOOM; ix++) {
                                for (int iz = 0; iz < SCAN_ZOOM; iz++) {
                                    if (pixelStartX + ix >= 0 && pixelStartX + ix < 128 && pixelStartZ + iz >= 0
                                            && pixelStartZ + iz < 128)
                                        data.setColor(pixelStartX + ix, pixelStartZ + iz, color);
                                }
                            }
                        } else {
                            for (int ix = 0; ix < SCAN_ZOOM; ix++) {
                                for (int iz = 0; iz < SCAN_ZOOM; iz++) {
                                    int px = entity.scanIterator.getX() * SCAN_ZOOM + ix;
                                    int pz = entity.scanIterator.getY() * SCAN_ZOOM + iz;
                                    if (px >= 0 && px < 128 && pz >= 0 && pz < 128)
                                        data.setColor(px, pz, color);
                                }
                            }
                        }
                        data.setDirty(true);
                    }

                    if (scanMode != com.ryvione.gatheringchunks.config.WorldScannerConfig.WorldScannerMode.Manual) {
                        entity.scanIterator.next();
                    }
                    entity.manualScanPending = false;
                    entity.scanCharge -= chunkCost;
                    changed = true;
                }
            }
        } catch (Exception e) {
            GatheringChunksConstants.LOGGER.error("CaveScanner error at " + blockPos + ": " + e.getMessage());
        }

        if (changed) {
            setChanged(level, blockPos, blockState);
        }

        if (entity.map != null && (changed || entity.tickUntilReplicate <= 0)) {
            MapItemSavedData data = serverLevel.getMapData(entity.map);
            if (data != null) {
                for (ServerPlayer player : serverLevel.players()) {
                    data.getHoldingPlayer(player);
                    Packet<?> packet = data.getUpdatePacket(entity.map, player);
                    if (packet != null)
                        player.connection.send(packet);
                }
            }
            entity.tickUntilReplicate = TICKS_BETWEEN_REPLICATES;
        } else {
            entity.tickUntilReplicate--;
        }
    }

    private static int countCaveBlocks(ChunkAccess chunk) {
        int count = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        ChunkPos cp = chunk.getPos();
        int maxY = Math.min(chunk.getMaxBuildHeight(), 64);
        for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++) {
            for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
                for (int y = chunk.getMinBuildHeight(); y <= maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir() || state.is(Blocks.CAVE_AIR)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void createMap() {
        if (map == null && level instanceof ServerLevel serverLevel) {
            try {
                ChunkPos pos = new ChunkPos(getBlockPos());
                MapItemSavedData data = MapItemSavedData.createFresh(pos.getMaxBlockX(), pos.getMaxBlockZ(), (byte) 2,
                        false, false, serverLevel.dimension());
                map = serverLevel.getFreeMapId();
                serverLevel.setMapData(map, data);
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        data.setColor(x, y, MapColor.NONE.getPackedId(MapColor.Brightness.NORMAL));
                    }
                }
                data.setDirty(true);
            } catch (Exception e) {
                map = null;
            }
        }
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return switch (direction) {
            case UP -> SLOTS_FOR_UP;
            case DOWN -> SLOTS_FOR_DOWN;
            default -> SLOTS_FOR_SIDES;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return slot == SLOT_INPUT;
    }

    @Override
    public void setItem(int slot, ItemStack newItem) {
        boolean targetUnchanged = true;
        if (slot == SLOT_INPUT) {
            ItemStack itemStack = this.getItem(slot);
            targetUnchanged = !newItem.isEmpty() && ItemStack.isSameItem(newItem, itemStack);
        }
        super.setItem(slot, newItem);
        if (!targetUnchanged) {
            resetScan();
        }
    }

    public boolean isEspEnabled() {
        return espEnabled;
    }

    public void setEspEnabled(boolean enabled) {
        this.espEnabled = enabled;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void triggerManualScan() {
        this.manualScanPending = true;
        setChanged();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    private void resetScan() {
        if (map != null && level instanceof ServerLevel serverLevel) {
            MapItemSavedData data = serverLevel.getMapData(map);
            if (data != null) {
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        data.setColor(x, y, MapColor.NONE.getPackedId(MapColor.Brightness.NORMAL));
                    }
                }
            }
        }
        scanIterator.reset(SCAN_CENTER, SCAN_CENTER);
        setChanged();
    }
}
