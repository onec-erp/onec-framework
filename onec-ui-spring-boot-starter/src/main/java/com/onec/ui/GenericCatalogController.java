package com.onec.ui;

import com.onec.metadata.AttributeDescriptor;
import com.onec.metadata.CatalogDescriptor;
import com.onec.metadata.MetadataRegistry;
import com.onec.numbering.NumberGenerator;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/ui/catalogs")
public class GenericCatalogController {

    private final MetadataRegistry registry;
    private final Jdbi jdbi;
    private final UiProperties properties;
    private final NumberGenerator numberGenerator;
    private final RefResolver refResolver;
    private final UiAccessService access;
    private final UiEventPublisher eventPublisher;

    public GenericCatalogController(MetadataRegistry registry, Jdbi jdbi, UiProperties properties,
                                    NumberGenerator numberGenerator,
                                    UiAccessService access,
                                    UiEventPublisher eventPublisher) {
        this.registry = registry;
        this.jdbi = jdbi;
        this.properties = properties;
        this.numberGenerator = numberGenerator;
        this.refResolver = new RefResolver(registry, jdbi);
        this.access = access;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/{name}")
    public List<Map<String, Object>> list(@PathVariable String name, Principal principal) {
        CatalogDescriptor desc = findCatalog(name);
        access.requireRead(principal, desc);
        List<Map<String, Object>> rows = jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM " + desc.tableName() + " WHERE _deletion_mark = false")
                        .mapToMap()
                        .list()
        );
        refResolver.resolveAttributes(rows, desc.attributes());
        return rows;
    }

    @GetMapping("/{name}/children")
    public List<Map<String, Object>> children(@PathVariable String name,
                                              @RequestParam(required = false) UUID parent,
                                              Principal principal) {
        CatalogDescriptor desc = findCatalog(name);
        access.requireRead(principal, desc);
        if (!desc.hierarchical()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Catalog is not hierarchical: " + name);
        }
        List<Map<String, Object>> rows = jdbi.withHandle(h -> {
            String sql = "SELECT * FROM " + desc.tableName() +
                    " WHERE _deletion_mark = false AND " +
                    (parent == null ? "_parent IS NULL" : "_parent = :parent") +
                    " ORDER BY _is_folder DESC, _description";
            var query = h.createQuery(sql);
            if (parent != null) query.bind("parent", parent);
            return query.mapToMap().list();
        });
        refResolver.resolveAttributes(rows, desc.attributes());
        return rows;
    }

    @GetMapping("/{name}/tree")
    public List<Map<String, Object>> tree(@PathVariable String name, Principal principal) {
        CatalogDescriptor desc = findCatalog(name);
        access.requireRead(principal, desc);
        if (!desc.hierarchical()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Catalog is not hierarchical: " + name);
        }
        List<Map<String, Object>> rows = jdbi.withHandle(h ->
                h.createQuery("SELECT * FROM " + desc.tableName() +
                                " WHERE _deletion_mark = false ORDER BY _is_folder DESC, _description")
                        .mapToMap()
                        .list()
        );
        refResolver.resolveAttributes(rows, desc.attributes());
        return buildTree(rows, null);
    }

    @GetMapping("/{name}/{id}")
    public Map<String, Object> get(@PathVariable String name, @PathVariable UUID id, Principal principal) {
        CatalogDescriptor desc = findCatalog(name);
        access.requireRead(principal, desc);
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
    public Map<String, Object> create(@PathVariable String name, @RequestBody Map<String, Object> body,
                                      Principal principal) {
        requireWritable();
        CatalogDescriptor desc = findCatalog(name);
        access.requireWrite(principal, desc);
        UUID id = UUID.randomUUID();

        List<String> columns = new ArrayList<>(List.of(
                "_id", "_code", "_description", "_deletion_mark", "_is_folder", "_parent", "_version"));
        List<String> values = new ArrayList<>(List.of(
                ":_id", ":_code", ":_description", ":_deletion_mark", ":_is_folder", ":_parent", ":_version"));

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
                    .bind("_code", resolveCode(desc, body))
                    .bind("_description", body.getOrDefault("description", ""))
                    .bind("_deletion_mark", false)
                    .bind("_is_folder", Boolean.TRUE.equals(body.get("folder")))
                    .bind("_parent", parseUuid(body.get("parent")))
                    .bind("_version", 0);

            for (AttributeDescriptor attr : desc.attributes()) {
                bindAttribute(update, attr, body.get(attr.fieldName()));
            }
            update.execute();
        });

        eventPublisher.publish("created", "catalog", desc.logicalName(), id);
        return get(name, id, principal);
    }

    @PutMapping("/{name}/{id}")
    public Map<String, Object> update(@PathVariable String name, @PathVariable UUID id,
                                       @RequestBody Map<String, Object> body,
                                       Principal principal) {
        requireWritable();
        CatalogDescriptor desc = findCatalog(name);
        access.requireWrite(principal, desc);

        List<String> setClauses = new ArrayList<>();
        if (body.containsKey("code")) setClauses.add("_code = :_code");
        if (body.containsKey("description")) setClauses.add("_description = :_description");
        if (body.containsKey("folder")) setClauses.add("_is_folder = :_is_folder");
        if (body.containsKey("parent")) setClauses.add("_parent = :_parent");

        for (AttributeDescriptor attr : desc.attributes()) {
            if (body.containsKey(attr.fieldName())) {
                setClauses.add(attr.columnName() + " = :" + attr.columnName());
            }
        }

        if (setClauses.isEmpty()) {
            return get(name, id, principal);
        }

        setClauses.add("_version = _version + 1");

        boolean hasExpectedVersion = body.containsKey("version") || body.containsKey("_version");
        String sql = "UPDATE " + desc.tableName() +
                " SET " + String.join(", ", setClauses) +
                " WHERE _id = :_id" + (hasExpectedVersion ? " AND _version = :_expected_version" : "");

        int updated = jdbi.withHandle(h -> {
            var update = h.createUpdate(sql).bind("_id", id);
            if (body.containsKey("code")) update.bind("_code", body.get("code"));
            if (body.containsKey("description")) update.bind("_description", body.get("description"));
            if (body.containsKey("folder")) update.bind("_is_folder", Boolean.TRUE.equals(body.get("folder")));
            if (body.containsKey("parent")) update.bind("_parent", parseUuid(body.get("parent")));
            if (hasExpectedVersion) {
                update.bind("_expected_version", parseInt(body.getOrDefault("version", body.get("_version"))));
            }

            for (AttributeDescriptor attr : desc.attributes()) {
                if (body.containsKey(attr.fieldName())) {
                    update.bind(attr.columnName(), body.get(attr.fieldName()));
                }
            }
            return update.execute();
        });
        if (updated == 0 && hasExpectedVersion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Catalog item was changed by another transaction: " + id);
        }

        eventPublisher.publish("updated", "catalog", desc.logicalName(), id);
        return get(name, id, principal);
    }

    @DeleteMapping("/{name}/{id}")
    public void delete(@PathVariable String name, @PathVariable UUID id, Principal principal) {
        requireWritable();
        CatalogDescriptor desc = findCatalog(name);
        access.requireWrite(principal, desc);
        jdbi.useHandle(h ->
                h.createUpdate("UPDATE " + desc.tableName() + " SET _deletion_mark = true WHERE _id = :id")
                        .bind("id", id)
                        .execute()
        );
        eventPublisher.publish("deleted", "catalog", desc.logicalName(), id);
    }

    private CatalogDescriptor findCatalog(String name) {
        String normalized = name.replace("_", "").toLowerCase();
        return registry.allCatalogs().stream()
                .filter(d -> d.logicalName().replace(" ", "").toLowerCase().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Catalog not found: " + name));
    }

    private void bindAttribute(Update update, AttributeDescriptor attr, Object value) {
        if (value == null || "".equals(value)) {
            update.bind(attr.columnName(), (UUID) null);
            return;
        }
        if (attr.isRef() || attr.javaType().isEnum()) {
            UUID uuid = value instanceof UUID u ? u : UUID.fromString(value.toString());
            update.bind(attr.columnName(), uuid);
        } else if (attr.javaType() == BigDecimal.class) {
            update.bind(attr.columnName(), value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString()));
        } else {
            update.bind(attr.columnName(), value);
        }
    }

    private UUID parseUuid(Object value) {
        if (value == null || "".equals(value)) return null;
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    private int parseInt(Object value) {
        return value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
    }

    private List<Map<String, Object>> buildTree(List<Map<String, Object>> rows, UUID parent) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            UUID rowParent = parseUuid(row.get("_parent"));
            if (!Objects.equals(rowParent, parent)) continue;
            Map<String, Object> copy = new LinkedHashMap<>(row);
            copy.put("children", buildTree(rows, parseUuid(row.get("_id"))));
            result.add(copy);
        }
        return result;
    }

    private void requireWritable() {
        if (properties.isReadOnly()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "UI is in read-only mode");
        }
    }

    private String resolveCode(CatalogDescriptor desc, Map<String, Object> body) {
        Object explicit = body.get("code");
        if (explicit != null && !explicit.toString().isBlank()) {
            return explicit.toString();
        }
        if (!desc.autoNumber()) {
            return "";
        }
        return numberGenerator.nextCode(desc.tableName(), desc.codePrefix(), desc.codeLength());
    }
}
