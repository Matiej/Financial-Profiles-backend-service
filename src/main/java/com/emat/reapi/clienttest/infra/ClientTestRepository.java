package com.emat.reapi.clienttest.infra;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ClientTestRepository extends ReactiveMongoRepository<ClientTestDocument, String> {
    Mono<ClientTestDocument> findByTestSubmissionPublicIdAndDeletedFalse(String testSubmissionPublicId);

    Mono<ClientTestDocument> findBySubmissionIdAndDeletedFalse(String submissionId);

    Flux<ClientTestDocument> findAllByDeletedFalse();

    Mono<ClientTestDocument> findByTestSubmissionPublicId(String testSubmissionPublicId);
}
