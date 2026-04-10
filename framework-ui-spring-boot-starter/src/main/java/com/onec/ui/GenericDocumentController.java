package com.onec.ui;

import com.onec.metadata.AttributeDescriptor;
import com.onec.metadata.DocumentDescriptor;
import com.onec.metadata.MetadataRegistry;
import com.onec.metadata.TabularSectionDescriptor;

import org.jdbi.v3.core.Jdbi;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/ui/documents")
public class GenericDocumentController {

    private final MetadataRegistry registry;
    private final Jdbi jdbi;
    private final UiProperties properties;

    public GenericDocumentController(MetadataRegistry registry, Jdbi jdbi, UiProperties properties) {
        this.registry = registry;
        this.jdbi = jdbi;
        this.properties = properties;
    }

    @GetMapping("/{name}")
    public List<Map<String, Object>> list(@PathVariable String name) {
        DocumentDescriptor desc = findDocument(name);
        return jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM " + desc.tableName() + " ORDER BY _date DESC")
                        .mapToMap()
                        .list()
        );
    }

    @GetMapping("/{name}/{id}")
    public Map<String, Object> get(@PathVariable String name, @PathVariable UUID id) {
        DocumentDescriptor desc = findDocument(name);
        Map<String, Object> doc = jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM " + desc.tableName() + " WHERE _id = :id")
                        .bind("id", id)
                        .mapToMap()
                        .findOne()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        );

        // Load tabular sections
        for (TabularSectionDescriptor ts : desc.tabularSections()) {
            List<Map<String, Object>> rows = jdbi.withHandle(h ->
                    h.createQuery("SELECT * FROM " + ts.tableName() +
                                    " WHERE _parent_id = :parentId ORDER BY _line_number")
                            .bind("parentId", id)
                            .mapToMap()
                            .list()
            );
            doc.put(ts.name(), rows);
        }

        return doc;
    }

    @PostMapping("/{name}")
    public Map<String, Object> create(@PathVariable String name, @RequestBody Map<String, Object> body) {
        requireWritable();
        DocumentDescriptor desc = findDocument(name);
        UUID id = UUID.randomUUID();

        List<String> columns = new ArrayList<>(List.of("_id", "_number", "_date", "_posted", "_deletion_mark"));
        List<String> values = new ArrayList<>(List.of(":_id", ":_number", ":_date", ":_posted", ":_deletion_mark"));

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
                    .bind("_number", body.getOrDefault("number", ""))
                    .bind("_date", body.getOrDefault("date", java.time.LocalDateTime.now().toString()))
                    .bind("_posted", false)
                    .bind("_deletion_mark", false);

            for (AttributeDescriptor attr : desc.attributes()) {
                update.bind(attr.columnName(), body.get(attr.fieldName()));
            }
            update.execute();
        });

        // Insert tabular section rows
        insertTabularSections(desc, id, body);

        return get(name, id);
    }

    @PutMapping("/{name}/{id}")
    public Map<String, Object> update(@PathVariable String name, @PathVariable UUID id,
                                       @RequestBody Map<String, Object> body) {
        requireWritable();
        DocumentDescriptor desc = findDocument(name);

        List<String> setClauses = new ArrayList<>();
        if (body.containsKey("number")) setClauses.add("_number = :_number");
        if (body.containsKey("date")) setClauses.add("_date = :_date");

        for (AttributeDescriptor attr : desc.attributes()) {
            if (body.containsKey(attr.fieldName())) {
                setClauses.add(attr.columnName() + " = :" + attr.columnName());
            }
        }

        if (!setClauses.isEmpty()) {
            String sql = "UPDATE " + desc.tableName() +
                    " SET " + String.join(", ", setClauses) +
                    " WHERE _id = :_id";

            jdbi.useHandle(h -> {
                var update = h.createUpdate(sql).bind("_id", id);
                if (body.containsKey("number")) update.bind("_number", body.get("number"));
                if (body.containsKey("date")) update.bind("_date", body.get("date"));

                for (AttributeDescriptor attr : desc.attributes()) {
                    if (body.containsKey(attr.fieldName())) {
                        update.bind(attr.columnName(), body.get(attr.fieldName()));
                    }
                }
                update.execute();
            });
        }

        // Re-insert tabular sections if provided
        for (TabularSectionDescriptor ts : desc.tabularSections()) {
            if (body.containsKey(ts.name())) {
                jdbi.useHandle(h ->
                        h.createUpdate("DELETE FROM " + ts.tableName() + " WHERE _parent_id = :parentId")
                                .bind("parentId", id)
                                .execute()
                );
            }
        }
        insertTabularSections(desc, id, body);

        return get(name, id);
    }

    @DeleteMapping("/{name}/{id}")
    public void delete(@PathVariable String name, @PathVariable UUID id) {
        requireWritable();
        DocumentDescriptor desc = findDocument(name);
        jdbi.useHandle(h ->
                h.createUpdate("UPDATE " + desc.tableName() + " SET _deletion_mark = true WHERE _id = :id")
                        .bind("id", id)
                        .execute()
        );
    }

    @SuppressWarnings("unchecked")
    private void insertTabularSections(DocumentDescriptor desc, UUID parentId, Map<String, Object> body) {
        for (TabularSectionDescriptor ts : desc.tabularSections()) {
            Object rawRows = body.get(ts.name());
            if (!(rawRows instanceof List<?> rows)) continue;

            int lineNumber = 1;
            for (Object rawRow : rows) {
                if (!(rawRow instanceof Map<?, ?> row)) continue;
                Map<String, Object> typedRow = (Map<String, Object>) row;

                List<String> columns = new ArrayList<>(List.of("_id", "_parent_id", "_line_number"));
                List<String> values = new ArrayList<>(List.of(":_id", ":_parent_id", ":_line_number"));

                for (AttributeDescriptor attr : ts.attributes()) {
                    columns.add(attr.columnName());
                    values.add(":" + attr.columnName());
                }

                String sql = "INSERT INTO " + ts.tableName() +
                        " (" + String.join(", ", columns) + ")" +
                        " VALUES (" + String.join(", ", values) + ")";

                int ln = lineNumber;
                jdbi.useHandle(h -> {
                    var update = h.createUpdate(sql)
                            .bind("_id", UUID.randomUUID())
                            .bind("_parent_id", parentId)
                            .bind("_line_number", ln);

                    for (AttributeDescriptor attr : ts.attributes()) {
                        update.bind(attr.columnName(), typedRow.get(attr.fieldName()));
                    }
                    update.execute();
                });
                lineNumber++;
            }
        }
    }

    private DocumentDescriptor findDocument(String name) {
        String normalized = name.replace("_", "").toLowerCase();
        return registry.allDocuments().stream()
                .filter(d -> d.logicalName().replace(" ", "").toLowerCase().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document not found: " + name));
    }

    private void requireWritable() {
        if (properties.isReadOnly()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "UI is in read-only mode");
        }
    }
}
