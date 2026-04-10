package com.onec.ui;

import com.onec.metadata.AccumulationRegisterDescriptor;
import com.onec.metadata.AttributeDescriptor;
import com.onec.metadata.MetadataRegistry;
import com.onec.model.AccumulationType;

import org.jdbi.v3.core.Jdbi;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ui/registers")
public class GenericRegisterController {

    private final MetadataRegistry registry;
    private final Jdbi jdbi;
    private final RefResolver refResolver;

    public GenericRegisterController(MetadataRegistry registry, Jdbi jdbi) {
        this.registry = registry;
        this.jdbi = jdbi;
        this.refResolver = new RefResolver(registry, jdbi);
    }

    @GetMapping("/{name}/movements")
    public List<Map<String, Object>> movements(@PathVariable String name,
                                                @RequestParam(required = false) String from,
                                                @RequestParam(required = false) String to) {
        AccumulationRegisterDescriptor desc = findRegister(name);
        StringBuilder sql = new StringBuilder("SELECT * FROM " + desc.tableName() + " WHERE _active = true");

        if (from != null) sql.append(" AND _period >= :from");
        if (to != null) sql.append(" AND _period <= :to");
        sql.append(" ORDER BY _period DESC");

        List<Map<String, Object>> rows = jdbi.withHandle(h -> {
            var query = h.createQuery(sql.toString());
            if (from != null) query.bind("from", from);
            if (to != null) query.bind("to", to);
            return query.mapToMap().list();
        });
        resolveAll(desc, rows);
        return rows;
    }

    @GetMapping("/{name}/balance")
    public List<Map<String, Object>> balance(@PathVariable String name,
                                              @RequestParam Map<String, String> filters) {
        AccumulationRegisterDescriptor desc = findRegister(name);
        if (desc.accumulationType() != AccumulationType.BALANCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Balance is only available for BALANCE registers");
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM " + desc.totalsTableName());
        List<String> conditions = desc.dimensions().stream()
                .filter(d -> filters.containsKey(d.fieldName()))
                .map(d -> d.columnName() + " = :" + d.columnName())
                .toList();

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        List<Map<String, Object>> rows = jdbi.withHandle(h -> {
            var query = h.createQuery(sql.toString());
            for (AttributeDescriptor dim : desc.dimensions()) {
                if (filters.containsKey(dim.fieldName())) {
                    query.bind(dim.columnName(), filters.get(dim.fieldName()));
                }
            }
            return query.mapToMap().list();
        });
        resolveAll(desc, rows);
        return rows;
    }

    @GetMapping("/{name}/turnover")
    public List<Map<String, Object>> turnover(@PathVariable String name,
                                               @RequestParam String from,
                                               @RequestParam String to,
                                               @RequestParam Map<String, String> allParams) {
        AccumulationRegisterDescriptor desc = findRegister(name);

        String dimColumns = desc.dimensions().stream()
                .map(AttributeDescriptor::columnName)
                .collect(Collectors.joining(", "));

        String resourceSums = desc.resources().stream()
                .map(r -> "SUM(" + r.columnName() + ") AS " + r.columnName())
                .collect(Collectors.joining(", "));

        StringBuilder sql = new StringBuilder("SELECT ");
        if (!dimColumns.isEmpty()) {
            sql.append(dimColumns).append(", ");
        }
        sql.append(resourceSums);
        sql.append(" FROM ").append(desc.tableName());
        sql.append(" WHERE _active = true AND _period >= :from AND _period <= :to");

        for (AttributeDescriptor dim : desc.dimensions()) {
            if (allParams.containsKey(dim.fieldName())) {
                sql.append(" AND ").append(dim.columnName()).append(" = :").append(dim.columnName());
            }
        }

        if (!dimColumns.isEmpty()) {
            sql.append(" GROUP BY ").append(dimColumns);
        }

        List<Map<String, Object>> rows = jdbi.withHandle(h -> {
            var query = h.createQuery(sql.toString())
                    .bind("from", from)
                    .bind("to", to);

            for (AttributeDescriptor dim : desc.dimensions()) {
                if (allParams.containsKey(dim.fieldName())) {
                    query.bind(dim.columnName(), allParams.get(dim.fieldName()));
                }
            }
            return query.mapToMap().list();
        });
        resolveAll(desc, rows);
        return rows;
    }

    private void resolveAll(AccumulationRegisterDescriptor desc, List<Map<String, Object>> rows) {
        List<AttributeDescriptor> all = new ArrayList<>();
        all.addAll(desc.dimensions());
        all.addAll(desc.resources());
        refResolver.resolveAttributes(rows, all);
    }

    private AccumulationRegisterDescriptor findRegister(String name) {
        String normalized = name.replace("_", "").toLowerCase();
        return registry.allRegisters().stream()
                .filter(d -> d.logicalName().replace(" ", "").toLowerCase().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Register not found: " + name));
    }
}
