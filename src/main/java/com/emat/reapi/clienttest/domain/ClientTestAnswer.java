package com.emat.reapi.clienttest.domain;

import com.emat.reapi.statement.domain.StatementProfile;

public record ClientTestAnswer(
        String statementKey,
        String profileId,
        String limitingDescription,
        String supportingDescription,
        int scoring
) {
    // Transitional bridge to the StatementProfile enum (removed in F6, once scoring/AI/n8n
    // read profileId/snapshot directly). Lets downstream consumers keep using category().
    public StatementProfile category() {
        return StatementProfile.fromProfileId(profileId);
    }
}
