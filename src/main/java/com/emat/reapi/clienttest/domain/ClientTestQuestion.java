package com.emat.reapi.clienttest.domain;

import com.emat.reapi.statement.domain.StatementProfile;

public record ClientTestQuestion(
        String id,
        String statementKey,
        StatementProfile statementCategory,
        String supportingStatement,
        String limitingStatement
        ) {
}
