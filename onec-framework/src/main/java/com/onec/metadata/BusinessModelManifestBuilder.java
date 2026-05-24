package com.onec.metadata;

import com.onec.annotations.BusinessRule;
import com.onec.annotations.DomainEvent;
import com.onec.annotations.PostingRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BusinessModelManifestBuilder {

    public static final String SCHEMA_VERSION = "onec.business-model.v1";

    private final MetadataRegistry registry;

    public BusinessModelManifestBuilder(MetadataRegistry registry) {
        this.registry = registry;
    }

    /**
     * Build the manifest using {@code @DashboardWidget} annotations as the widget source.
     *
     * <p>Retained for backwards compatibility. New callers should prefer
     * {@link #build(List)} and pass widgets resolved from {@code UiLayoutBuilder}
     * so that the manifest stays consistent with {@code /api/ui/metadata/dashboard}.</p>
     */
    public BusinessModelManifest build() {
        return build(registry.allDashboardWidgets());
    }

    /**
     * Build the manifest with an explicit widget source. Use this overload to
     * pass widgets resolved from the configurer (see {@code UiLayoutResolver
     * .resolveWidgets}) instead of scanning {@code @DashboardWidget}.
     */
    public BusinessModelManifest build(List<DashboardWidgetDescriptor> widgets) {
        return new BusinessModelManifest(
                SCHEMA_VERSION,
                registry.allCatalogs().stream()
                        .sorted(Comparator.comparing(CatalogDescriptor::logicalName))
                        .map(this::catalog)
                        .toList(),
                registry.allDocuments().stream()
                        .sorted(Comparator.comparing(DocumentDescriptor::logicalName))
                        .map(this::document)
                        .toList(),
                registry.allRegisters().stream()
                        .sorted(Comparator.comparing(AccumulationRegisterDescriptor::logicalName))
                        .map(this::accumulationRegister)
                        .toList(),
                registry.allInformationRegisters().stream()
                        .sorted(Comparator.comparing(InformationRegisterDescriptor::logicalName))
                        .map(this::informationRegister)
                        .toList(),
                registry.allEnumerations().stream()
                        .sorted(Comparator.comparing(EnumerationDescriptor::logicalName))
                        .map(this::enumeration)
                        .toList(),
                registry.allConstants().stream()
                        .sorted(Comparator.comparing(ConstantDescriptor::logicalName))
                        .map(this::constant)
                        .toList(),
                widgets.stream()
                        .sorted(Comparator.comparingInt(DashboardWidgetDescriptor::order)
                                .thenComparing(DashboardWidgetDescriptor::title))
                        .map(this::dashboardWidget)
                        .toList());
    }

    private BusinessModelManifest.Catalog catalog(CatalogDescriptor descriptor) {
        return new BusinessModelManifest.Catalog(
                id("catalog", descriptor.logicalName()),
                descriptor.logicalName(),
                descriptor.javaClass().getName(),
                descriptor.tableName(),
                descriptor.context(),
                descriptor.codeLength(),
                descriptor.hierarchical(),
                descriptor.autoNumber(),
                descriptor.codePrefix(),
                descriptor.readRoles(),
                descriptor.writeRoles(),
                descriptor.attributes().stream().map(this::field).toList(),
                businessRules(descriptor.javaClass()),
                domainEvents(descriptor.javaClass()),
                BusinessModelManifest.capabilitiesFor(descriptor.javaClass()));
    }

    private BusinessModelManifest.Document document(DocumentDescriptor descriptor) {
        return new BusinessModelManifest.Document(
                id("document", descriptor.logicalName()),
                descriptor.logicalName(),
                descriptor.javaClass().getName(),
                descriptor.tableName(),
                descriptor.context(),
                descriptor.numberLength(),
                descriptor.autoNumber(),
                descriptor.numberPrefix(),
                descriptor.readRoles(),
                descriptor.writeRoles(),
                descriptor.attributes().stream().map(this::field).toList(),
                descriptor.tabularSections().stream()
                        .sorted(Comparator.comparing(TabularSectionDescriptor::name))
                        .map(ts -> tabularSection(descriptor, ts))
                        .toList(),
                businessRules(descriptor.javaClass()),
                postingRules(descriptor.javaClass()),
                domainEvents(descriptor.javaClass()),
                BusinessModelManifest.capabilitiesFor(descriptor.javaClass()));
    }

    private BusinessModelManifest.AccumulationRegister accumulationRegister(
            AccumulationRegisterDescriptor descriptor) {
        return new BusinessModelManifest.AccumulationRegister(
                id("accumulationRegister", descriptor.logicalName()),
                descriptor.logicalName(),
                descriptor.javaClass().getName(),
                descriptor.tableName(),
                descriptor.totalsTableName(),
                descriptor.context(),
                descriptor.accumulationType().name(),
                descriptor.readRoles(),
                descriptor.writeRoles(),
                descriptor.dimensions().stream().map(this::field).toList(),
                descriptor.resources().stream().map(this::field).toList(),
                BusinessModelManifest.capabilitiesFor(descriptor.javaClass()));
    }

    private BusinessModelManifest.InformationRegister informationRegister(
            InformationRegisterDescriptor descriptor) {
        return new BusinessModelManifest.InformationRegister(
                id("informationRegister", descriptor.logicalName()),
                descriptor.logicalName(),
                descriptor.javaClass().getName(),
                descriptor.tableName(),
                descriptor.context(),
                descriptor.periodicity().name(),
                descriptor.readRoles(),
                descriptor.writeRoles(),
                descriptor.dimensions().stream().map(this::field).toList(),
                descriptor.resources().stream().map(this::field).toList(),
                descriptor.attributes().stream().map(this::field).toList(),
                BusinessModelManifest.capabilitiesFor(descriptor.javaClass()));
    }

    private BusinessModelManifest.Enumeration enumeration(EnumerationDescriptor descriptor) {
        return new BusinessModelManifest.Enumeration(
                id("enumeration", descriptor.logicalName()),
                descriptor.logicalName(),
                descriptor.javaClass().getName(),
                descriptor.tableName(),
                descriptor.values().stream()
                        .map(v -> new BusinessModelManifest.EnumerationValue(
                                v.name(), v.id().toString(), v.order()))
                        .toList());
    }

    private BusinessModelManifest.Constant constant(ConstantDescriptor descriptor) {
        return new BusinessModelManifest.Constant(
                id("constant", descriptor.logicalName()),
                descriptor.logicalName(),
                descriptor.javaClass().getName(),
                scalarField(descriptor.fieldName(), descriptor.logicalName(), descriptor.valueType()));
    }

    private BusinessModelManifest.TabularSection tabularSection(
            DocumentDescriptor document, TabularSectionDescriptor descriptor) {
        return new BusinessModelManifest.TabularSection(
                id("document", document.logicalName()) + ".tabularSection:" + descriptor.name(),
                descriptor.name(),
                descriptor.fieldName(),
                descriptor.rowClass().getName(),
                descriptor.tableName(),
                descriptor.attributes().stream().map(this::field).toList());
    }

    private BusinessModelManifest.Field field(AttributeDescriptor descriptor) {
        return new BusinessModelManifest.Field(
                descriptor.fieldName(),
                descriptor.displayName(),
                descriptor.columnName(),
                descriptor.javaType().getName(),
                semanticType(descriptor),
                descriptor.required(),
                descriptor.isRef() ? new BusinessModelManifest.Reference(descriptor.refTarget()) : null,
                new BusinessModelManifest.NumberFormat(
                        descriptor.length(), descriptor.precision(), descriptor.scale()),
                new BusinessModelManifest.Ui(
                        descriptor.visibleInList(),
                        descriptor.visibleInForm(),
                        descriptor.visibleInDetail(),
                        descriptor.order(),
                        descriptor.group(),
                        descriptor.widthHint(),
                        descriptor.widget()));
    }

    private BusinessModelManifest.Field scalarField(String fieldName, String displayName, Class<?> javaType) {
        return new BusinessModelManifest.Field(
                fieldName,
                displayName,
                fieldName,
                javaType.getName(),
                semanticType(javaType, false),
                false,
                null,
                new BusinessModelManifest.NumberFormat(255, 15, 2),
                new BusinessModelManifest.Ui(true, true, true, 0, "", "", ""));
    }

    private BusinessModelManifest.DashboardWidget dashboardWidget(DashboardWidgetDescriptor descriptor) {
        return new BusinessModelManifest.DashboardWidget(
                descriptor.title(),
                descriptor.widgetType(),
                descriptor.order(),
                descriptor.width(),
                descriptor.entityType(),
                descriptor.entityName(),
                descriptor.maxItems(),
                descriptor.dateField(),
                descriptor.titleField(),
                descriptor.extraConfig());
    }

    private java.util.List<BusinessModelManifest.BusinessRule> businessRules(Class<?> javaClass) {
        return java.util.Arrays.stream(javaClass.getAnnotationsByType(BusinessRule.class))
                .map(rule -> new BusinessModelManifest.BusinessRule(
                        rule.name(), rule.expression(), rule.message()))
                .toList();
    }

    private java.util.List<BusinessModelManifest.PostingRule> postingRules(Class<?> javaClass) {
        return java.util.Arrays.stream(javaClass.getAnnotationsByType(PostingRule.class))
                .map(rule -> new BusinessModelManifest.PostingRule(
                        rule.register().getName(),
                        rule.movement().name(),
                        rule.forEach(),
                        mappings(rule.map())))
                .toList();
    }

    private java.util.List<BusinessModelManifest.DomainEvent> domainEvents(Class<?> javaClass) {
        return java.util.Arrays.stream(javaClass.getAnnotationsByType(DomainEvent.class))
                .map(event -> new BusinessModelManifest.DomainEvent(
                        event.name(), event.when().name()))
                .toList();
    }

    private Map<String, String> mappings(String[] values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String mapping : values) {
            int eq = mapping.indexOf('=');
            if (eq > 0) {
                result.put(mapping.substring(0, eq).trim(), mapping.substring(eq + 1).trim());
            }
        }
        return result;
    }

    private String semanticType(AttributeDescriptor descriptor) {
        return semanticType(descriptor.javaType(), descriptor.isRef());
    }

    private String semanticType(Class<?> javaType, boolean isRef) {
        if (isRef) return "reference";
        if (javaType.isEnum()) return "enumeration";
        if (javaType == BigDecimal.class) return "quantity";
        if (javaType == String.class) return "text";
        if (javaType == UUID.class) return "identifier";
        if (javaType == LocalDate.class) return "date";
        if (javaType == LocalDateTime.class) return "datetime";
        if (javaType == boolean.class || javaType == Boolean.class) return "boolean";
        if (Number.class.isAssignableFrom(javaType)
                || javaType == int.class
                || javaType == long.class
                || javaType == double.class
                || javaType == float.class) {
            return "number";
        }
        return "object";
    }

    private String id(String kind, String name) {
        return kind + ":" + name.replaceAll("\\s+", "");
    }
}
