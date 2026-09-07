/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */

package com.ryvione.gatheringchunks.config.system;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ListFieldMetadata extends FieldMetadata<List<String>> {

    public ListFieldMetadata(Field field, String name, String comment) {
        super(field, name, comment);
    }

    @Override
    public String serializeValue(Object object) {
        List<String> list = getValue(object);
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(list.get(i)).append('"');
        }
        return sb.append(']').toString();
    }

    @Override
    public void deserializeValue(Object object, String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        List<String> list = new ArrayList<>();
        for (String entry : trimmed.split(",")) {
            String item = entry.trim();
            if (item.startsWith("\"")) {
                item = item.substring(1);
            }
            if (item.endsWith("\"") && item.length() > 1) {
                item = item.substring(0, item.length() - 1);
            }
            item = item.trim();
            if (!item.isEmpty()) {
                list.add(item);
            }
        }
        setValue(object, list);
    }
}
