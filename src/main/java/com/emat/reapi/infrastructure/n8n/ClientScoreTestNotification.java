package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.profiler.domain.ScoringProfiledClientDetails;

import java.time.Instant;
import java.util.List;

/**
 * v2 payload for the n8n email webhook ({@code POST /score-test/email}): carries the already-computed
 * scoring (same shape as {@code GET /api/profiler/{tspi}/scoring}) plus the mail recipient and dates.
 * Own DTO on purpose — the outbound contract stays decoupled from the internal scoring DTO.
 */
public record ClientScoreTestNotification(
        String testSubmissionPublicId,
        String clientName,
        String clientId,
        String clientEmail,
        String submissionId,
        Instant submissionDate,
        Instant clientTestDate,
        String testId,
        String testName,
        OverallSummaryNotification overallSummary,
        List<ProfileBlockNotification> profiles
) {
    public static ClientScoreTestNotification of(
            ScoringProfiledClientDetails details,
            String clientEmail,
            Instant clientTestDate
    ) {
        return new ClientScoreTestNotification(
                details.getTestSubmissionPublicId(),
                details.getClientName(),
                details.getClientId(),
                clientEmail,
                details.getSubmissionId(),
                details.getSubmissionDate(),
                clientTestDate,
                details.getTestId(),
                details.getTestName(),
                OverallSummaryNotification.of(details.getOverallSummary()),
                details.getProfiles().stream().map(ProfileBlockNotification::of).toList()
        );
    }
}
