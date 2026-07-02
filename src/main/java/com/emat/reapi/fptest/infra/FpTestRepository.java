package com.emat.reapi.fptest.infra;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface FpTestRepository extends ReactiveMongoRepository<FpTestDocument, String> {
    Mono<FpTestDocument> findByTestId(String testId);

    // Batch name-resolution for the admin submissions list — one query, not one per row.
    // Unfiltered by isDeleted on purpose (soft-deleted tests still need their name resolved).
    Flux<FpTestDocument> findByTestIdIn(Collection<String> testIds);

    Flux<FpTestDocument> findAllByIsDeletedFalse();
    Mono<Boolean> existsByFpTestStatementDocumentsStatementKey(String statementKey);
    Flux<FpTestDocument> findAllByFpTestStatementDocumentsStatementKey(String statementKey);
}
