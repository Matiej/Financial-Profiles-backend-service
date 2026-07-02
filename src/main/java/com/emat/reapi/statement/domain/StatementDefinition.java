package com.emat.reapi.statement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data
public class StatementDefinition {
    private String id;
    private String profileId;
    // Stable reference key (FpTest refers to definitions by it) — immutable after creation.
    // Seeded definitions keep deterministic p{n}_q{n} keys; user-created ones get "sk_" + UUID.
    private String statementKey;
    private List<StatementTypeDefinition> statementTypeDefinitions;
    private boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;
}
