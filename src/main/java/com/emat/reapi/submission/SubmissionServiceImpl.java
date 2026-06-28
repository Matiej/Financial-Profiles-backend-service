package com.emat.reapi.submission;

import com.emat.reapi.api.dto.SubmissionDto;
import com.emat.reapi.api.dto.SubmissionUpdateDto;
import com.emat.reapi.submission.domain.Submission;
import com.emat.reapi.submission.domain.SubmissionStatus;
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

    @Override
    public Mono<Submission> findBySubmissionId(String submissionId) {
        return submissionRepository.findBySubmissionId(submissionId)
                .switchIfEmpty(Mono.error(new SubmissionException(
                                "Submission not found: " + submissionId,
                                SubmissionException.SubmissionErrorType.SUBMISSION_NOT_FOUND)
                        )
                )
                .map(SubmissionDocument::toDomain);
    }

    @Override
    public Mono<Submission> createSubmission(SubmissionDto request) {
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

        return submissionRepository.save(document)
                .map(SubmissionDocument::toDomain)
                .doOnSuccess(sa -> log.info("Submission ID: '{}' created successful", sa.submissionId()))
                .onErrorMap(e -> {
                    log.error("Error creating submission for clientId: {}", request.clientId(), e);
                    return new SubmissionException(
                            "Failed to create submission",
                            e,
                            SubmissionException.SubmissionErrorType.SUBMISSION_CREATE_ERROR
                    );
                });
    }

    @Override
    public Mono<Submission> updateSubmission(SubmissionUpdateDto request, String submissionId) {
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
                            document.setClientEmail(request.clientEmail());
                            document.setClientName(request.clientName());
                            document.setTestId(request.testId());
                            document.setDurationDays(request.durationDays());
                            document.setExpireAt(Instant.now().plusSeconds(convertDaysToSeconds(request.durationDays())));
                            return submissionRepository.save(document);
                        }
                )
                .map(SubmissionDocument::toDomain)
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

    private long convertDaysToSeconds(int days) {
        return days * 24L * 60L * 60L;
    }

    @Override
    public Flux<Submission> findAllByOrderByCreatedAtDesc() {
        return submissionRepository.findAllByOrderByCreatedAtDesc()
                .map(SubmissionDocument::toDomain);
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
    public Mono<Submission> closeSubmission(String submissionId) {
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

    public Mono<Boolean> existsByTestId(String testId) {
        return submissionRepository.existsByTestId(testId);
    }

    public Flux<Submission> findAllByTestId(String testId) {
        return submissionRepository.findAllByTestId(testId)
                .map(SubmissionDocument::toDomain);
    }

    @Override
    public Mono<Submission> findByPublicTokenAndStatus(String publicToken, SubmissionStatus status) {
        return submissionRepository.findByPublicTokenAndStatus(publicToken, status)
                .map(SubmissionDocument::toDomain)
                .doOnSuccess(suc -> log.info("Found submissions for public token: {}, with status: {}",
                                publicToken,
                                status.name()
                        )
                );
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
}
