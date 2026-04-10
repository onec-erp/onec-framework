package com.onec.posting;

import com.onec.lifecycle.AfterPostHandler;
import com.onec.lifecycle.BeforePostHandler;
import com.onec.lifecycle.Postable;
import com.onec.metadata.DocumentDescriptor;
import com.onec.metadata.MetadataRegistry;
import com.onec.model.DocumentObject;
import com.onec.repository.RegisterRepositoryImpl;

import org.jdbi.v3.core.Jdbi;

import java.util.Map;

public class PostingEngine {

    private final Jdbi jdbi;
    private final MetadataRegistry registry;
    private final Map<Class<?>, RegisterRepositoryImpl<?>> repositoryMap;

    public PostingEngine(Jdbi jdbi, MetadataRegistry registry,
                         Map<Class<?>, RegisterRepositoryImpl<?>> repositoryMap) {
        this.jdbi = jdbi;
        this.registry = registry;
        this.repositoryMap = repositoryMap;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void post(DocumentObject document) {
        if (!(document instanceof Postable postable)) {
            throw new IllegalArgumentException(
                    document.getClass().getName() + " does not implement Postable");
        }

        if (document instanceof BeforePostHandler handler) {
            handler.beforePost();
        }

        DocumentDescriptor docDescriptor = registry.getDocumentDescriptor(document.getClass());

        PostingContext context = new PostingContext(repositoryMap);
        postable.handlePosting(context);

        jdbi.useTransaction(handle -> {
            for (RegisterRepositoryImpl<?> repo : context.touchedRepositories()) {
                RegisterPersistence persistence = repo.getPersistence();
                persistence.insertRecords(handle, repo.getPendingMovements(),
                        document.getId(), document.getDate());
                persistence.updateTotals(handle, repo.getPendingMovements());
                repo.clearPending();
            }

            handle.createUpdate("UPDATE " + docDescriptor.tableName() +
                            " SET _posted = TRUE WHERE _id = :id")
                    .bind("id", document.getId())
                    .execute();
        });

        document.setPosted(true);

        if (document instanceof AfterPostHandler handler) {
            handler.afterPost();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void unpost(DocumentObject document) {
        DocumentDescriptor docDescriptor = registry.getDocumentDescriptor(document.getClass());

        jdbi.useTransaction(handle -> {
            for (RegisterRepositoryImpl<?> repo : repositoryMap.values()) {
                RegisterPersistence persistence = repo.getPersistence();
                persistence.reverseTotals(handle, document.getId());
                persistence.deactivateRecords(handle, document.getId());
            }

            handle.createUpdate("UPDATE " + docDescriptor.tableName() +
                            " SET _posted = FALSE WHERE _id = :id")
                    .bind("id", document.getId())
                    .execute();
        });

        document.setPosted(false);
    }
}
