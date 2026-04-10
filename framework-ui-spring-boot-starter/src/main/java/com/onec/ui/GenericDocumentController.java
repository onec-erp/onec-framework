package com.onec.ui;

import com.onec.metadata.AttributeDescriptor;
import com.onec.metadata.DocumentDescriptor;
import com.onec.metadata.MetadataRegistry;
import com.onec.metadata.TabularSectionDescriptor;
import com.onec.model.DocumentObject;
import com.onec.model.TabularSectionRow;
import com.onec.posting.PostingService;
import com.onec.types.Ref;

import org.jdbi.v3.core.Jdbi;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ui/documents")
public class GenericDocumentController {

    private final MetadataRegistry registry;
    private final Jdbi jdbi;
    private final UiProperties properties;
    private final PostingService postingService;
    private final RefResolver refResolver;

    public GenericDocumentController(MetadataRegistry registry, Jdbi jdbi, UiProperties properties,
                                      PostingService postingService) {
        this.registry = registry;
        this.jdbi = jdbi;
        this.properties = properties;
        this.postingService = postingService;
        this.refResolver = new RefResolver(registry, jdbi);
    }

    @GetMapping("/{name}")
    public List<Map<String, Object>> list(@PathVariable String name,
                                           @RequestParam(required = false) String from,
                                           @RequestParam(required = false) String to) {
        DocumentDescriptor desc = findDocument(name);
        StringBuilder sql = new StringBuilder("SELECT * FROM " + desc.tableName());
        if (from != null || to != null) {
            sql.append(" WHERE 1=1");
            if (from != null) sql.append(" AND _date >= :from");
            if (to != null) sql.append(" AND _date <= :to");
        }
        sql.append(" ORDER BY _date DESC");

        List<Map<String, Object>> rows = jdbi.withHandle(h -> {
            var query = h.createQuery(sql.toString());
            if (from != null) query.bind("from", from);
            if (to != null) query.bind("to", to);
            return query.mapToMap().list();
        });
        refResolver.resolveAttributes(rows, desc.attributes());
        return rows;
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

        refResolver.resolveAttributes(List.of(doc), desc.attributes());

        // Load tabular sections
        for (TabularSectionDescriptor ts : desc.tabularSections()) {
            List<Map<String, Object>> rows = jdbi.withHandle(h ->
                    h.createQuery("SELECT * FROM " + ts.tableName() +
                                    " WHERE _parent_id = :parentId ORDER BY _line_number")
                            .bind("parentId", id)
                            .mapToMap()
                            .list()
            );
            refResolver.resolveAttributes(rows, ts.attributes());
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

    @PostMapping("/{name}/{id}/post")
    public Map<String, Object> post(@PathVariable String name, @PathVariable UUID id) {
        requireWritable();
        DocumentDescriptor desc = findDocument(name);
        DocumentObject doc = loadDocumentObject(desc, id);
        postingService.post(doc);
        return get(name, id);
    }

    @PostMapping("/{name}/{id}/unpost")
    public Map<String, Object> unpost(@PathVariable String name, @PathVariable UUID id) {
        requireWritable();
        DocumentDescriptor desc = findDocument(name);
        DocumentObject doc = loadDocumentObject(desc, id);
        postingService.unpost(doc);
        return get(name, id);
    }

    @SuppressWarnings("unchecked")
    private DocumentObject loadDocumentObject(DocumentDescriptor desc, UUID id) {
        Map<String, Object> raw = jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM " + desc.tableName() + " WHERE _id = :id")
                        .bind("id", id)
                        .mapToMap()
                        .findOne()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        );

        try {
            DocumentObject doc = (DocumentObject) desc.javaClass().getDeclaredConstructor().newInstance();
            doc.setId(id);
            doc.setNumber((String) raw.get("_number"));
            Object dateVal = raw.get("_date");
            if (dateVal instanceof LocalDateTime ldt) {
                doc.setDate(ldt);
            } else if (dateVal instanceof java.sql.Timestamp ts) {
                doc.setDate(ts.toLocalDateTime());
            } else if (dateVal != null) {
                doc.setDate(LocalDateTime.parse(dateVal.toString().replace(' ', 'T')));
            }
            doc.setPosted(Boolean.TRUE.equals(raw.get("_posted")));
            doc.setDeletionMark(Boolean.TRUE.equals(raw.get("_deletion_mark")));
            doc.setNew(false);

            for (AttributeDescriptor attr : desc.attributes()) {
                setFieldValue(doc, attr, raw.get(attr.columnName()));
            }

            for (TabularSectionDescriptor ts : desc.tabularSections()) {
                List<Map<String, Object>> rows = jdbi.withHandle(h ->
                        h.createQuery("SELECT * FROM " + ts.tableName() +
                                        " WHERE _parent_id = :parentId ORDER BY _line_number")
                                .bind("parentId", id)
                                .mapToMap()
                                .list()
                );

                List<TabularSectionRow> rowObjects = new ArrayList<>();
                for (Map<String, Object> rowData : rows) {
                    TabularSectionRow rowObj = (TabularSectionRow) ts.rowClass()
                            .getDeclaredConstructor().newInstance();
                    Object rowId = rowData.get("_id");
                    if (rowId instanceof UUID uuid) {
                        rowObj.setId(uuid);
                    } else if (rowId != null) {
                        rowObj.setId(UUID.fromString(rowId.toString()));
                    }
                    Object ln = rowData.get("_line_number");
                    if (ln instanceof Number num) {
                        rowObj.setLineNumber(num.intValue());
                    }

                    for (AttributeDescriptor rowAttr : ts.attributes()) {
                        setFieldValue(rowObj, rowAttr, rowData.get(rowAttr.columnName()));
                    }
                    rowObjects.add(rowObj);
                }

                Field listField = findField(desc.javaClass(), ts.fieldName());
                if (listField != null) {
                    listField.setAccessible(true);
                    listField.set(doc, rowObjects);
                }
            }

            return doc;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to reconstruct document: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void setFieldValue(Object target, AttributeDescriptor attr, Object value) throws Exception {
        Field field = findField(target.getClass(), attr.fieldName());
        if (field == null || value == null) return;
        field.setAccessible(true);

        Class<?> fieldType = field.getType();

        if (Ref.class.isAssignableFrom(fieldType)) {
            java.lang.reflect.Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType pt) {
                Class<?> refTargetClass = (Class<?>) pt.getActualTypeArguments()[0];
                UUID refId = value instanceof UUID u ? u : UUID.fromString(value.toString());
                field.set(target, Ref.of(refTargetClass, refId));
            }
        } else if (fieldType == BigDecimal.class) {
            field.set(target, value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString()));
        } else if (fieldType == String.class) {
            field.set(target, value.toString());
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(target, value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString()));
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(target, value instanceof Number n ? n.longValue() : Long.parseLong(value.toString()));
        } else if (fieldType == double.class || fieldType == Double.class) {
            field.set(target, value instanceof Number n ? n.doubleValue() : Double.parseDouble(value.toString()));
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            field.set(target, Boolean.TRUE.equals(value));
        } else if (fieldType == LocalDate.class) {
            field.set(target, value instanceof LocalDate ld ? ld : LocalDate.parse(value.toString()));
        } else if (fieldType == LocalDateTime.class) {
            field.set(target, value instanceof LocalDateTime ldt ? ldt : LocalDateTime.parse(value.toString()));
        } else if (fieldType == UUID.class) {
            field.set(target, value instanceof UUID u ? u : UUID.fromString(value.toString()));
        } else {
            field.set(target, value);
        }
    }

    private Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
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
