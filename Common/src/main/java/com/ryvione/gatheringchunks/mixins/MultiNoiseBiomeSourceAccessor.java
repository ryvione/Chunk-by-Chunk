package com.ryvione.gatheringchunks.mixins;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import com.ryvione.gatheringchunks.common.mixinterface.IMultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.level.biome.MultiNoiseBiomeSource")
public interface MultiNoiseBiomeSourceAccessor extends IMultiNoiseBiomeSource {
    @Override
    @Accessor("parameters")
    Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> gc$getParameters();

    @Accessor("parameters")
    Either<Climate.ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> getParameters();
}