package com.ryvione.gatheringchunks.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractCauldronBlock.class)
public class CauldronPrecipitationMixin {

    @Inject(method = "handlePrecipitation", at = @At("HEAD"))
    public void onHandlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation, CallbackInfo ci) {
        if (level.isClientSide) return;
        
        if (precipitation == Biome.Precipitation.RAIN && state.getBlock() == Blocks.CAULDRON) {
             level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState());
        }
    }
}
