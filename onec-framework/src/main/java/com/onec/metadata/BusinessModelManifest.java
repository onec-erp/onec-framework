package com.onec.metadata;

import com.onec.lifecycle.AfterPostHandler;
import com.onec.lifecycle.AfterWriteHandler;
import com.onec.lifecycle.BeforeDeleteHandler;
import com.onec.lifecycle.BeforePostHandler;
import com.onec.lifecycle.BeforeWriteHandler;
import com.onec.lifecycle.OnFillingHandler;
import com.onec.lifecycle.Postable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record BusinessModelManifest(
        String schemaVersion,
        List<Catalog> catalogs,
        List<Document> documents,
        List<AccumulationRegister> accumulationRegisters,
        List<InformationRegister> informationRegisters,
        List<Enumeration> enumerations,
        List<Constant> constants,
        List<DashboardWidget> dashboardWidgets
) {

    public record Catalog(
            String id,
            String name,
            String javaClass,
            String tableName,
            String context,
            int codeLength,
            boolean hierarchical,
            boolean autoNumber,
            String codePrefix,
            List<String> readRoles,
            List<String> writeRoles,
            List<Field> fields,
            List<BusinessRule> rules,
            List<DomainEvent> events,
            List<Capability> capabilities
    ) {
    }

    public record Document(
            String id,
            String name,
            String javaClass,
            String tableName,
            String context,
            int numberLength,
            boolean autoNumber,
            String numberPrefix,
            List<String> readRoles,
            List<String> writeRoles,
            List<Field> fields,
            List<TabularSection> tabularSections,
            List<BusinessRule> rules,
            List<PostingRule> postingRules,
            List<DomainEvent> events,
            List<Capability> capabilities
    ) {
    }

    public record AccumulationRegister(
            String id,
            String name,
            String javaClass,
            String tableName,
            String totalsTableName,
            String context,
            String accumulationType,
            List<String> readRoles,
            List<String> writeRoles,
            List<Field> dimensions,
            List<Field> resources,
            List<Capability> capabilities
    ) {
    }

    public record InformationRegister(
            String id,
            String name,
            String javaClass,
            String tableName,
            String context,
            String periodicity,
            List<String> readRoles,
            List<String> writeRoles,
            List<Field> dimensions,
            List<Field> resources,
            List<Field> attributes,
            List<Capability> capabilities
    ) {
    }

    public record Enumeration(
            String id,
            String name,
            String javaClass,
            String tableName,
            List<EnumerationValue> values
    ) {
    }

    public record Constant(
            String id,
            String name,
            String javaClass,
            Field value
    ) {
    }

    public record TabularSection(
            String id,
            String name,
            String fieldName,
            String javaClass,
            String tableName,
            List<Field> fields
    ) {
    }

    public record Field(
            String name,
            String displayName,
            String columnName,
            String javaType,
            String semanticType,
            boolean required,
            Reference reference,
            NumberFormat numberFormat,
            Ui ui
    ) {
    }

    public record Reference(
            String target
    ) {
    }

    public record NumberFormat(
            int length,
            int precision,
            int scale
    ) {
    }

    public record Ui(
            boolean visibleInList,
            boolean visibleInForm,
            boolean visibleInDetail,
            int order,
            String group,
            String widthHint,
            String widget
    ) {
    }

    public record EnumerationValue(
            String name,
            String id,
            int order
    ) {
    }

    public record DashboardWidget(
            String title,
            String widgetType,
            int order,
            String width,
            String entityType,
            String entityName,
            int maxItems,
            String dateField,
            String titleField,
            Map<String, String> extraConfig
    ) {
    }

    public record Capability(
            String name,
            String description
    ) {
    }

    public record BusinessRule(
            String name,
            String expression,
            String message
    ) {
    }

    public record PostingRule(
            String register,
            String movement,
            String forEach,
            Map<String, String> mappings
    ) {
    }

    public record DomainEvent(
            String name,
            String timing
    ) {
    }

    static List<Capability> capabilitiesFor(Class<?> javaClass) {
        return Stream.of(
                        capability(javaClass, BeforeWriteHandler.class,
                                "beforeWrite", "Mutates or validates data immediately before persistence."),
                        capability(javaClass, AfterWriteHandler.class,
                                "afterWrite", "Runs behavior after persistence succeeds."),
                        capability(javaClass, BeforeDeleteHandler.class,
                                "beforeDelete", "Runs behavior before deletion."),
                        capability(javaClass, OnFillingHandler.class,
                                "onFilling", "Initializes default values before editing or persistence."),
                        capability(javaClass, BeforePostHandler.class,
                                "beforePost", "Runs checks or calculations before document posting."),
                        capability(javaClass, Postable.class,
                                "posting", "Produces register movements when the document is posted."),
                        capability(javaClass, AfterPostHandler.class,
                                "afterPost", "Runs behavior after document posting succeeds."))
                .filter(c -> c != null)
                .toList();
    }

    private static Capability capability(Class<?> javaClass, Class<?> marker,
                                         String name, String description) {
        return marker.isAssignableFrom(javaClass) ? new Capability(name, description) : null;
    }
}
