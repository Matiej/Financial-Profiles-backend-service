package com.emat.reapi.clienttest;

import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import com.emat.reapi.clienttest.infra.ClientTestDocument;
import com.emat.reapi.clienttest.infra.ClientTestRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@AllArgsConstructor
@Service
public class ClientTestSubmissionServiceImpl implements ClientTestSubmissionService {
    private final ClientTestRepository clientTestRepository;

    @Override
    public Mono<ClientTestSubmission> findClientTestByTestSubmissionId(String testSubmissionId) {
        log.info("Retrieving ClientTestSubmission for submissionsId: {}.", testSubmissionId);
        return clientTestRepository.findByTestSubmissionPublicIdAndDeletedFalse(testSubmissionId)
                .map(ClientTestDocument::toDomain)
                .doOnSuccess(suc -> log.info("Successfully fetched ClientTestDocument for submissionID: {}", testSubmissionId));
    }

    @Override
    public Mono<ClientTestSubmission> findBySubmissionId(String submissionId) {
        log.info("Retrieving ClientTestSubmission by submissionId: {}.", submissionId);
        return clientTestRepository.findBySubmissionIdAndDeletedFalse(submissionId)
                .map(ClientTestDocument::toDomain)
                .doOnSuccess(suc -> log.info("Successfully fetched ClientTestDocument for submissionId: {}", submissionId));
    }

    @Override
    public Flux<ClientTestSubmission> findAll() {
        log.info("Retrieving all ClientTestSubmissiona ");
        return clientTestRepository.findAllByDeletedFalse()
                .map(ClientTestDocument::toDomain);
    }

    @Override
    public Mono<Void> softDeleteByTestSubmissionPublicId(String testSubmissionPublicId) {
        log.info("Soft-deleting ClientTestSubmission by testSubmissionPublicId: {}.", testSubmissionPublicId);
        // Unfiltered lookup so a missing test yields 404; an already-deleted one is an idempotent no-op.
        return clientTestRepository.findByTestSubmissionPublicId(testSubmissionPublicId)
                .switchIfEmpty(Mono.error(new ClientTestSubmissionException(
                        "Can't find client test for testSubmissionPublicId: " + testSubmissionPublicId,
                        HttpStatus.NOT_FOUND
                )))
                .flatMap(document -> {
                    if (document.isDeleted()) {
                        log.info("ClientTestSubmission for testSubmissionPublicId: {} already deleted, skipping.", testSubmissionPublicId);
                        return Mono.empty();
                    }
                    document.setDeleted(true);
                    document.setDeletedAt(Instant.now());
                    return clientTestRepository.save(document)
                            .doOnSuccess(saved -> log.info("Soft-deleted ClientTestSubmission for testSubmissionPublicId: {}", testSubmissionPublicId));
                })
                .then();
    }
}
