package com.onec.ui;

import com.onec.metadata.AttributeDescriptor;
import com.onec.metadata.CatalogDescriptor;
import com.onec.metadata.MetadataRegistry;

import org.jdbi.v3.core.Jdbi;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/ui/catalogs")
public class GenericCatalogController {

    private final MetadataRegistry registry;
    private final Jdbi jdbi;
    private final UiProperties properties;
    private final RefResolver refResolver;

    public GenericCatalogController(MetadataRegistry registry, Jdbi jdbi, UiProperties properties) {
        this.registry = registry;
        this.jdbi = jdbi;
        this.properties = properties;
        this.refResolver = new RefResolver(registry, jdbi);
    }

    @GetMapping("/{name}")
    public List<Map<String, Object>> list(@PathVariable String name) {
        CatalogDescriptor desc = findCatalog(name);
        List<Map<String, Object>> rows = jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM " + desc.tableName())
                        .mapToMap()
                        .list()
        );
        refResolver.resolveAttributes(rows, desc.attributes());
        return rows;
    }

    @GetMapping("/{name}/{id}")
    public Map<String, Object> get(@PathVariable String name, @PathVariable UUID id) {
        CatalogDescriptor desc = findCatalog(name);
        Map<String, Object> row = jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM " + desc.tableName() + " WHERE _id = :id")
                        .bind("id", id)
                        .mapToMap()
                        .findOne()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        );
        refResolver.resolveAttributes(List.of(row), desc.attributes());
        return row;
    }

    @PostMapping("/{name}")
    public Map<String, Object> create(@PathVariable String name, @RequestBody Map<String, Object> body) {
        requireWritable();
        CatalogDescriptor desc = findCatalog(name);
        UUID id = UUID.randomUUID();

        List<String> columns = new ArrayList<>(List.of("_id", "_code", "_description", "_deletion_mark"));
        List<String> values = new ArrayList<>(List.of(":_id", ":_code", ":_description", ":_deletion_mark"));

        for (AttributeDescriptor attr : desc.attributes()) {
            columns.add(attr.columnName());
            values.add(":" + attr.columnName());
        }

        String sql = "INSERT INTO " + desc.tableName() +
                " (" + String.join(", ", columns) + ")" +
                " VALUES (" + String.join(", ", values) + ")";

        jdbi.useHandle(h -> {
            var update = h.createUpdate(sql)
                    .bind("_id", id)
                    .bind("_code", body.getOrDefault("code", ""))
                    .bind("_description", body.getOrDefault("description", ""))
                    .bind("_deletion_mark", false);

            for (AttributeDescriptor attr : desc.attributes()) {
                update.bind(attr.columnName(), body.get(attr.fieldName()));
            }
            update.execute();
        });

        return get(name, id);
    }

    @PutMapping("/{name}/{id}")
    public Map<String, Object> update(@PathVariable String name, @PathVariable UUID id,
                                       @RequestBody Map<String, Object> body) {
        requireWritable();
        CatalogDescriptor desc = findCatalog(name);

        List<String> setClauses = new ArrayList<>();
        if (body.containsKey("code")) setClauses.add("_code = :_code");
        if (body.containsKey("description")) setClauses.add("_description = :_description");

        for (AttributeDescriptor attr : desc.attributes()) {
            if (body.containsKey(attr.fieldName())) {
                setClauses.add(attr.columnName() + " = :" + attr.columnName());
            }
        }

        if (setClauses.isEmpty()) {
            return get(name, id);
        }

        String sql = "UPDATE " + desc.tableName() +
                " SET " + String.join(", ", setClauses) +
                " WHERE _id = :_id";

        jdbi.useHandle(h -> {
            var update = h.createUpdate(sql).bind("_id", id);
            if (body.containsKey("code")) update.bind("_code", body.get("code"));
            if (body.containsKey("description")) update.bind("_description", body.get("description"));

            for (AttributeDescriptor attr : desc.attributes()) {
                if (body.containsKey(attr.fieldName())) {
                    update.bind(attr.columnName(), body.get(attr.fieldName()));
                }
            }
            update.execute();
        });

        return get(name, id);
    }

    @DeleteMapping("/{name}/{id}")
    public void delete(@PathVariable String name, @PathVariable UUID id) {
        requireWritable();
        CatalogDescriptor desc = findCatalog(name);
        jdbi.useHandle(h ->
                h.createUpdate("UPDATE " + desc.tableName() + " SET _deletion_mark = true WHERE _id = :id")
                        .bind("id", id)
                        .execute()
        );
    }

    private CatalogDescriptor findCatalog(String name) {
        String normalized = name.replace("_", "").toLowerCase();
        return registry.allCatalogs().stream()
                .filter(d -> d.logicalName().replace(" ", "").toLowerCase().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Catalog not found: " + name));
    }

    private void requireWritable() {
        if (properties.isReadOnly()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "UI is in read-only mode");
        }
    }
}
