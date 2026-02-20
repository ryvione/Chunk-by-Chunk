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

import com.ryvione.gatheringchunks.common.mixinterface.IHolderReference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.core.Holder$Reference")
public interface HolderReferenceAccessor<T> extends IHolderReference<T> {
    @Override
    @Accessor("value")
    @Mutable
    void gc$setValue(T value);

    @Override
    @Accessor("value")
    T gc$getValue();

    @Accessor("value")
    @Mutable
    void setValue(T value);

    @Accessor("value")
    T getValue();
}