package com.onec.metadata;

public record AttributeDescriptor(
        String fieldName,
        String displayName,
        String columnName,
        Class<?> javaType,
        int length,
        boolean required,
        boolean isRef,
        int precision,
        int scale
) {
}
