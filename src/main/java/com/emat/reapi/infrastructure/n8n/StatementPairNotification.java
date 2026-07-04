package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.profiler.domain.ScoringStatementPair;

public record StatementPairNotification(
        String statementKey,
        int scoring,
        String limitingDescription,
        String supportingDescription
) {
    public static StatementPairNotification of(ScoringStatementPair pair) {
        return new StatementPairNotification(
                pair.getStatementKey(),
                pair.getScoring(),
                pair.getLimitingDescription(),
                pair.getSupportingDescription()
        );
    }
}
