package com.emat.reapi.api.dto.statementdefiniton;

import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementTypeDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Create/update payload for a statement definition. The client sends only the editable
 * fields: the profile assignment and the statement texts. Server owns {@code id}
 * (Mongo-generated), {@code statementKey} ("sk_" + UUID, immutable), {@code isDeleted}
 * and timestamps.
 */
public record StatementDefinitionRequest(
        @NotBlank(message = "profileId is required")
        String profileId,

        @NotEmpty(message = "statementTypeDefinitions must not be empty")
        List<StatementTypeDefinition> statementTypeDefinitions
) {
    /**
     * Maps the request to a domain definition carrying only the editable fields.
     * Identity, key, deletion flag and timestamps are assigned by the service.
     */
    public StatementDefinition toDomain() {
        return StatementDefinition.builder()
                .profileId(profileId)
                .statementTypeDefinitions(statementTypeDefinitions)
                .build();
    }
}
