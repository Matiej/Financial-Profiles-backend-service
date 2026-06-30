package com.emat.reapi.clienttest.domain;

public record ClientTestQuestion(
        String id,
        String statementKey,
        String statementCategory,
        String supportingStatement,
        String limitingStatement
        ) {
}
