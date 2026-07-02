package com.emat.reapi.submission.domain;

/**
 * Admin-facing read model: a submission together with the resolved name of its test.
 * The name is looked up from FpTest by the service (also for soft-deleted tests, falling
 * back to the raw testId) — it is NOT persisted on the submission document, and the
 * {@link Submission} entity stays a pure mirror of persistence. Internal flows
 * (token-based client-test lookups) keep using {@link Submission} directly.
 */
public record SubmissionView(
        Submission submission,
        String testName
) {
}
