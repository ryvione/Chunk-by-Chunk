package com.ryvione.gatheringchunks.mixins;

import com.ryvione.gatheringchunks.server.world.SpawnChunkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
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
    private void preventSpreadIntoVoid(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo ci) {
        if (shouldCancelFluidFlow(level, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "canSpreadTo", at = @At("HEAD"), cancellable = true)
    private void preventCanSpreadIntoVoid(BlockGetter level, BlockPos fromPos, BlockState fromState, Direction direction, BlockPos toPos, BlockState toState, FluidState toFluidState, Fluid toFluid, CallbackInfoReturnable<Boolean> cir) {
        if (shouldCancelFluidFlow(level, toPos)) {
            cir.setReturnValue(false);
        }
    }

    private boolean shouldCancelFluidFlow(BlockGetter level, BlockPos pos) {
        if (!com.ryvione.gatheringchunks.config.ChunkByChunkConfig.get().getGatheringChunksConfig().isPreventFluidFlowIntoVoid()) {
            return false;
        }

        if (!(level instanceof Level serverLevel)) {
            return false;
        }

        if (serverLevel.isClientSide) {
            return false;
        }

        ChunkPos targetChunk = new ChunkPos(pos);

        if (SpawnChunkHelper.isEmptyChunk(serverLevel, targetChunk)) {
            return true;
        }

        return false;
    }
}