/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.server.world;

import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.Collections;
import java.util.List;
public class ChunkGeneratorAccess {
    public static Holder<NoiseGeneratorSettings> getNoiseGeneratorSettings(ChunkGenerator generator) {
        if (generator instanceof NoiseBasedChunkGenerator noiseGen) {
            return noiseGen.generatorSettings();
        }
        return null;
    }
    public static List<StructurePlacement> getPlacementsForFeature(ChunkGenerator generator, Holder<Structure> structure) {
        return Collections.emptyList();
    }
}
