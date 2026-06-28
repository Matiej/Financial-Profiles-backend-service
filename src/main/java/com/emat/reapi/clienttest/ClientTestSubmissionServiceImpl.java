package com.emat.reapi.clienttest;

import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import com.emat.reapi.clienttest.infra.ClientTestDocument;
import com.emat.reapi.clienttest.infra.ClientTestRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
@Service
public class ClientTestSubmissionServiceImpl implements ClientTestSubmissionService {
    private final ClientTestRepository clientTestRepository;

    @Override
    public Mono<ClientTestSubmission> findClientTestByTestSubmissionId(String testSubmissionId) {
        log.info("Retrieving ClientTestSubmission for submissionsId: {}.", testSubmissionId);
        return clientTestRepository.findByTestSubmissionPublicId(testSubmissionId)
                .map(ClientTestDocument::toDomain)
                .doOnSuccess(suc -> log.info("Successfully fetched ClientTestDocument for submissionID: {}", testSubmissionId));
    }

    @Override
    public Mono<ClientTestSubmission> findBySubmissionId(String submissionId) {
        log.info("Retrieving ClientTestSubmission by submissionId: {}.", submissionId);
        return clientTestRepository.findBySubmissionId(submissionId)
                .map(ClientTestDocument::toDomain)
                .doOnSuccess(suc -> log.info("Successfully fetched ClientTestDocument for submissionId: {}", submissionId));
    }

    @Override
    public Flux<ClientTestSubmission> findAll() {
        log.info("Retrieving all ClientTestSubmissiona ");
        return clientTestRepository.findAll()
                .map(ClientTestDocument::toDomain);
    }
}
