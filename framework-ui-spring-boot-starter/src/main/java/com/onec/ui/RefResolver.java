package com.onec.ui;

import com.onec.metadata.*;

import org.jdbi.v3.core.Jdbi;

import java.util.*;

/**
 * Resolves Ref UUID columns and Enum UUID columns to human-readable display values.
 * Adds a "{columnName}_display" key to each row map.
 */
public class RefResolver {

    private final MetadataRegistry registry;
    private final Jdbi jdbi;

    public RefResolver(MetadataRegistry registry, Jdbi jdbi) {
        this.registry = registry;
        this.jdbi = jdbi;
    }

    public void resolveAttributes(List<Map<String, Object>> rows, List<AttributeDescriptor> attributes) {
        for (AttributeDescriptor attr : attributes) {
            if (attr.isRef() && attr.refTarget() != null) {
                resolveRefColumn(rows, attr);
            } else if (attr.javaType().isEnum()) {
                resolveEnumColumn(rows, attr);
            }
        }
    }

    private void resolveRefColumn(List<Map<String, Object>> rows, AttributeDescriptor attr) {
        Set<UUID> ids = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Object val = row.get(attr.columnName());
            if (val != null) {
                ids.add(toUUID(val));
            }
        }
        if (ids.isEmpty()) return;

        CatalogDescriptor catalog = registry.allCatalogs().stream()
                .filter(c -> c.logicalName().equals(attr.refTarget()))
                .findFirst().orElse(null);
        if (catalog == null) return;

        Map<UUID, String> displayMap = jdbi.withHandle(h ->
                h.createQuery("SELECT _id, _description FROM " + catalog.tableName() +
                                " WHERE _id IN (<ids>)")
                        .bindList("ids", new ArrayList<>(ids))
                        .reduceRows(new HashMap<>(), (map, rv) -> {
                            map.put(rv.getColumn("_id", UUID.class),
                                    rv.getColumn("_description", String.class));
                            return map;
                        })
        );

        for (Map<String, Object> row : rows) {
            Object val = row.get(attr.columnName());
            if (val != null) {
                String display = displayMap.get(toUUID(val));
                row.put(attr.columnName() + "_display", display != null ? display : val.toString());
            }
        }
    }

    private void resolveEnumColumn(List<Map<String, Object>> rows, AttributeDescriptor attr) {
        EnumerationDescriptor enumDesc = registry.allEnumerations().stream()
                .filter(e -> e.javaClass().equals(attr.javaType()))
                .findFirst().orElse(null);
        if (enumDesc == null) return;

        Map<String, String> idToName = new HashMap<>();
        for (EnumerationValueDescriptor v : enumDesc.values()) {
            idToName.put(v.id().toString(), v.name());
        }

        for (Map<String, Object> row : rows) {
            Object val = row.get(attr.columnName());
            if (val != null) {
                String name = idToName.get(val.toString());
                row.put(attr.columnName() + "_display", name != null ? name : val.toString());
            }
        }
    }

    private static UUID toUUID(Object val) {
        return val instanceof UUID u ? u : UUID.fromString(val.toString());
    }
}
