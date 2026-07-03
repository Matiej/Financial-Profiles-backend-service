package com.emat.reapi.fptest.infra;

import com.emat.reapi.fptest.domain.FpTestStatement;

import java.util.List;

public record FpTestStatementDocument(
        String statementKey,
        String statementsDescription,
        String statementsProfile
) {

    public static List<FpTestStatementDocument> fromDomainlist(List<FpTestStatement> fpTestStatementList) {
        return fpTestStatementList
                .stream()
                .map(fpTestStatement -> new FpTestStatementDocument(
                                fpTestStatement.statementKey(),
                                fpTestStatement.statementsDescription(),
                                fpTestStatement.statementsProfile()
                        )
                ).toList();
    }
}
