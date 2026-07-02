package com.emat.reapi.statement.port;

import com.emat.reapi.fptest.infra.FpTestDocument;
import com.emat.reapi.fptest.infra.FpTestRepository;
import com.emat.reapi.statement.StatementDefinitionStateException;
import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.infra.ProfileRepository;
import com.emat.reapi.statement.infra.StatementDefinitionDocument;
import com.emat.reapi.statement.infra.StatementDefinitionRepository;
import com.emat.reapi.submission.SubmissionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class StatementDefinitionServiceImpl implements StatementDefinitionService {
    private static final String STATEMENT_KEY_PREFIX = "sk_";

    private final StatementDefinitionRepository statementDefinitionRepository;
    private final ProfileRepository profileRepository;
    private final FpTestRepository fpTestRepository;
    private final SubmissionService submissionService;

    @Override
    public Mono<StatementDefinition> createStatementDefinition(StatementDefinition statementDefinition) {
        // statementKey is server-owned: generated once here, immutable afterwards
        String statementKey = STATEMENT_KEY_PREFIX + UUID.randomUUID();
        log.info("Creating statement definition with key: {}", statementKey);
        return requireActiveProfile(statementDefinition.getProfileId())
                .then(Mono.defer(() -> {
                    StatementDefinition toCreate = statementDefinition.toBuilder()
                            .id(null) // let Mongo generate the identifier
                            .statementKey(statementKey)
                            .isDeleted(false)
                            .build();
                    return statementDefinitionRepository.save(StatementDefinitionDocument.toDocument(toCreate));
                }))
                .map(StatementDefinitionDocument::toDomain);
    }

    @Override
    public Mono<StatementDefinition> updateStatementDefinition(String id, StatementDefinition updatedDefinition) {
        log.info("Updating statement definition: {}", id);
        return statementDefinitionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Statement definition not found: " + id)))
                .flatMap(existing -> requireActiveProfile(updatedDefinition.getProfileId()).thenReturn(existing))
                .flatMap(existing -> guardProfileReassignment(existing, updatedDefinition.getProfileId()).thenReturn(existing))
                .flatMap(existing -> {
                    // statementKey is immutable — only the profile assignment and texts are editable
                    existing.setProfileId(updatedDefinition.getProfileId());
                    existing.setStatementTypeDefinitions(updatedDefinition.getStatementTypeDefinitions());
                    return statementDefinitionRepository.save(existing);
                })
                .map(StatementDefinitionDocument::toDomain);
    }

    @Override
    public Mono<Void> softDeleteStatementDefinition(String id) {
        log.info("Soft-deleting statement definition: {}", id);
        return statementDefinitionRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Statement definition not found: " + id)))
                .flatMap(existing ->
                        // "In use" counts only existing FpTests (live templates). Completed client
                        // tests never block: their history is self-contained (texts + profile
                        // snapshots frozen in ClientTestDocument).
                        // TODO F8.5: once FpTest gets soft-delete, narrow to non-deleted FpTests.
                        fpTestRepository.existsByFpTestStatementDocumentsStatementKey(existing.getStatementKey())
                                .flatMap(inUse -> {
                                    if (Boolean.TRUE.equals(inUse)) {
                                        return Mono.error(new StatementDefinitionStateException(
                                                "Definition is referenced by an existing test and cannot be deleted: "
                                                        + existing.getStatementKey(),
                                                StatementDefinitionStateException.DefinitionErrorType.DEFINITION_IN_USE));
                                    }
                                    existing.setDeleted(true);
                                    return statementDefinitionRepository.save(existing);
                                }))
                .then();
    }

    @Override
    public Flux<StatementDefinition> getActiveStatementDefinitions() {
        log.info("Retrieving all active statement definitions");
        return statementDefinitionRepository.findAllByIsDeletedFalseOrderByCreatedAtAsc()
                .map(StatementDefinitionDocument::toDomain);
    }

    @Override
    public Flux<StatementDefinition> getStatementDefinitionsByProfileId(String profileId) {
        log.info("Retrieving active statement definitions for profileId: {}", profileId);
        return statementDefinitionRepository.findAllByProfileIdAndIsDeletedFalseOrderByCreatedAtAsc(profileId)
                .map(StatementDefinitionDocument::toDomain);
    }

    private Mono<Void> requireActiveProfile(String profileId) {
        return profileRepository.findById(profileId)
                .filter(profile -> !profile.isDeleted())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown or deleted profileId: " + profileId)))
                .then();
    }

    /**
     * Changing the profile of a definition that sits in an already-submitted FpTest would
     * silently alter how future retakes of that test are scored — rejected with 409,
     * consistent with the FpTest statement-edit guard. Completed results themselves are
     * safe either way (profile snapshots); this protects the live template's integrity.
     */
    private Mono<Void> guardProfileReassignment(StatementDefinitionDocument existing, String newProfileId) {
        if (existing.getProfileId().equals(newProfileId)) {
            return Mono.empty();
        }
        return fpTestRepository.findAllByFpTestStatementDocumentsStatementKey(existing.getStatementKey())
                .map(FpTestDocument::getTestId)
                .flatMap(submissionService::existsByTestId)
                .any(Boolean.TRUE::equals)
                .flatMap(inSubmittedTest -> Boolean.TRUE.equals(inSubmittedTest)
                        ? Mono.error(new StatementDefinitionStateException(
                                "Can't change profile of definition " + existing.getStatementKey()
                                        + " because it is used in an already submitted test",
                                StatementDefinitionStateException.DefinitionErrorType.DEFINITION_EDIT_ERROR))
                        : Mono.empty());
    }
}
