package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.clienttest.domain.ClientTestAnswer;

public record ClientAnswerNotification(
        String questionKey,
        String profileId,
        String limitingDescription,
        String supportingDescription,
        int scoring
) {

    public static ClientAnswerNotification fromDomain(ClientTestAnswer clientTestAnswerDocument) {
        return new ClientAnswerNotification(
                clientTestAnswerDocument.statementKey(),
                clientTestAnswerDocument.profileId(),
                clientTestAnswerDocument.limitingDescription(),
                clientTestAnswerDocument.supportingDescription(),
                clientTestAnswerDocument.scoring()
        );
    }
}
