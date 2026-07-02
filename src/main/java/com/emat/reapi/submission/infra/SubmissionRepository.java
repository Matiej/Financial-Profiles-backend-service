package com.emat.reapi.submission.infra;

import com.emat.reapi.submission.domain.SubmissionStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface SubmissionRepository extends ReactiveMongoRepository<SubmissionDocument, String> {
    Mono<SubmissionDocument> findBySubmissionId(String submissionId);
    Mono<Void> deleteBySubmissionId(String submissionId);
    Flux<SubmissionDocument> findAllByOrderByCreatedAtDesc();
    Mono<Boolean> existsByTestId(String testId);
    Flux<SubmissionDocument> findAllByTestId(String testId);
    Mono<SubmissionDocument> findByPublicTokenAndStatus(String publicToken, SubmissionStatus status);

    Mono<SubmissionDocument> findByPublicTokenAndStatusAndExpireAtAfter(String publicToken, SubmissionStatus status, Instant now);

    // FpTest soft-delete guard: is anyone still able to take the test (live token)?
    Mono<Boolean> existsByTestIdAndStatusAndExpireAtAfter(String testId, SubmissionStatus status, Instant now);
}
