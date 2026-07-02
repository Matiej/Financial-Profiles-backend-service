package com.emat.reapi.statement.infra;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface StatementDefinitionRepository extends ReactiveMongoRepository<StatementDefinitionDocument, String> {
    Flux<StatementDefinitionDocument> findAllByIsDeletedFalseOrderByCreatedAtAsc();
    Flux<StatementDefinitionDocument> findAllByProfileIdAndIsDeletedFalseOrderByCreatedAtAsc(String profileId);
    Mono<Boolean> existsByProfileIdAndIsDeletedFalse(String profileId);
}
