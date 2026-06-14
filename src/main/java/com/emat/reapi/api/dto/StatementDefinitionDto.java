package com.emat.reapi.api.dto;


import com.emat.reapi.statement.domain.StatementProfile;
import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementTypeDefinition;

import java.util.List;

public record StatementDefinitionDto(
        String statementId,
        StatementProfile category,
        String statementKey,
        List<StatementTypeDefinition> statementTypeDefinitions
) {
    public static StatementDefinitionDto toDto(StatementDefinition domain) {
        return new StatementDefinitionDto(
                domain.getStatementId(),
                domain.getCategory(),
                domain.getStatementKey(),
                domain.getStatementTypeDefinitions()
        );
    }

    public StatementDefinition toDomain() {
        return new StatementDefinition(statementId, category, statementKey, statementTypeDefinitions);
    }
}
