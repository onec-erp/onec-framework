package com.onec.ui;

import com.onec.annotations.UiSection;
import com.onec.metadata.*;
import com.onec.ui.UiLayout;
import com.onec.ui.UiLayoutResolver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ui/metadata")
public class MetadataApiController {

    private final MetadataRegistry registry;
    private final UiLayout uiLayout;
    private final UiLayoutResolver layoutResolver;
    private final UiAccessService access;

    public MetadataApiController(MetadataRegistry registry,
                                  UiLayout uiLayout,
                                  UiLayoutResolver layoutResolver,
                                  UiAccessService access) {
        this.registry = registry;
        this.uiLayout = uiLayout;
        this.layoutResolver = layoutResolver;
        this.access = access;
    }

    @GetMapping("/layout")
    public List<UiLayout.ResolvedSection> layout(Principal principal) {
        return layoutResolver.resolve(uiLayout).stream()
                .map(section -> new UiLayout.ResolvedSection(
                        section.name(),
                        section.order(),
                        section.icon(),
                        section.placement(),
                        section.items().stream()
                                .filter(item -> access.canRead(principal, item.type(), item.name()))
                                .toList()))
                .filter(section -> !section.items().isEmpty())
                .toList();
    }

    @GetMapping("/catalogs")
    public List<Map<String, Object>> catalogs(Principal principal) {
        return registry.allCatalogs().stream()
                .filter(d -> access.canRead(principal, d))
                .map(this::describeCatalog)
                .toList();
    }

    @GetMapping("/documents")
    public List<Map<String, Object>> documents(Principal principal) {
        return registry.allDocuments().stream()
                .filter(d -> access.canRead(principal, d))
                .map(this::describeDocument)
                .toList();
    }

    @GetMapping("/dashboard")
    public List<Map<String, Object>> dashboard(Principal principal) {
        return layoutResolver.resolveWidgets(uiLayout).stream()
                .filter(w -> access.canRead(principal, w.entityType(), w.entityName()))
                .map(w -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("title", w.title());
                    map.put("widgetType", w.widgetType());
                    map.put("order", w.order());
                    map.put("width", w.width());
                    map.put("entityType", w.entityType());
                    map.put("entityName", w.entityName());
                    map.put("maxItems", w.maxItems());
                    map.put("dateField", w.dateField());
                    map.put("titleField", w.titleField());
                    map.put("extraConfig", w.extraConfig());
                    return map;
                })
                .toList();
    }

    @GetMapping("/manifest")
    public BusinessModelManifest manifest(Principal principal) {
        BusinessModelManifest manifest = new BusinessModelManifestBuilder(registry).build();
        return new BusinessModelManifest(
                manifest.schemaVersion(),
                manifest.catalogs().stream()
                        .filter(c -> access.canRead(principal, "catalog", c.name()))
                        .toList(),
                manifest.documents().stream()
                        .filter(d -> access.canRead(principal, "document", d.name()))
                        .toList(),
                manifest.accumulationRegisters().stream()
                        .filter(r -> access.canRead(principal, "register", r.name()))
                        .toList(),
                manifest.informationRegisters().stream()
                        .filter(r -> registry.allInformationRegisters().stream()
                                .filter(desc -> desc.logicalName().equals(r.name()))
                                .anyMatch(desc -> access.canRead(principal, desc)))
                        .toList(),
                manifest.enumerations(),
                manifest.constants(),
                manifest.dashboardWidgets().stream()
                        .filter(w -> access.canRead(principal, w.entityType(), w.entityName()))
                        .toList());
    }

    @GetMapping("/registers")
    public List<Map<String, Object>> registers(Principal principal) {
        return registry.allRegisters().stream()
                .filter(d -> access.canRead(principal, d))
                .map(this::describeRegister)
                .toList();
    }

    private Map<String, Object> describeCatalog(CatalogDescriptor d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", d.logicalName());
        map.put("tableName", d.tableName());
        map.put("codeLength", d.codeLength());
        map.put("hierarchical", d.hierarchical());
        map.put("autoNumber", d.autoNumber());
        map.put("codePrefix", d.codePrefix());
        map.put("context", d.context());
        map.put("readRoles", d.readRoles());
        map.put("writeRoles", d.writeRoles());
        map.put("attributes", describeAttributes(d.attributes()));
        addSectionInfo(map, d.javaClass());
        return map;
    }

    private Map<String, Object> describeDocument(DocumentDescriptor d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", d.logicalName());
        map.put("tableName", d.tableName());
        map.put("numberLength", d.numberLength());
        map.put("autoNumber", d.autoNumber());
        map.put("numberPrefix", d.numberPrefix());
        map.put("context", d.context());
        map.put("readRoles", d.readRoles());
        map.put("writeRoles", d.writeRoles());
        map.put("attributes", describeAttributes(d.attributes()));
        map.put("tabularSections", d.tabularSections().stream().map(ts -> {
            Map<String, Object> tsMap = new LinkedHashMap<>();
            tsMap.put("name", ts.name());
            tsMap.put("tableName", ts.tableName());
            tsMap.put("attributes", describeAttributes(ts.attributes()));
            return tsMap;
        }).toList());
        addSectionInfo(map, d.javaClass());
        return map;
    }

    private Map<String, Object> describeRegister(AccumulationRegisterDescriptor d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", d.logicalName());
        map.put("tableName", d.tableName());
        map.put("type", d.accumulationType().name());
        map.put("context", d.context());
        map.put("readRoles", d.readRoles());
        map.put("writeRoles", d.writeRoles());
        map.put("dimensions", describeAttributes(d.dimensions()));
        map.put("resources", describeAttributes(d.resources()));
        addSectionInfo(map, d.javaClass());
        return map;
    }

    private void addSectionInfo(Map<String, Object> map, Class<?> javaClass) {
        UiSection section = javaClass.getAnnotation(UiSection.class);
        map.put("section", section != null ? section.value() : null);
        map.put("sectionOrder", section != null ? section.order() : null);
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
            map.put("refTarget", a.refTarget());
            map.put("precision", a.precision());
            map.put("scale", a.scale());
            map.put("visibleInList", a.visibleInList());
            map.put("visibleInForm", a.visibleInForm());
            map.put("visibleInDetail", a.visibleInDetail());
            map.put("order", a.order());
            map.put("group", a.group());
            map.put("widthHint", a.widthHint());
            map.put("widget", a.widget());
            boolean isEnum = a.javaType().isEnum();
            map.put("isEnum", isEnum);
            if (isEnum) {
                EnumerationDescriptor enumDesc = registry.allEnumerations().stream()
                        .filter(e -> e.javaClass().equals(a.javaType()))
                        .findFirst().orElse(null);
                if (enumDesc != null) {
                    map.put("enumName", enumDesc.logicalName());
                    map.put("enumValues", enumDesc.values().stream().map(v -> {
                        Map<String, Object> vm = new LinkedHashMap<>();
                        vm.put("name", v.name());
                        vm.put("id", v.id().toString());
                        return vm;
                    }).toList());
                }
            }
            return map;
        }).toList();
    }
}
