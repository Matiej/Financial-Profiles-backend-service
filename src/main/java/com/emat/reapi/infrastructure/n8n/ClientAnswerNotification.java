package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.clienttest.domain.ClientTestAnswer;
import com.emat.reapi.statement.domain.StatementCategory;

public record ClientAnswerNotification(
        String questionKey,
        StatementCategory category,
        String limitingDescription,
        String supportingDescription,
        int scoring
) {

    public static ClientAnswerNotification fromDomain(ClientTestAnswer clientTestAnswerDocument) {
        return new ClientAnswerNotification(
                clientTestAnswerDocument.statementKey(),
                clientTestAnswerDocument.category(),
                clientTestAnswerDocument.limitingDescription(),
                clientTestAnswerDocument.supportingDescription(),
                clientTestAnswerDocument.scoring()
        );
    }
}
