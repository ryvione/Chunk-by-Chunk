package com.ryvione.gatheringchunks.mixins;

import com.ryvione.gatheringchunks.server.world.SkyChunkGenerator;
import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PortalForcer.class)
public class NetherPortalForcerMixin {

    @Final
    @Shadow
    private ServerLevel level;

    @Inject(method = "createPortal", at = @At("HEAD"))
    private void gatheringchunks$ensureNetherPlatform(BlockPos pos, Direction.Axis axis, CallbackInfoReturnable<Optional<DimensionTransition>> cir) {
        if (!level.dimension().equals(Level.NETHER)) return;
        if (!(level.getChunkSource().getGenerator() instanceof SkyChunkGenerator)) return;
        ChunkPos chunkPos = new ChunkPos(pos);
        if (!SpawnChunkHelper.isEmptyChunk(level, chunkPos)) return;

        if (hasViableGroundNearby(pos)) return;

        buildMinimalPlatform(pos, axis);
    }

    private boolean hasViableGroundNearby(BlockPos center) {
        int searchRadius = 8;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - 16);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + 16);

        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos check = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                    BlockState state = level.getBlockState(check);
                    if (state.isAir()) continue;
                    if (state.getFluidState().getType() == Fluids.LAVA) continue;
                    if (state.getFluidState().getType() == Fluids.FLOWING_LAVA) continue;
                    BlockPos above = check.above();
                    BlockPos above2 = check.above(2);
                    if (level.getBlockState(above).isAir() && level.getBlockState(above2).isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void buildMinimalPlatform(BlockPos pos, Direction.Axis axis) {
        int y = pos.getY();
        int cx = pos.getX();
        int cz = pos.getZ();

        int clearFrom = -2;
        int clearTo = 2;
        for (int d = clearFrom; d <= clearTo; d++) {
            for (int dy = 0; dy <= 3; dy++) {
                int bx = axis == Direction.Axis.X ? cx + d : cx;
                int bz = axis == Direction.Axis.Z ? cz + d : cz;
                level.setBlock(new BlockPos(bx, y + dy, bz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        for (int d = clearFrom; d <= clearTo; d++) {
            int bx = axis == Direction.Axis.X ? cx + d : cx;
            int bz = axis == Direction.Axis.Z ? cz + d : cz;
            level.setBlock(new BlockPos(bx, y - 1, bz), Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_CLIENTS);
        }

    
        for (int dy = 0; dy <= 1; dy++) {
            int leftBx  = axis == Direction.Axis.X ? cx - 2 : cx;
            int leftBz  = axis == Direction.Axis.Z ? cz - 2 : cz;
            int rightBx = axis == Direction.Axis.X ? cx + 2 : cx;
            int rightBz = axis == Direction.Axis.Z ? cz + 2 : cz;
            level.setBlock(new BlockPos(leftBx,  y + dy, leftBz),  Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(new BlockPos(rightBx, y + dy, rightBz), Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}