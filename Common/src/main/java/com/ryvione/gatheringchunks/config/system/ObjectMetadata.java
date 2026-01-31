/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Chunk By Chunk (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.config.system;

import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
abstract class ObjectMetadata {
    private final Map<String, FieldMetadata<?>> fields;
    public ObjectMetadata(Collection<FieldMetadata<?>> fields) {
        ImmutableMap.Builder<String, FieldMetadata<?>> fieldsBuilder = ImmutableMap.builder();
        for (FieldMetadata<?> field : fields) {
            fieldsBuilder.put(field.getName().toLowerCase(Locale.ROOT), field);
        }
        this.fields = fieldsBuilder.build();
    }
    public Map<String, FieldMetadata<?>> getFields() {
        return fields;
    }
}
