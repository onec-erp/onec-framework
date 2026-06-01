package com.onec.ui;

import com.onec.metadata.CatalogDescriptor;
import com.onec.metadata.DocumentDescriptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a per-entity {@link EntityView} (authored in code) over the
 * auto-generated metadata defaults into a renderer-agnostic {@link ResolvedListView}.
 * Entities without an EntityView fall back entirely to the defaults (system +
 * visible custom columns, in field-hint order), so adding a view is purely
 * additive. The DivKit emitter compiles the result; the model is renderer-neutral.
 */
public class UiViewResolver {

    private final ResolvedMetadataService metadata;
    private final Map<Class<?>, EntityView> views = new LinkedHashMap<>();

    public UiViewResolver(ResolvedMetadataService metadata, List<EntityView> entityViews) {
        this.metadata = metadata;
        for (EntityView view : entityViews) {
            if (view.entity() != null) {
                views.put(view.entity(), view);
            }
        }
    }

    public ResolvedListView catalogList(CatalogDescriptor d) {
        return resolveList(d.javaClass(), metadata.describeCatalog(d));
    }

    public ResolvedListView documentList(DocumentDescriptor d) {
        return resolveList(d.javaClass(), metadata.describeDocument(d));
    }

    @SuppressWarnings("unchecked")
    private ResolvedListView resolveList(Class<?> entity, Map<String, Object> meta) {
        ListSpec spec = new ListSpec();
        EntityView view = views.get(entity);
        if (view != null) {
            view.list(spec);
        }

        // Available columns by field name: built-in system columns first, then custom.
        Map<String, ColumnMeta> available = new LinkedHashMap<>();
        for (Map<String, Object> sc : (List<Map<String, Object>>) meta.getOrDefault("systemColumns", List.of())) {
            available.put(str(sc.get("fieldName")), ColumnMeta.of(sc));
        }
        for (Map<String, Object> a : (List<Map<String, Object>>) meta.getOrDefault("attributes", List.of())) {
            available.put(str(a.get("fieldName")), ColumnMeta.of(a));
        }

        List<ResolvedListView.Column> columns = new ArrayList<>();
        if (spec.explicit()) {
            // Author took explicit control: exactly these fields, in this order.
            for (String field : spec.include()) {
                ColumnMeta cm = available.get(field);
                if (cm == null) {
                    continue;
                }
                columns.add(new ResolvedListView.Column(
                        spec.labels().getOrDefault(field, cm.label()), cm.columnName()));
            }
        } else {
            // Default: visible columns in configured order, minus any hidden, with label overrides.
            available.entrySet().stream()
                    .filter(e -> e.getValue().visibleInList())
                    .filter(e -> !spec.hidden().contains(e.getKey()))
                    .sorted(Comparator.comparingInt(e -> e.getValue().order()))
                    .forEach(e -> columns.add(new ResolvedListView.Column(
                            spec.labels().getOrDefault(e.getKey(), e.getValue().label()),
                            e.getValue().columnName())));
        }

        String title = spec.title() != null ? spec.title() : str(meta.get("name"));
        return new ResolvedListView(title, columns);
    }

    private record ColumnMeta(String label, String columnName, boolean visibleInList, int order) {
        static ColumnMeta of(Map<String, Object> m) {
            Object order = m.get("order");
            return new ColumnMeta(
                    str(m.get("displayName")), str(m.get("columnName")),
                    Boolean.TRUE.equals(m.get("visibleInList")),
                    order == null ? 0 : ((Number) order).intValue());
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
