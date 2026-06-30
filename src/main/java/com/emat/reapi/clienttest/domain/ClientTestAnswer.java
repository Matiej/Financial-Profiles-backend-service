package com.emat.reapi.clienttest.domain;

public record ClientTestAnswer(
        String statementKey,
        String profileId,
        String limitingDescription,
        String supportingDescription,
        int scoring
) {
}
