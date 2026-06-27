package com.emat.reapi.fptest;

import com.emat.reapi.api.dto.fptestdto.FpTestDto;
import com.emat.reapi.api.dto.fptestdto.FpTestResponse;
import com.emat.reapi.fptest.domain.FpTest;
import com.emat.reapi.fptest.domain.FpTestStatement;
import com.emat.reapi.fptest.infra.FpTestDocument;
import com.emat.reapi.fptest.infra.FpTestRepository;
import com.emat.reapi.fptest.infra.FpTestStatementDocument;
import com.emat.reapi.statement.domain.StatementDefinition;
import com.emat.reapi.statement.port.StatementDefinitionService;
import com.emat.reapi.submission.SubmissionService;
import com.emat.reapi.submission.domain.Submission;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
class FpTestServiceImpl implements FpTestService {
    private final FpTestRepository fpTestRepository;
    private final StatementDefinitionService statementDefinitionService;
    private final SubmissionService submissionService;

    @Override
    public Mono<FpTestResponse> createFpTest(FpTestDto fpTestDto) {
        var testId = "fpt_" + UUID.randomUUID().toString();
        return buildFpTestStatements(fpTestDto.statementKeys())
                .flatMap(fpTestStatementDocuments -> {
                            FpTest domain = new FpTest(
                                    null,
                                    testId,
                                    fpTestDto.testName(),
                                    fpTestDto.descriptionBefore(),
                                    fpTestDto.descriptionAfter(),
                                    fpTestStatementDocuments,
                                    null,
                                    null
                            );
                            return fpTestRepository
                                    .save(FpTestDocument.fromDomain(domain))
                                    .flatMap(doc -> mapToResponseWithSubmissions(doc.toDomain()));
                        }
                );
    }

    @Override
    public Mono<FpTestResponse> updateFpTest(FpTestDto fpTestDto) {
        if (fpTestDto.testId() == null || fpTestDto.testId().isBlank()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "testId is required for update"
            ));
        }
        return fpTestRepository.findByTestId(fpTestDto.testId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Test with testId=" + fpTestDto.testId() + " not found"
                )))
                .flatMap(existing -> buildFpTestStatements(fpTestDto.statementKeys())
                        .flatMap(fpTestStatements -> {
                            existing.setTestName(fpTestDto.testName());
                            existing.setDescriptionBefore(fpTestDto.descriptionBefore());
                            existing.setDescriptionAfter(fpTestDto.descriptionAfter());

                            return submissionService.existsByTestId(existing.getTestId())
                                    .flatMap(isSubmission -> {
                                        List<String> currentStatements = existing.getFpTestStatementDocuments()
                                                .stream()
                                                .map(FpTestStatementDocument::statementKey)
                                                .toList();
                                        if (!areStatementsTheSame(fpTestDto.statementKeys(), currentStatements) && isSubmission) {
                                            return Mono.error(new FpTestStateException(
                                                    "Can't update statements in test with ID: " + existing.getTestId() + " because is already submitted!",
                                                    FpTestStateException.FpTestErrorType.FP_TEST_EDIT_ERROR
                                            ));
                                        }
                                        existing.setFpTestStatementDocuments(FpTestStatementDocument.fromDomainlist(fpTestStatements));
                                        return fpTestRepository.save(existing);
                                    });
                        }))
                .flatMap(doc -> mapToResponseWithSubmissions(doc.toDomain()));
    }

    @Override
    public Flux<FpTestResponse> findAll() {
        return fpTestRepository.findAll()
                .map(FpTestDocument::toDomain)
                .flatMap(this::mapToResponseWithSubmissions);
    }

    private Mono<FpTestResponse> mapToResponseWithSubmissions(FpTest fpTest) {
        return submissionService.findAllByTestId(fpTest.testId())
                .collectList()
                .map(list -> {
                    var submissionIds = list.stream().map(Submission::submissionId).toList();
                    return FpTestResponse.toResponse(fpTest, submissionIds);
                });
    }

    @Override
    public Mono<FpTestResponse> findFpTestByTestId(String testId) {
        return fpTestRepository.findByTestId(testId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Test with testId=" + testId + " not found"
                )))
                .flatMap(doc -> mapToResponseWithSubmissions(doc.toDomain()));
    }

    @Override
    public Mono<Void> deleteFpTestByTestId(String testId) {
        return fpTestRepository.findByTestId(testId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Test with testId=" + testId + " not found"
                )))
                .flatMap(doc -> submissionService.existsByTestId(testId))
                .flatMap(isSubmission -> {
                    if (isSubmission) {
                        return Mono.error(new FpTestStateException(
                                "Can't delete test Id: " + testId + " because is already submitted",
                                FpTestStateException.FpTestErrorType.FP_TEST_DELETE_ERROR));
                    }
                    return fpTestRepository.deleteByTestId(testId);
                });
    }

    @Override
    public Flux<FpTestStatement> getAllTestStatements() {
        return statementDefinitionService.getAllStatementDefinitions()
                .map(FpTestStatement::formStatementDefinition);
    }

    @Override
    public Mono<FpTest> getFpTesByTestId(String testId) {
        return fpTestRepository.findByTestId(testId)
                .map(FpTestDocument::toDomain);
    }

    private boolean areStatementsTheSame(List<String> newStatementKeys, List<String> currentStatementKeys) {
        return new HashSet<>(currentStatementKeys).containsAll(newStatementKeys)
                && currentStatementKeys.size() == newStatementKeys.size();
    }

    private Mono<List<FpTestStatement>> buildFpTestStatements(List<String> statementKeys) {
        return statementDefinitionService.getAllStatementDefinitions()
                .collectMap(StatementDefinition::getStatementKey)
                .flatMap(definitionsByKey -> {
                    List<String> missingKeys = statementKeys.stream()
                            .filter(key -> !definitionsByKey.containsKey(key))
                            .toList();
                    if (!missingKeys.isEmpty()) {
                        String msg = "Keys not found in statement definitions: " +
                                String.join(", ", missingKeys);
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, msg));
                    }
                    List<FpTestStatement> statements = statementKeys.stream()
                            .map(definitionsByKey::get)
                            .map(FpTestStatement::formStatementDefinition) // -> FpTestStatementDocument
                            .map(doc -> new FpTestStatement(
                                    doc.statementKey(),
                                    doc.statementsDescription(),
                                    doc.statementsCategory()
                            ))
                            .collect(Collectors.toList());
                    return Mono.just(statements);
                });
    }
}
