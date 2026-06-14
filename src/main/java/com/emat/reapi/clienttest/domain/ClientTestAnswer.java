package com.emat.reapi.clienttest.domain;

import com.emat.reapi.statement.domain.StatementProfile;

public record ClientTestAnswer(
        String statementKey,
        StatementProfile category,
        String limitingDescription,
        String supportingDescription,
        int scoring
) {
}
