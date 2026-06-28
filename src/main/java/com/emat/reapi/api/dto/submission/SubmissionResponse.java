package com.emat.reapi.api.dto;

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
        SubmissionStatus status,
        long remainingSeconds,
        String publicToken,
        Instant createdAt
) {
    public static SubmissionResponse fromDomain(Submission domain) {
        long secs = Duration.between(Instant.now(), domain.expireAt()).getSeconds();
        var remaining = domain.status() == SubmissionStatus.OPEN ? Math.max(0, secs): 0;
        return new SubmissionResponse(
                domain.submissionId(),
                domain.clientId(),
                domain.clientName(),
                domain.clientEmail(),
                domain.orderId(),
                domain.testId(),
                domain.status(),
                remaining,
                domain.publicToken(),
                domain.createdAt()
        );
    }
}

