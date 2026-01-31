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
import com.google.common.collect.ImmutableList;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.List;
public class IntFieldMetadata extends FieldMetadata<Integer> {
    public static final Logger LOGGER = LogManager.getLogger(GatheringChunksConstants.MOD_ID);
    private final int minValue;
    private final int maxValue;
    public IntFieldMetadata(Field field, String name, String comment, int minValue, int maxValue) {
        super(field, name, comment);
        Preconditions.checkArgument(Integer.TYPE.equals(field.getType()));
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
    public int getMinValue() {
        return minValue;
    }
    public int getMaxValue() {
        return maxValue;
    }
    @Override
    public List<String> getComments() {
        return ImmutableList.<String>builder().addAll(super.getComments()).add("Range: " + minValue + " ~ " + maxValue).build();
    }
    @Override
    public String serializeValue(Object object) {
        return getValue(object).toString();
    }
    @Override
    public void deserializeValue(Object object, String value) {
        try {
            int intValue = Integer.parseInt(value);
            if (intValue > maxValue) {
                LOGGER.warn("Invalid value {} for config field {} - greater than max value {}", value, getName(), maxValue);
            } else if (intValue < minValue) {
                LOGGER.warn ("Invalid value {} for config field {} - less than min value {}", value, getName(), minValue);
            } else {
                setValue(object, intValue);
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid value {} for config field {}", value, getName());
        }
    }
}
