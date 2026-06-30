package com.emat.reapi.api.dto.clienttestdto;

import com.emat.reapi.clienttest.domain.ClientTest;
import com.emat.reapi.clienttest.domain.ClientTestQuestion;

import java.util.List;

public record ClientTestDto(
        String testName,
        String descriptionBefore,
        String descriptionAfter,
        String publicToken,
        String submissionId,
        List<ClientQuestionDto> clientQuestions
) {
    public static ClientTestDto fromDomain(ClientTest clientTest) {
        return new ClientTestDto(
                clientTest.testName(),
                clientTest.descriptionBefore(),
                clientTest.descriptionAfter(),
                clientTest.publicToken(),
                clientTest.submissionId(),
                fromDomainList(clientTest.clientTestQuestions())
        );
    }

    private static List<ClientQuestionDto> fromDomainList(List<ClientTestQuestion> clientTestQuestions) {
        return clientTestQuestions
                .stream()
                .map(question ->
                        new ClientQuestionDto(
                                question.id(),
                                question.statementKey(),
                                question.statementCategory(),
                                question.supportingStatement(),
                                question.limitingStatement()
                        )
                ).toList();
    }
}
