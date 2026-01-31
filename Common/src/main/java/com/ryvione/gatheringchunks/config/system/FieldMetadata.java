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

import com.google.common.base.Strings;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
public abstract class FieldMetadata<T> {
    private final String name;
    private final String comment;
    private final Component displayName;
    private final Field field;
    public FieldMetadata(Field field, String name, String comment) {
        this.field = field;
        this.field.setAccessible(true);
        this.name = name;
        this.comment = comment;
        this.displayName = Component.translatable("config.chunkbychunk.option." + field.getName());
    }
    public String getName() {
        return name;
    }
    public Component getDisplayName() {
        return displayName;
    }
    public List<String> getComments() {
        if (Strings.isNullOrEmpty(comment)) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(comment);
        }
    }
    @SuppressWarnings("unchecked")
    public T getValue(Object object) {
        try {
            return (T) field.get(object);
        } catch (IllegalAccessException e) {
            throw new ConfigException("Failed to retrieve " + getName() + " from object " + object, e);
        }
    }
    public void setValue(Object object, T value) {
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new ConfigException("Failed to set " + getName() + " to value " + value, e);
        }
    }
    public abstract String serializeValue(Object object);
    public abstract void deserializeValue(Object object, String value);
}
