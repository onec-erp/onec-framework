package com.onec.metadata;

import com.onec.fixtures.TestCompanyName;
import com.onec.fixtures.TestInvoice;
import com.onec.fixtures.TestOrderStatus;
import com.onec.fixtures.TestPriceRegister;
import com.onec.fixtures.TestProduct;
import com.onec.fixtures.TestSalesRegister;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessModelManifestBuilderTest {

    private MetadataRegistry registry;
    private MetadataScanner scanner;

    @BeforeEach
    void setUp() {
        registry = new MetadataRegistry();
        scanner = new MetadataScanner(new DefaultNamingStrategy());
    }

    @Test
    void build_includesAgentReadableEntityGroups() {
        registry.registerCatalog(scanner.scan(TestProduct.class));
        registry.registerDocument(scanner.scanDocument(TestInvoice.class));
        registry.registerAccumulation(scanner.scanRegister(TestSalesRegister.class));
        registry.registerInformationRegister(scanner.scanInformationRegister(TestPriceRegister.class));
        registry.registerEnumeration(scanner.scanEnumeration(TestOrderStatus.class));
        registry.registerConstant(scanner.scanConstant(TestCompanyName.class));

        BusinessModelManifest manifest = new BusinessModelManifestBuilder(registry).build();

        assertThat(manifest.schemaVersion()).isEqualTo(BusinessModelManifestBuilder.SCHEMA_VERSION);
        assertThat(manifest.catalogs()).extracting(BusinessModelManifest.Catalog::id)
                .containsExactly("catalog:TestProducts");
        assertThat(manifest.documents()).extracting(BusinessModelManifest.Document::id)
                .containsExactly("document:TestInvoices");
        assertThat(manifest.accumulationRegisters())
                .extracting(BusinessModelManifest.AccumulationRegister::id)
                .containsExactly("accumulationRegister:TestSales");
        assertThat(manifest.informationRegisters())
                .extracting(BusinessModelManifest.InformationRegister::id)
                .containsExactly("informationRegister:Prices");
        assertThat(manifest.enumerations()).extracting(BusinessModelManifest.Enumeration::id)
                .containsExactly("enumeration:OrderStatuses");
        assertThat(manifest.constants()).extracting(BusinessModelManifest.Constant::id)
                .containsExactly("constant:CompanyName");
    }

    @Test
    void build_documentsIncludeFieldsTabularSectionsAndLifecycleCapabilities() {
        registry.registerDocument(scanner.scanDocument(TestInvoice.class));

        BusinessModelManifest.Document document = new BusinessModelManifestBuilder(registry)
                .build()
                .documents()
                .get(0);

        assertThat(document.name()).isEqualTo("TestInvoices");
        assertThat(document.tableName()).isEqualTo("document_test_invoices");
        assertThat(document.capabilities()).extracting(BusinessModelManifest.Capability::name)
                .containsExactlyInAnyOrder("beforeWrite", "afterWrite");
        assertThat(document.fields()).extracting(BusinessModelManifest.Field::name)
                .containsExactly("counterparty");
        assertThat(document.fields().get(0).semanticType()).isEqualTo("text");

        BusinessModelManifest.TabularSection items = document.tabularSections().get(0);
        assertThat(items.id()).isEqualTo("document:TestInvoices.tabularSection:items");
        assertThat(items.fields()).extracting(BusinessModelManifest.Field::name)
                .containsExactlyInAnyOrder("productName", "quantity", "price");
        assertThat(items.fields()).filteredOn(f -> f.name().equals("quantity"))
                .extracting(BusinessModelManifest.Field::semanticType)
                .containsExactly("quantity");
    }

    @Test
    void build_enumerationsIncludeStableValues() {
        registry.registerEnumeration(scanner.scanEnumeration(TestOrderStatus.class));

        BusinessModelManifest.Enumeration enumeration = new BusinessModelManifestBuilder(registry)
                .build()
                .enumerations()
                .get(0);

        assertThat(enumeration.values()).extracting(BusinessModelManifest.EnumerationValue::name)
                .containsExactly("NEW", "IN_PROGRESS", "COMPLETED");
        assertThat(enumeration.values()).extracting(BusinessModelManifest.EnumerationValue::order)
                .containsExactly(0, 1, 2);
    }
}
