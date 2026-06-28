package com.emat.reapi.api.dto.statement;


import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementTypeDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StatementDefinitionDto(
        String statementId,

        @NotBlank(message = "profileId is required")
        String profileId,

        @NotBlank(message = "statementKey is required")
        String statementKey,

        @NotEmpty(message = "statementTypeDefinitions must not be empty")
        List<StatementTypeDefinition> statementTypeDefinitions
) {
    public static StatementDefinitionDto toDto(StatementDefinition domain) {
        return new StatementDefinitionDto(
                domain.getStatementId(),
                domain.getProfileId(),
                domain.getStatementKey(),
                domain.getStatementTypeDefinitions()
        );
    }

    public StatementDefinition toDomain() {
        return new StatementDefinition(statementId, profileId, statementKey, statementTypeDefinitions);
    }
}
