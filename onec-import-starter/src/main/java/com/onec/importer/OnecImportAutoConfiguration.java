package com.onec.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onec.metadata.MetadataRegistry;
import com.onec.ui.CatalogCommandService;
import com.onec.ui.CatalogQueryService;
import com.onec.ui.DocumentCommandService;
import com.onec.ui.DocumentQueryService;
import com.onec.ui.UiAccessService;
import com.onec.ui.UiAutoConfiguration;

import org.jdbi.v3.core.Jdbi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = UiAutoConfiguration.class)
@ConditionalOnBean({
        MetadataRegistry.class,
        CatalogQueryService.class,
        CatalogCommandService.class,
        DocumentQueryService.class,
        DocumentCommandService.class,
        UiAccessService.class
})
@EnableConfigurationProperties(OnecImportProperties.class)
@ConditionalOnProperty(prefix = "onec.import", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OnecImportAutoConfiguration {

    @Bean
    public CatalogCsvImportService catalogCsvImportService(Jdbi jdbi,
                                                           CatalogCommandService catalogCommandService,
                                                           OnecImportProperties properties) {
        return new CatalogCsvImportService(jdbi, catalogCommandService, properties);
    }

    @Bean
    public DocumentCsvImportService documentCsvImportService(Jdbi jdbi,
                                                             DocumentCommandService documentCommandService,
                                                             OnecImportProperties properties) {
        return new DocumentCsvImportService(jdbi, documentCommandService, properties);
    }

    @Bean
    public CatalogImportController catalogImportController(CatalogQueryService catalogQueryService,
                                                           DocumentQueryService documentQueryService,
                                                           UiAccessService access,
                                                           CatalogCsvImportService catalogImports,
                                                           DocumentCsvImportService documentImports,
                                                           OnecImportProperties properties,
                                                           ObjectMapper objectMapper) {
        return new CatalogImportController(catalogQueryService, documentQueryService, access,
                catalogImports, documentImports, properties, objectMapper);
    }
}
