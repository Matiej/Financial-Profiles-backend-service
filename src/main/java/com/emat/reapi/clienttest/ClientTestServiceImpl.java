package com.emat.reapi.clienttest;

import com.emat.reapi.api.dto.clienttestdto.ClientTestAnswerDto;
import com.emat.reapi.api.dto.clienttestdto.ClientTestSubmissionDto;
import com.emat.reapi.clienttest.domain.ClientTest;
import com.emat.reapi.clienttest.domain.ClientTestAnswer;
import com.emat.reapi.clienttest.domain.ClientTestQuestion;
import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import com.emat.reapi.clienttest.infra.ClientTestDocument;
import com.emat.reapi.clienttest.infra.ClientTestRepository;
import com.emat.reapi.fptest.FpTestService;
import com.emat.reapi.fptest.domain.FpTest;
import com.emat.reapi.infrastructure.n8n.N8nService;
import com.emat.reapi.statement.domain.Profile;
import com.emat.reapi.statement.domain.ProfileSnapshot;
import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.domain.StatementType;
import com.emat.reapi.statement.domain.StatementTypeDefinition;
import com.emat.reapi.statement.port.ProfileService;
import com.emat.reapi.statement.port.StatementDefinitionService;
import com.emat.reapi.submission.SubmissionService;
import com.emat.reapi.submission.domain.Submission;
import com.emat.reapi.submission.domain.SubmissionStatus;
import com.emat.reapi.submission.domain.SubmissionView;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.emat.reapi.statement.domain.StatementType.LIMITING;
import static com.emat.reapi.statement.domain.StatementType.SUPPORTING;

@Slf4j
@AllArgsConstructor
@Service
public class ClientTestServiceImpl implements ClientTestService {
    private final SubmissionService submissionService;
    private final FpTestService fpTestService;
    private final StatementDefinitionService statementDefinitionService;
    private final ProfileService profileService;
    private final ClientTestRepository clientTestRepository;
    private final N8nService n8nService;

    @Override
    public Mono<ClientTest> getClientTestByToken(String publicToken) {
        return submissionService.findByPublicTokenAndStatusAndExpireAtAfter(publicToken, SubmissionStatus.OPEN, Instant.now())
                .switchIfEmpty(Mono.error(new ClientTestException(
                                        "Can't find open submission for publicToken: " + publicToken,
                                        HttpStatus.NOT_FOUND
                                )
                        )
                ).flatMap(sub -> Mono.zip(
                        Mono.just(sub),
                        fpTestService.getFpTesByTestId(sub.testId())
                                .switchIfEmpty(Mono.error(new ClientTestException(
                                                "Can't find FpTest for publicToken: " + publicToken,
                                                HttpStatus.NOT_FOUND
                                        )
                                ))
                ).flatMap(both -> {
                    Submission submission = both.getT1();
                    FpTest fpTest = both.getT2();
                    return Mono.zip(
                            statementDefinitionService.getActiveStatementDefinitions().collectList(),
                            profileService.getActiveProfiles().collectMap(Profile::getId, Profile::getPlName)
                    ).map(tuple -> {
                        List<StatementDefinition> defList = tuple.getT1();
                        Map<String, String> profileNamesById = tuple.getT2();
                        List<StatementDefinition> definitions = fpTest.fpTestStatements()
                                .stream()
                                .map(fpTestStatement -> findDefByStatementKey(fpTestStatement.statementKey(), defList))
                                .toList();
                        List<ClientTestQuestion> clientTestQuestions = definitions
                                .stream()
                                .map(definition -> mapToTestQuestions(definition, profileNamesById))
                                .collect(Collectors.toCollection(ArrayList::new));
                        Collections.shuffle(clientTestQuestions);
                        return new ClientTest(
                                fpTest.testName(),
                                fpTest.descriptionBefore(),
                                fpTest.descriptionAfter(),
                                submission.publicToken(),
                                submission.submissionId(),
                                clientTestQuestions
                        );
                    });
                }));
    }

    @Override
    public Mono<Void> saveClientTest(ClientTestSubmissionDto request) {
        return submissionService.findBySubmissionId(request.submissionId())
                // internal token flow needs the bare Submission, not the admin read-model
                .map(SubmissionView::submission)
                .flatMap(submission -> {
                    if (!submission.publicToken().equals(request.publicToken())) {
                        return Mono.error(new ClientTestException(
                                "Public token does not match submission",
                                HttpStatus.BAD_REQUEST
                        ));
                    }
                    Mono<List<StatementDefinition>> monoDef = statementDefinitionService.getActiveStatementDefinitions()
                            .collectList()
                            .flatMap(definitions -> validateQuestions(request.clientTestAnswers(), definitions).thenReturn(definitions));
                    Mono<FpTest> fpTestMono = fpTestService.getFpTesByTestId(submission.testId())
                            .switchIfEmpty(Mono.error(new ClientTestException(
                                    "Test definition missing for testId=" + submission.testId(),
                                    HttpStatus.NOT_FOUND
                            )));
                    return Mono.zip(Mono.just(submission), fpTestMono, monoDef);
                }).flatMap((both) -> {
                    Submission submission = both.getT1();
                    FpTest fpTest = both.getT2();
                    List<StatementDefinition> statementDefinitions = both.getT3();

                    if (fpTest.fpTestStatements().size() != request.clientTestAnswers().size()) {
                        return Mono.error(new ClientTestException("Number of client test are different than in submitted test", HttpStatus.BAD_REQUEST));
                    }
                    String testSubmissionPublicId = "tsb_" + UUID.randomUUID();
                    ClientTestSubmission clientTestSubmission = new ClientTestSubmission(
                            testSubmissionPublicId,
                            submission.clientId(),
                            submission.clientName(),
                            submission.clientEmail(),
                            submission.submissionId(),
                            submission.createdAt(),
                            fpTest.testId(),
                            fpTest.testName(),
                            submission.publicToken(),
                            mapToDomain(statementDefinitions, request.clientTestAnswers())
                    );
                    ClientTestDocument clientTestDocument = ClientTestDocument.fromDomain(clientTestSubmission);
                    return buildProfileSnapshots(clientTestSubmission)
                            .doOnNext(clientTestDocument::setProfileSnapshots)
                            .then(clientTestRepository.save(clientTestDocument))
                            .onErrorMap(err -> {
                                if (err instanceof DuplicateKeyException) {
                                    return new ClientTestException(
                                            "Can't save client test for submissionId: " + submission.submissionId() + " - already exists",
                                            HttpStatus.CONFLICT,
                                            err);
                                } else {
                                    return new ClientTestException(
                                            "Can't save client test for submissionId: " + submission.submissionId(),
                                            HttpStatus.INTERNAL_SERVER_ERROR,
                                            err);
                                }
                            })
                            .flatMap(saved ->
                                    n8nService.sendScoringTestNotification(saved.toDomain())
                                            .timeout(java.time.Duration.ofSeconds(3))
                                            .doOnError(e -> log.warn("n8n notification failed, ignoring", e))
                                            .onErrorResume(e -> Mono.empty())
                                            .thenReturn(saved)
                            )
                            .flatMap(saved -> submissionService.closeSubmission(submission.submissionId()))
                            .then();
                }).doOnError(ex -> log.error("Error saving client test answers"))
                .doOnSuccess(suc -> log.info("Saved '{}' client answers for submission '{}'",
                        request.clientTestAnswers().size(),
                        request.submissionId()))
                .then();
    }

    // Freeze the labels of every profile referenced by the test's answers (profileId -> snapshot),
    // captured at save time so historical scoring/AI stays stable across later profile edits/deletes.
    private Mono<Map<String, ProfileSnapshot>> buildProfileSnapshots(ClientTestSubmission submission) {
        List<String> profileIds = submission.getClientTestAnswerList().stream()
                .map(ClientTestAnswer::profileId)
                .distinct()
                .toList();
        return Flux.fromIterable(profileIds)
                .flatMap(profileService::getProfileById)
                .collectMap(Profile::getId, ProfileSnapshot::of);
    }

    private List<ClientTestAnswer> mapToDomain(List<StatementDefinition> statementDefinitions, List<ClientTestAnswerDto> clientTestAnswers) {
        var definitionByKey = statementDefinitions.stream()
                .collect(Collectors.toMap(
                        StatementDefinition::getStatementKey,
                        d -> d
                ));

        return clientTestAnswers.stream()
                .map(answerDto -> {
                    String statementKey = answerDto.statementKey();
                    StatementDefinition statementDefinition = definitionByKey.get(statementKey);
                    return new ClientTestAnswer(
                            statementKey,
                            statementDefinition.getProfileId(),
                            mapToDefinition(statementDefinition.getStatementTypeDefinitions(), LIMITING),
                            mapToDefinition(statementDefinition.getStatementTypeDefinitions(), SUPPORTING),
                            answerDto.scoring()
                    );
                }).toList();
    }

    private String mapToDefinition(List<StatementTypeDefinition> statementTypeDefinitions, StatementType type) {
        return statementTypeDefinitions.stream()
                .filter(d -> d.getStatementType() == type)
                .findFirst()
                .map(StatementTypeDefinition::getStatementDescription)
                .orElseThrow();
    }

    private Mono<Void> validateQuestions(List<ClientTestAnswerDto> answers, List<StatementDefinition> definitions) {
        List<String> keys = definitions.stream()
                .map(StatementDefinition::getStatementKey)
                .toList();
        var missing = answers
                .stream()
                .map(ClientTestAnswerDto::statementKey)
                .filter(p -> !keys.contains(p))
                .toList();

        if (!missing.isEmpty()) {
            return Mono.error(new ClientTestException(
                    "Keys not found in statement definitions: " + String.join(", ", missing),
                    HttpStatus.NOT_FOUND)
            );
        }
        return Mono.empty();
    }

    private StatementDefinition findDefByStatementKey(String statementKey, List<StatementDefinition> statementDefinitions) {
        return statementDefinitions
                .stream()
                .filter(p -> p.getStatementKey().equalsIgnoreCase(statementKey))
                .findFirst()
                .orElseThrow(() -> new ClientTestException(
                                "Can't find statement definition for key: " + statementKey,
                                HttpStatus.NOT_FOUND
                        )
                );
    }

    private ClientTestQuestion mapToTestQuestions(StatementDefinition statementDefinition, Map<String, String> profileNamesById) {
        StatementTypeDefinition supportingDef = getStatementType(StatementType.SUPPORTING, statementDefinition.getStatementTypeDefinitions());
        StatementTypeDefinition limitingDef = getStatementType(StatementType.LIMITING, statementDefinition.getStatementTypeDefinitions());
        String profileId = statementDefinition.getProfileId();
        return new ClientTestQuestion(
                // definition document id — opaque to the client app (answers reference statementKey)
                statementDefinition.getId(),
                statementDefinition.getStatementKey(),
                profileNamesById.getOrDefault(profileId, profileId),
                supportingDef.getStatementDescription(),
                limitingDef.getStatementDescription()
        );
    }

    private StatementTypeDefinition getStatementType(StatementType type, List<StatementTypeDefinition> statementTypeDefinitions) {
        return statementTypeDefinitions
                .stream()
                .filter(stType -> stType.getStatementType() == type)
                .findFirst()
                .orElseThrow(() -> new ClientTestException(
                                "Can't find statement type: " + type.name(),
                                HttpStatus.NOT_FOUND
                        )
                );
    }

}

