package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.clienttest.domain.ClientTestSubmission;

import java.time.Instant;
import java.util.List;

public record ClientScoreTestNotification(
        String clientName,
        String clientId,
        String clientEmail,
        Instant clientAnswerDate,
        String testName,
        String testId,
        String submissionId,
        Instant submissionDate,
        List<ClientAnswerNotification> clientTestAnswerNotificationList

) {
    public static ClientScoreTestNotification fromDomain(ClientTestSubmission submission) {
        List<ClientAnswerNotification> clientTestAnswerNotifications = submission.getClientTestAnswerList()
                .stream()
                .map(ClientAnswerNotification::fromDomain)
                .toList();
        return new ClientScoreTestNotification(
                submission.getClientName(),
                submission.getClientId(),
                submission.getClientEmail(),
                submission.getCreatedAt(),
                submission.getTestName(),
                submission.getTestId(),
                submission.getSubmissionId(),
                submission.getSubmissionDate(),
                clientTestAnswerNotifications
        );
    }
}
