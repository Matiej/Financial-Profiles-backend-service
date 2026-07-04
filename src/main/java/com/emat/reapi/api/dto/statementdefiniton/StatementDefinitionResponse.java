package com.emat.reapi.api.dto.statementdefiniton;

import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementTypeDefinition;

import java.time.Instant;
import java.util.List;

/**
 * Full statement-definition view returned to the admin panel.
 */
public record StatementDefinitionResponse(
        String id,
        String profileId,
        String statementKey,
        List<StatementTypeDefinition> statementTypeDefinitions,
        Instant createdAt,
        Instant updatedAt
) {
    public static StatementDefinitionResponse toResponse(StatementDefinition domain) {
        return new StatementDefinitionResponse(
                domain.getId(),
                domain.getProfileId(),
                domain.getStatementKey(),
                domain.getStatementTypeDefinitions(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
