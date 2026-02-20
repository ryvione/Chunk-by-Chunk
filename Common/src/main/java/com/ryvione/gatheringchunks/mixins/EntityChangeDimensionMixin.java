/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
package com.ryvione.gatheringchunks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.entity.Entity;

@Mixin(Entity.class)
public abstract class EntityChangeDimensionMixin {
}