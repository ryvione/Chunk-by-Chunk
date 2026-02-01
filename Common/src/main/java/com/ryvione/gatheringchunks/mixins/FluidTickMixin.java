package com.ryvione.gatheringchunks.mixins;

import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents fluids from flowing into void/empty chunks in the ChunkByChunk dimension.
 * This mixin intercepts fluid tick events and cancels them if the destination is void.
 */
@Mixin(FlowingFluid.class)
public class FluidTickMixin {

    /**
     * Intercepts the fluid tick to prevent flow into void chunks.
     * This is optimized to only check when necessary and uses efficient chunk position checks.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preventFlowIntoVoid(Level level, BlockPos pos, FluidState state, CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }

        BlockPos[] adjacentPositions = {
            pos.north(),
            pos.south(),
            pos.east(),
            pos.west(),
            pos.below()
        };

        ChunkPos currentChunk = new ChunkPos(pos);
        
        for (BlockPos adjacentPos : adjacentPositions) {
            ChunkPos adjacentChunk = new ChunkPos(adjacentPos);
            
            if (!currentChunk.equals(adjacentChunk)) {
                if (SpawnChunkHelper.isEmptyChunk(level, adjacentChunk)) {
                    if (level.getBlockState(adjacentPos).isAir() || 
                        level.getBlockState(adjacentPos).getBlock() == Blocks.CAVE_AIR) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }
}
