package com.onec.metadata;

import java.util.UUID;

public record EnumerationValueDescriptor(
        String name,
        UUID id,
        int order
) {
}
