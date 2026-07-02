package com.emat.reapi.fptest.infra;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface FpTestRepository extends ReactiveMongoRepository<FpTestDocument, String> {
    Mono<FpTestDocument> findByTestId(String testId);
    Mono<Void> deleteByTestId(String testId);
    Mono<Boolean> existsByFpTestStatementDocumentsStatementKey(String statementKey);
    Flux<FpTestDocument> findAllByFpTestStatementDocumentsStatementKey(String statementKey);
}
