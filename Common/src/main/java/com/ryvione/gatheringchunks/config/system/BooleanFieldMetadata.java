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

import com.google.common.base.Preconditions;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
public class BooleanFieldMetadata extends FieldMetadata<Boolean> {
    public static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    private final Field field;
    public BooleanFieldMetadata(Field field, String name, String comment) {
        super(field, name, comment);
        Preconditions.checkArgument(Boolean.TYPE.equals(field.getType()));
        this.field = field;
        this.field.setAccessible(true);
    }
    @Override
    public String serializeValue(Object object) {
        try {
            return field.get(object).toString();
        } catch (IllegalAccessException e) {
            throw new ConfigException("Failed to retrieve " + getName() + " from object " + object, e);
        }
    }
    @Override
    public void deserializeValue(Object object, String value) {
        try {
            if ("true".equalsIgnoreCase(value)) {
                field.set(object, true);
            } else if ("false".equalsIgnoreCase(value)) {
                field.set(object, false);
            } else {
                LOGGER.warn("Invalid boolean value {} for configuration item {}", value, getName());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set " + getName() + " to value " + value, e);
        }
    }
}
