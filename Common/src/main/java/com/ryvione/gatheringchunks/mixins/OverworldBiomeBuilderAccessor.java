/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.mixins;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;
@Mixin(targets = "net.minecraft.world.level.biome.OverworldBiomeBuilder")
public interface OverworldBiomeBuilderAccessor {
    @Invoker
    void callAddBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> pairConsumer);
}
