package com.ryvione.gatheringchunks.mixins;

import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public class FluidTickMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preventFlowIntoVoidOnTick(Level level, BlockPos pos, FluidState state, CallbackInfo ci) {
        if (shouldCancelFluidFlow(level, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void preventSpreadIntoVoid(Level level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo ci) {
        BlockPos targetPos = pos.relative(direction);
        if (shouldCancelFluidFlow(level, targetPos)) {
            ci.cancel();
        }
    }

    @Inject(method = "canSpreadTo", at = @At("HEAD"), cancellable = true)
    private void preventCanSpreadIntoVoid(Level level, BlockPos fromPos, BlockState fromState, Direction direction, BlockPos toPos, BlockState toState, FluidState toFluidState, CallbackInfoReturnable<Boolean> cir) {
        if (shouldCancelFluidFlow(level, toPos)) {
            cir.setReturnValue(false);
        }
    }

    private boolean shouldCancelFluidFlow(Level level, BlockPos pos) {
        if (!com.ryvione.gatheringchunks.config.ChunkByChunkConfig.get().getGatheringChunksConfig().isPreventFluidFlowIntoVoid()) {
            return false;
        }

        if (level.isClientSide) {
            return false;
        }

        ChunkPos currentChunk = new ChunkPos(pos);

        if (SpawnChunkHelper.isEmptyChunk(level, currentChunk)) {
            return true;
        }

        BlockPos[] adjacentPositions = {
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west(),
                pos.below()
        };

        for (BlockPos adjacentPos : adjacentPositions) {
            ChunkPos adjacentChunk = new ChunkPos(adjacentPos);

            if (!currentChunk.equals(adjacentChunk)) {
                if (SpawnChunkHelper.isEmptyChunk(level, adjacentChunk)) {
                    BlockState adjacentState = level.getBlockState(adjacentPos);
                    if (adjacentState.isAir() || adjacentState.getBlock() == Blocks.CAVE_AIR) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}