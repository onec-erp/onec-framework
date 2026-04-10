package com.onec.ui;

import com.onec.metadata.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ui/metadata")
public class MetadataApiController {

    private final MetadataRegistry registry;

    public MetadataApiController(MetadataRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/catalogs")
    public List<Map<String, Object>> catalogs() {
        return registry.allCatalogs().stream()
                .map(this::describeCatalog)
                .toList();
    }

    @GetMapping("/documents")
    public List<Map<String, Object>> documents() {
        return registry.allDocuments().stream()
                .map(this::describeDocument)
                .toList();
    }

    @GetMapping("/registers")
    public List<Map<String, Object>> registers() {
        return registry.allRegisters().stream()
                .map(this::describeRegister)
                .toList();
    }

    private Map<String, Object> describeCatalog(CatalogDescriptor d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", d.logicalName());
        map.put("tableName", d.tableName());
        map.put("codeLength", d.codeLength());
        map.put("attributes", describeAttributes(d.attributes()));
        return map;
    }

    private Map<String, Object> describeDocument(DocumentDescriptor d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", d.logicalName());
        map.put("tableName", d.tableName());
        map.put("numberLength", d.numberLength());
        map.put("attributes", describeAttributes(d.attributes()));
        map.put("tabularSections", d.tabularSections().stream().map(ts -> {
            Map<String, Object> tsMap = new LinkedHashMap<>();
            tsMap.put("name", ts.name());
            tsMap.put("tableName", ts.tableName());
            tsMap.put("attributes", describeAttributes(ts.attributes()));
            return tsMap;
        }).toList());
        return map;
    }

    private Map<String, Object> describeRegister(AccumulationRegisterDescriptor d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", d.logicalName());
        map.put("tableName", d.tableName());
        map.put("type", d.accumulationType().name());
        map.put("dimensions", describeAttributes(d.dimensions()));
        map.put("resources", describeAttributes(d.resources()));
        return map;
    }

    private List<Map<String, Object>> describeAttributes(List<AttributeDescriptor> attrs) {
        return attrs.stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("fieldName", a.fieldName());
            map.put("displayName", a.displayName());
            map.put("columnName", a.columnName());
            map.put("javaType", a.javaType().getSimpleName());
            map.put("length", a.length());
            map.put("required", a.required());
            map.put("isRef", a.isRef());
            map.put("precision", a.precision());
            map.put("scale", a.scale());
            return map;
        }).toList();
    }
}
