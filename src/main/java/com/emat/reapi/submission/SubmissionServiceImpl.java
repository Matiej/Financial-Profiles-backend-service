package com.emat.reapi.submission;

import com.emat.reapi.api.dto.submission.SubmissionDto;
import com.emat.reapi.api.dto.submission.SubmissionUpdateDto;
import com.emat.reapi.fptest.infra.FpTestDocument;
import com.emat.reapi.fptest.infra.FpTestRepository;
import com.emat.reapi.submission.domain.Submission;
import com.emat.reapi.submission.domain.SubmissionStatus;
import com.emat.reapi.submission.domain.SubmissionView;
import com.emat.reapi.submission.infra.SubmissionDocument;
import com.emat.reapi.submission.infra.SubmissionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final FpTestRepository fpTestRepository;

    @Override
    public Mono<SubmissionView> findBySubmissionId(String submissionId) {
        return submissionRepository.findBySubmissionId(submissionId)
                .switchIfEmpty(Mono.error(new SubmissionException(
                                "Submission not found: " + submissionId,
                                SubmissionException.SubmissionErrorType.SUBMISSION_NOT_FOUND)
                        )
                )
                .map(SubmissionDocument::toDomain)
                .flatMap(this::withResolvedTestName);
    }

    @Override
    public Mono<SubmissionView> createSubmission(SubmissionDto request) {
        log.info("Creating submission for clientId: {}", request.clientId());
        var document = new SubmissionDocument();
        document.setSubmissionId("sub_" + UUID.randomUUID());
        document.setClientId(request.clientId());
        document.setClientName(request.clientName());
        document.setClientEmail(request.clientEmail());
        document.setOrderId(request.orderId() + "_" + UUID.randomUUID());
        document.setTestId(request.testId());
        document.setStatus(SubmissionStatus.OPEN);
        document.setDurationDays(request.durationDays());
        document.setPublicToken("pt_" + UUID.randomUUID());
        document.setExpireAt(Instant.now().plusSeconds(convertDaysToSeconds(request.durationDays())));

        return requireActiveTest(request.testId())
                .then(submissionRepository.save(document))
                .map(SubmissionDocument::toDomain)
                .flatMap(this::withResolvedTestName)
                .doOnSuccess(sa -> log.info("Submission ID: '{}' created successful", sa.submission().submissionId()))
                .onErrorMap(e -> {
                    log.error("Error creating submission for clientId: {}", request.clientId(), e);
                    if (e instanceof SubmissionException) {
                        return e;
                    }
                    return new SubmissionException(
                            "Failed to create submission",
                            e,
                            SubmissionException.SubmissionErrorType.SUBMISSION_CREATE_ERROR
                    );
                });
    }

    @Override
    public Mono<SubmissionView> updateSubmission(SubmissionUpdateDto request, String submissionId) {
        return submissionRepository.findBySubmissionId(submissionId)
                .switchIfEmpty(Mono.error(new SubmissionException(
                        "Submission not found: " + submissionId,
                        SubmissionException.SubmissionErrorType.SUBMISSION_NOT_FOUND
                )))
                .flatMap(document -> {
                            if (document.getStatus() == SubmissionStatus.DONE) {
                                return Mono.error(
                                        new SubmissionStateException(
                                                "Can't update submission Id: " + submissionId + " because test is already done!",
                                                SubmissionException.SubmissionErrorType.SUBMISSION_UPDATE_ERROR
                                        )
                                );
                            }
                            // update also revives expired tokens (new expireAt) and can re-point
                            // testId — both must target an existing, non-deleted test
                            return requireActiveTest(request.testId())
                                    .then(Mono.defer(() -> {
                                        document.setClientEmail(request.clientEmail());
                                        document.setClientName(request.clientName());
                                        document.setTestId(request.testId());
                                        document.setDurationDays(request.durationDays());
                                        document.setExpireAt(Instant.now().plusSeconds(convertDaysToSeconds(request.durationDays())));
                                        return submissionRepository.save(document);
                                    }));
                        }
                )
                .map(SubmissionDocument::toDomain)
                .flatMap(this::withResolvedTestName)
                .doOnSuccess(suc -> log.info("Submission ID: {} updated successful", submissionId))
                .onErrorMap(e -> {
                    log.error("Error updating submission ID: {}", submissionId, e);
                    if (e instanceof SubmissionStateException || e instanceof SubmissionException) {
                        return e;
                    } else {
                        return new SubmissionException(
                                "Failed to update submission",
                                e,
                                SubmissionException.SubmissionErrorType.SUBMISSION_UPDATE_ERROR);
                    }
                });

    }

    @Override
    public Flux<SubmissionView> findAllByOrderByCreatedAtDesc() {
        return submissionRepository.findAllByOrderByCreatedAtDesc()
                .map(SubmissionDocument::toDomain)
                .collectList()
                .flatMapMany(submissions -> {
                    if (submissions.isEmpty()) {
                        return Flux.empty();
                    }
                    // resolve test names once for the whole list (map), not one query per row
                    var testIds = submissions.stream().map(Submission::testId).distinct().toList();
                    return fpTestRepository.findByTestIdIn(testIds)
                            .collectMap(FpTestDocument::getTestId, FpTestDocument::getTestName)
                            .flatMapMany(testNamesById -> Flux.fromIterable(submissions)
                                    .map(submission -> new SubmissionView(
                                            submission,
                                            testNamesById.getOrDefault(submission.testId(), submission.testId()))));
                })
                .doOnComplete(() -> log.info("Retrieved all submissions ordered by createdAt descending"));
    }

    @Override
    public Mono<Void> deleteSubmission(String submissionId) {
        return submissionRepository.findBySubmissionId(submissionId)
                .flatMap(submissionDocument -> {
                    if (submissionDocument.getStatus() == SubmissionStatus.DONE) {
                        return Mono.error(
                                new SubmissionStateException(
                                        "Can't delete submission Id: " + submissionId + " because test is already done!",
                                        SubmissionException.SubmissionErrorType.SUBMISSION_DELETE_ERROR
                                )
                        );
                    } else {
                        return submissionRepository.deleteBySubmissionId(submissionId);
                    }
                })
                .doOnError(e -> {
                    log.error("Can't delete submissionID: {}", submissionId, e);
                });
    }

    @Override
    public Mono<SubmissionView> closeSubmission(String submissionId) {
        return submissionRepository.findBySubmissionId(submissionId)
                .switchIfEmpty(Mono.error(new SubmissionException(
                        "Submission not found: " + submissionId,
                        SubmissionException.SubmissionErrorType.SUBMISSION_NOT_FOUND
                )))
                .flatMap(document -> {
                            document.setStatus(SubmissionStatus.DONE);
                            return submissionRepository.save(document);
                        }
                )
                .map(SubmissionDocument::toDomain)
                .flatMap(this::withResolvedTestName)
                .doOnSuccess(suc -> log.info("Submission ID: {} closed successful", submissionId))
                .onErrorMap(e -> {
                    log.error("Error closing submission ID: {}", submissionId, e);
                    if (e instanceof SubmissionException) {
                        return e;
                    }
                    return new SubmissionException(
                            "Failed to close submission",
                            e,
                            SubmissionException.SubmissionErrorType.SUBMISSION_UPDATE_ERROR);
                });
    }

    @Override
    public Mono<Boolean> existsByTestId(String testId) {
        return submissionRepository.existsByTestId(testId);
    }

    @Override
    public Mono<Boolean> existsByTestIdAndStatusAndExpireAtAfter(String testId, SubmissionStatus status, Instant now) {
        return submissionRepository.existsByTestIdAndStatusAndExpireAtAfter(testId, status, now);
    }

    @Override
    public Flux<Submission> findAllByTestId(String testId) {
        return submissionRepository.findAllByTestId(testId)
                .map(SubmissionDocument::toDomain);
    }

    @Override
    public Mono<Submission> findByPublicTokenAndStatusAndExpireAtAfter(String publicToken, SubmissionStatus status, Instant now) {
        return submissionRepository
                .findByPublicTokenAndStatusAndExpireAtAfter(publicToken, status, now)
                .map(SubmissionDocument::toDomain)
                .doOnSuccess(suc -> log.info("Found submissions for public token: {}, with status: {}",
                                publicToken,
                                status.name()
                        )
                );
    }

    /**
     * Tokens may only be issued (create) or revived (update/extend) for an existing,
     * non-deleted test — otherwise extending an expired OPEN submission would resurrect
     * a soft-deleted FpTest (serve-by-token resolves the test without an isDeleted filter).
     */
    private Mono<Void> requireActiveTest(String testId) {
        return fpTestRepository.findByTestId(testId)
                .switchIfEmpty(Mono.error(new SubmissionException(
                        "Test not found: " + testId,
                        SubmissionException.SubmissionErrorType.TEST_NOT_FOUND)))
                .flatMap(test -> test.isDeleted()
                        ? Mono.error(new SubmissionStateException(
                        "Test is deleted — tokens can't be issued or extended for testId: " + testId,
                        SubmissionException.SubmissionErrorType.TEST_DELETED))
                        : Mono.empty());
    }


    private long convertDaysToSeconds(int days) {
        return days * 24L * 60L * 60L;
    }

    private Mono<SubmissionView> withResolvedTestName(Submission submission) {
        return fpTestRepository.findByTestId(submission.testId())
                .map(fptTest -> new SubmissionView(submission, fptTest.getTestName()))
                .switchIfEmpty(Mono.just(new SubmissionView(submission, submission.testId())));
    }

}
