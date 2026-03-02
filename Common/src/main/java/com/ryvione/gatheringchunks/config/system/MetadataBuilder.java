package com.ryvione.gatheringchunks.config.system;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class MetadataBuilder {
    private MetadataBuilder() {
    }

    public static ConfigMetadata build(Class<?> type) {
        List<FieldMetadata<?>> fields = processFields(type);
        List<SectionMetadata> sections = new ArrayList<>();
        for (Field declaredField : type.getDeclaredFields()) {
            if (Modifier.isStatic(declaredField.getModifiers())) continue;
            if (isObjectSection(declaredField.getType())) {
                sections.add(processSection(declaredField.getType(), declaredField, ""));
            }
        }
        return new ConfigMetadata(sections, fields);
    }

    private static SectionMetadata processSection(Class<?> type, Field field, String parentPrefix) {
        String name = getName(field);
        String fullName = parentPrefix.isEmpty() ? name : parentPrefix + "." + name;
        List<FieldMetadata<?>> fields = processFields(type);
        List<SectionMetadata> subsections = new ArrayList<>();
        for (Field declaredField : type.getDeclaredFields()) {
            if (Modifier.isStatic(declaredField.getModifiers())) continue;
            if (isObjectSection(declaredField.getType())) {
                subsections.add(processSection(declaredField.getType(), declaredField, fullName));
            }
        }
        return new SectionMetadata(fullName, fields, field, subsections);
    }

    static boolean isObjectSection(Class<?> fieldType) {
        return Object.class.isAssignableFrom(fieldType)
                && !Enum.class.isAssignableFrom(fieldType)
                && !String.class.isAssignableFrom(fieldType)
                && !List.class.isAssignableFrom(fieldType);
    }

    private static List<FieldMetadata<?>> processFields(Class<?> type) {
        List<FieldMetadata<?>> fields = new ArrayList<>();
        for (Field declaredField : type.getDeclaredFields()) {
            Class<?> fieldType = declaredField.getType();
            if (Integer.TYPE.equals(fieldType)) {
                fields.add(processIntField(declaredField));
            } else if (Boolean.TYPE.equals(fieldType)) {
                fields.add(processBooleanField(declaredField));
            } else if (Enum.class.isAssignableFrom(fieldType)) {
                fields.add(processEnumField(declaredField));
            } else if (String.class.isAssignableFrom(fieldType)) {
                fields.add(processStringField(declaredField));
            }
        }
        return fields;
    }

    private static FieldMetadata<?> processStringField(Field field) {
        return new StringFieldMetadata(field, getName(field), getComment(field));
    }

    private static FieldMetadata<?> processEnumField(Field field) {
        return new EnumFieldMetadata(field, getName(field), getComment(field));
    }

    private static FieldMetadata<?> processBooleanField(Field field) {
        return new BooleanFieldMetadata(field, getName(field), getComment(field));
    }

    private static FieldMetadata<?> processIntField(Field field) {
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        IntRange intRange = field.getAnnotation(IntRange.class);
        if (intRange != null) {
            min = intRange.min();
            max = intRange.max();
        }
        return new IntFieldMetadata(field, getName(field), getComment(field), min, max);
    }

    private static String getName(Field field) {
        Name annotation = field.getAnnotation(Name.class);
        return annotation != null ? annotation.value() : field.getName();
    }

    private static String getComment(Field field) {
        Comment annotation = field.getAnnotation(Comment.class);
        return annotation != null ? annotation.value() : "";
    }
}