package com.emat.reapi.clienttest;

import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientTestSubmissionService {
    Mono<ClientTestSubmission> findClientTestByTestSubmissionId(String submissionId);
    Mono<ClientTestSubmission> findBySubmissionId(String submissionId);
    Flux<ClientTestSubmission> findAll();

    Mono<Void> softDeleteByTestSubmissionPublicId(String testSubmissionPublicId);
}
