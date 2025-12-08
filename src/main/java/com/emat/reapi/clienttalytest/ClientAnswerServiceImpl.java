package com.emat.reapi.clienttalytest;

import com.emat.reapi.api.dto.ClientAnswerDto;
import com.emat.reapi.clienttalytest.domain.ClientAnswer;
import com.emat.reapi.clienttalytest.infra.ClientAnswerDocument;
import com.emat.reapi.clienttalytest.infra.ClientAnswerRepository;
import com.emat.reapi.clienttalytest.infra.ClientAnswerShortProjection;
import com.emat.reapi.statement.infra.StatementDefinitionDocument;
import com.emat.reapi.statement.infra.StatementDefinitionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
class ClientAnswerServiceImpl implements ClientAnswerService {
    private final ClientAnswerRepository clientAnswerRepository;
    private final StatementDefinitionRepository statementDefinitionRepository;

    @Override
    public Mono<Void> saveClientAnswers(ClientAnswerDto clientAnswer) {
        log.info("Saving client answers for clientId: {}", clientAnswer.clientId());
        return statementDefinitionRepository.findAll()
                .map(StatementDefinitionDocument::toDomain)
                .collectList()
                .flatMap(list -> {
                            ClientAnswer clientAnswerDomain = clientAnswer.toDomain(list);
                            ClientAnswerDocument document = ClientAnswerDocument.toDocument(clientAnswerDomain);
                            return clientAnswerRepository.save(document)
                                    .doOnSuccess(saved -> log.debug("Saved clients answers: {}", clientAnswer))
                                    .doOnError(error -> log.error("Error saving client answers for clientId: {}", clientAnswer.clientId(), error));

                        }
                ).then();
    }

    @Override
    public Mono<List<ClientAnswer>> getAllAnsweredStatements() {
        log.info("Trying to retrieve all clients tests");
        return clientAnswerRepository.findAll()
                .map(ClientAnswerDocument::toDomain)
                .collectList()
                .doOnError(error -> log.error("Error retrieving client test answers"))
                .doOnSuccess(it -> log.info("Retrieved {} client answered tests", it.size())
                );
    }

    @Override
    public Flux<ClientAnswerShortProjection> getAllAnsweredShortProjections(Sort sort) {
        return clientAnswerRepository.findAllProjectedBy(sort)
                .doOnError(error -> log.error("Error retrieving client test answers", error));
    }

    @Override
    public Mono<ClientAnswer> getAnsweredStatementBySubmissionId(String submissionId) {
        log.info("Trying to retrieve client test by submission Id: {}", submissionId);
        return clientAnswerRepository.findClientAnswerDocumentBySubmissionId(submissionId)
                .map(ClientAnswerDocument::toDomain)
                .doOnNext(a -> log.info("Retrieved client test for submission Id: {}", a.getSubmissionId()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No submissions found for {}", submissionId);
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No client statement test found for submissionId=" + submissionId
                    ));
                }))
                .doOnError(error -> log.error("Error retrieving client test answers"));
    }

    @Override
    public Mono<ClientAnswer> getAnsweredStatementByClientId(String clientId) {
        log.info("Trying to retrieve client test by client Id: {}", clientId);
        return clientAnswerRepository.findClientAnswerDocumentByClientId(clientId)
                .map(ClientAnswerDocument::toDomain)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No client answers for clientId={}", clientId);
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No client statement test found for clientId=" + clientId
                    ));
                }))
                .doOnError(error -> log.error("Error retrieving client test answers for client id: {}", clientId))
                .doOnSuccess(it -> log.info("Retrieved client test for client Id: {}", it.getClientId())
                );
    }
}
