package com.ryvione.gatheringchunks.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.ryvione.gatheringchunks.common.blockEntities.WorldScannerBlockEntity;
import com.ryvione.gatheringchunks.config.ChunkByChunkConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class ExperimentalScannerRenderer {
    
    private static final Map<ChunkPos, Map<Block, List<BlockPos>>> blockCache = new HashMap<>();
    private static long lastCacheClearTime = 0;
    private static final long CACHE_EXPIRY_MS = 10000; // 10 seconds

    public static void render(PoseStack poseStack, DeltaTracker deltaTracker, boolean renderBlockOutline, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer, net.minecraft.client.renderer.LightTexture lightTexture, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        if (!ChunkByChunkConfig.get().getWorldScannerConfig().isExperimentalMode()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheClearTime > CACHE_EXPIRY_MS) {
            blockCache.clear();
            lastCacheClearTime = currentTime;
        }

        Vec3 camPos = camera.getPosition();
        
        // Find all active scanners by iterating over loaded chunks
        int renderDistance = mc.options.getEffectiveRenderDistance();
        int playerChunkX = SectionPos.blockToSectionCoord(camPos.x);
        int playerChunkZ = SectionPos.blockToSectionCoord(camPos.z);

        for (int cx = playerChunkX - renderDistance; cx <= playerChunkX + renderDistance; cx++) {
            for (int cz = playerChunkZ - renderDistance; cz <= playerChunkZ + renderDistance; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof WorldScannerBlockEntity scanner) {
                        renderScannerHighlights(poseStack, level, scanner, camPos);
                    }
                }
            }
        }
    }

    private static void renderScannerHighlights(PoseStack poseStack, Level level, WorldScannerBlockEntity scanner, Vec3 camPos) {
        ItemStack input = scanner.getItem(WorldScannerBlockEntity.SLOT_INPUT);
        if (input.isEmpty()) return;

        int mapIdValue = scanner.dataAccess.get(WorldScannerBlockEntity.DATA_MAP);
        if (mapIdValue == WorldScannerBlockEntity.NO_MAP) return;

        MapItemSavedData mapData = level.getMapData(new net.minecraft.world.level.saveddata.maps.MapId(mapIdValue));
        if (mapData == null) return;

        Item inputItem = input.getItem();
        Collection<Block> targetBlocks = WorldScannerBlockEntity.scanItemMappings.get(inputItem);
        if (targetBlocks.isEmpty() && inputItem instanceof net.minecraft.world.item.BlockItem blockItem) {
            targetBlocks = Set.of(blockItem.getBlock());
        }
        
        if (targetBlocks.isEmpty()) return;

        BlockPos scannerPos = scanner.getBlockPos();
        ChunkPos scannerChunk = new ChunkPos(scannerPos);

        // Iterate over chunks in range of the scanner (31x31 chunks)
        for (int xOffset = -WorldScannerBlockEntity.SCAN_CENTER; xOffset <= WorldScannerBlockEntity.SCAN_CENTER; xOffset++) {
            for (int zOffset = -WorldScannerBlockEntity.SCAN_CENTER; zOffset <= WorldScannerBlockEntity.SCAN_CENTER; zOffset++) {
                int chunkX = scannerChunk.x + xOffset;
                int chunkZ = scannerChunk.z + zOffset;

                // Check if chunk is within distance of camera for performance (e.g. 64 blocks)
                double distSq = (chunkX * 16 + 8 - camPos.x) * (chunkX * 16 + 8 - camPos.x) + 
                               (chunkZ * 16 + 8 - camPos.z) * (chunkZ * 16 + 8 - camPos.z);
                if (distSq > 64 * 64) continue;

                LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) continue;

                // Get color from map data
                int pixelX = (xOffset + WorldScannerBlockEntity.SCAN_CENTER) * WorldScannerBlockEntity.SCAN_ZOOM;
                int pixelY = (zOffset + WorldScannerBlockEntity.SCAN_CENTER) * WorldScannerBlockEntity.SCAN_ZOOM;
                
                if (pixelX < 0 || pixelX >= 128 || pixelY < 0 || pixelY >= 128) continue;
                byte colorId = mapData.colors[pixelX + pixelY * 128];
                
                // Determine if this is a "valid" color representing density
                if (colorId == MapColor.NONE.getPackedId(MapColor.Brightness.NORMAL)) continue;
                if (colorId == MapColor.COLOR_BLACK.getPackedId(MapColor.Brightness.NORMAL)) continue;

                renderTargetBlocksInChunk(poseStack, chunk, targetBlocks, colorId, camPos);
            }
        }
    }

    private static void renderTargetBlocksInChunk(PoseStack poseStack, LevelChunk chunk, Collection<Block> targetBlocks, byte colorId, Vec3 camPos) {
        // Find MapColor by its packed ID
        MapColor mapColor = getMapColorFromPackedId(colorId);

        int color = mapColor.col;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        ChunkPos cp = chunk.getPos();
        Map<Block, List<BlockPos>> chunkCache = blockCache.computeIfAbsent(cp, k -> new HashMap<>());

        for (Block targetBlock : targetBlocks) {
            List<BlockPos> positions = chunkCache.get(targetBlock);
            if (positions == null) {
                positions = new ArrayList<>();
                for (BlockPos pos : BlockPos.betweenClosed(cp.getMinBlockX(), chunk.getMinBuildHeight(), cp.getMinBlockZ(), 
                                                           cp.getMaxBlockX(), chunk.getMaxBuildHeight(), cp.getMaxBlockZ())) {
                    if (chunk.getBlockState(pos).is(targetBlock)) {
                        positions.add(pos.immutable());
                    }
                }
                chunkCache.put(targetBlock, positions);
            }

            for (BlockPos pos : positions) {
                renderHighlight(poseStack, pos, r, g, b, camPos);
            }
        }
    }

    private static MapColor getMapColorFromPackedId(byte packedId) {
        // Check standard colors used in WorldScannerBlockEntity
        MapColor[] possibleColors = {
            MapColor.COLOR_BLACK, MapColor.NETHER, MapColor.COLOR_RED, 
            MapColor.TERRACOTTA_YELLOW, MapColor.COLOR_YELLOW, MapColor.GOLD, MapColor.SNOW
        };
        MapColor.Brightness[] brightnesses = MapColor.Brightness.values();

        for (MapColor color : possibleColors) {
            for (MapColor.Brightness brightness : brightnesses) {
                if (color.getPackedId(brightness) == packedId) {
                    return color;
                }
            }
        }
        return MapColor.COLOR_BLACK;
    }

    private static void renderHighlight(PoseStack poseStack, BlockPos pos, float r, float g, float b, Vec3 camPos) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        LevelRenderer.renderLineBox(poseStack, bufferBuilder, 0, 0, 0, 1, 1, 1, r, g, b, 1.0f);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        
        poseStack.popPose();
    }
}
