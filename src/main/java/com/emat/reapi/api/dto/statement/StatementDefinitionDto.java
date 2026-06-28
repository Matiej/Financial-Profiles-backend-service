package com.emat.reapi.api.dto;


import com.emat.reapi.statement.domain.StatementProfile;
import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementTypeDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StatementDefinitionDto(
        String statementId,

        @NotNull(message = "category is required")
        StatementProfile category,

        @NotBlank(message = "statementKey is required")
        String statementKey,

        @NotEmpty(message = "statementTypeDefinitions must not be empty")
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
