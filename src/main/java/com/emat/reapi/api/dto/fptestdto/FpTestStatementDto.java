package com.emat.reapi.api.dto.fptestdto;

import com.emat.reapi.fptest.domain.FpTestStatement;

public record FpTestStatementDto(
        String statementKey,
        String statementsDescription,
        String statementsProfile
) {

    public static FpTestStatementDto fromDomain(FpTestStatement fpTestStatement) {
        return new FpTestStatementDto(
                fpTestStatement.statementKey(),
                fpTestStatement.statementsDescription(),
                fpTestStatement.statementsProfile()
        );
    }
}
