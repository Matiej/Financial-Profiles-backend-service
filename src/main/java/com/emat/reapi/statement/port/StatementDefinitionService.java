package com.emat.reapi.statement.port;

import com.emat.reapi.statement.domain.StatementDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StatementDefinitionService {

    Mono<StatementDefinition> createStatementDefinition(StatementDefinition statementDefinition);
    Mono<StatementDefinition> updateStatementDefinition(String id, StatementDefinition updatedDefinition);
    Mono<Void> softDeleteStatementDefinition(String id);
    Flux<StatementDefinition> getActiveStatementDefinitions();
    Flux<StatementDefinition> getStatementDefinitionsByProfileId(String profileId);
}
