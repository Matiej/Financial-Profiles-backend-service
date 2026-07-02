package com.emat.reapi.api.dto.submission;

import com.emat.reapi.submission.domain.Submission;
import com.emat.reapi.submission.domain.SubmissionStatus;

import java.time.Duration;
import java.time.Instant;

public record SubmissionResponse(
        String submissionId,
        String clientId,
        String clientName,
        String clientEmail,
        String orderId,
        String testId,
        // resolved server-side from FpTest (also for soft-deleted tests), so the FE
        // submissions view is self-contained; falls back to raw testId when unresolvable
        String testName,
        SubmissionStatus status,
        long remainingSeconds,
        String publicToken,
        Instant createdAt
) {
    public static SubmissionResponse fromDomain(Submission domain, String testName) {
        long secs = Duration.between(Instant.now(), domain.expireAt()).getSeconds();
        var remaining = domain.status() == SubmissionStatus.OPEN ? Math.max(0, secs): 0;
        return new SubmissionResponse(
                domain.submissionId(),
                domain.clientId(),
                domain.clientName(),
                domain.clientEmail(),
                domain.orderId(),
                domain.testId(),
                testName,
                domain.status(),
                remaining,
                domain.publicToken(),
                domain.createdAt()
        );
    }
}
