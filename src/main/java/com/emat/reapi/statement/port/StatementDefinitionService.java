package com.emat.reapi.statement.port;

import com.emat.reapi.statement.domain.StatementProfile;
import com.emat.reapi.statement.domain.StatementDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StatementDefinitionService {

    Mono<StatementDefinition> saveStatementDefinition(StatementDefinition statementDefinition);
    Flux<StatementDefinition > getAllStatementDefinitions();
    Flux<StatementDefinition> getStatementDefinitionsByCategory(StatementProfile category);
}
