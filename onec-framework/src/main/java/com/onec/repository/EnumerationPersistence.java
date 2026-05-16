package com.onec.repository;

import com.onec.metadata.EnumerationDescriptor;
import com.onec.metadata.EnumerationValueDescriptor;

import org.jdbi.v3.core.Jdbi;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class EnumerationPersistence {

    private final Jdbi jdbi;

    public EnumerationPersistence(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void populateValues(EnumerationDescriptor descriptor) {
        jdbi.useHandle(handle -> {
            for (EnumerationValueDescriptor v : descriptor.values()) {
                handle.createUpdate("MERGE INTO " + descriptor.tableName() +
                                " (_id, _name, _order) KEY(_id) VALUES (:id, :name, :order)")
                        .bind("id", v.id())
                        .bind("name", v.name())
                        .bind("order", v.order())
                        .execute();
            }
        });
    }

    public static UUID resolveId(Class<? extends Enum<?>> enumClass, Enum<?> value) {
        return UUID.nameUUIDFromBytes(
                (enumClass.getName() + "." + value.name()).getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends Enum<E>> E resolveValue(Class<?> enumClass, UUID id) {
        Class<E> ec = (Class<E>) enumClass;
        for (E constant : ec.getEnumConstants()) {
            UUID candidateId = UUID.nameUUIDFromBytes(
                    (enumClass.getName() + "." + constant.name()).getBytes(StandardCharsets.UTF_8));
            if (candidateId.equals(id)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("No enum value found for UUID " + id + " in " + enumClass.getName());
    }
}
