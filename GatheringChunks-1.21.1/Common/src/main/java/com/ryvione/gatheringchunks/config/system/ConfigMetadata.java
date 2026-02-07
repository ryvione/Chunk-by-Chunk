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
public class ConfigMetadata extends ObjectMetadata {
    private final Map<String, SectionMetadata> sections;
    public ConfigMetadata(Collection<SectionMetadata> sections, Collection<FieldMetadata<?>> fields) {
        super(fields);
        ImmutableMap.Builder<String, SectionMetadata> sectionsBuilder = ImmutableMap.builder();
        for (SectionMetadata section : sections) {
            sectionsBuilder.put(section.getName().toLowerCase(Locale.ROOT), section);
        }
        this.sections = sectionsBuilder.build();
    }
    public Map<String, SectionMetadata> getSections() {
        return sections;
    }
}
