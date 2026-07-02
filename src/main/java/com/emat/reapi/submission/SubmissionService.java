package com.emat.reapi.submission;

import com.emat.reapi.api.dto.submission.SubmissionDto;
import com.emat.reapi.api.dto.submission.SubmissionUpdateDto;
import com.emat.reapi.submission.domain.Submission;
import com.emat.reapi.submission.domain.SubmissionStatus;
import com.emat.reapi.submission.domain.SubmissionView;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface SubmissionService {
    // admin-facing methods return SubmissionView (submission + resolved testName);
    // internal flows (client-test serving/closing) use the bare Submission variants
    Mono<SubmissionView> findBySubmissionId(String submissionId);
    Mono<SubmissionView> createSubmission(SubmissionDto request);
    Mono<SubmissionView> updateSubmission(SubmissionUpdateDto request, String submissionId);
    Flux<SubmissionView> findAllByOrderByCreatedAtDesc();
    Mono<Void> deleteSubmission(String submissionId);
    Mono<SubmissionView> closeSubmission(String submissionId);
    Mono<Boolean> existsByTestId(String testId);
    Flux<Submission> findAllByTestId(String testId);
    Mono<Submission> findByPublicTokenAndStatusAndExpireAtAfter(
            String publicToken,
            SubmissionStatus status,
            Instant now
    );
    Mono<Boolean> existsByTestIdAndStatusAndExpireAtAfter(String testId, SubmissionStatus status, Instant now);
}
