package com.emat.reapi.profiler;

import com.emat.reapi.clienttest.ClientTestSubmissionService;
import com.emat.reapi.clienttest.domain.ClientTestAnswer;
import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import com.emat.reapi.profiler.domain.*;
import com.emat.reapi.statement.domain.StatementProfile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ProfiledScoringTestServiceImpl implements ProfiledScoringTestService {
    private final ClientTestSubmissionService clientTestSubmissionService;

    @Override
    public Mono<ScoringProfiledClientDetails> getScoringProfile(String testSubmissionPublicId) {
        log.info("Retrieving ScoringProfiledClientDetails for submissionsId: {}.", testSubmissionPublicId);
        return clientTestSubmissionService.findClientTestByTestSubmissionId(testSubmissionPublicId)
                .switchIfEmpty(Mono.error(new ProfilerException(
                        "Can't find clientTest for testSubmissionPublicId; {}" + testSubmissionPublicId,
                        HttpStatus.NOT_FOUND,
                        ProfilerException.ProfilerErrorType.GENERATE_SCORING_PROFILE_ERROR)))
                .map(this::mapToProfile)
                .doOnSuccess(suc -> log.info("Successful prepared ScoringProfiledClientDetails for testSubmissionPublicId: {}.", testSubmissionPublicId));
    }

    @Override
    public Flux<ScoringProfiledShort> getScoringShortProfiles() {
        log.info("Retrieving all ScoringProfiledClientShort s.");
        return clientTestSubmissionService.findAll()
                .map(this::mapToScoringProfilerShort);
    }

    private ScoringProfiledClientDetails mapToProfile(ClientTestSubmission clientTestSubmission) {
        Map<StatementProfile, List<ClientTestAnswer>> byProfile =
                clientTestSubmission.getClientTestAnswerList().stream()
                        .collect(Collectors.groupingBy(ClientTestAnswer::category));

        ScoringOverallSummary overall = buildOverallSummary(clientTestSubmission.getClientTestAnswerList());

        List<ScoringProfileBlock> profiles = byProfile.entrySet().stream()
                .map(entry -> buildProfileBlock(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(ScoringProfileBlock::getTotalScore))
                .toList();

        return new ScoringProfiledClientDetails(
                clientTestSubmission.getTestSubmissionPublicId(),
                clientTestSubmission.getClientName(),
                clientTestSubmission.getClientId(),
                clientTestSubmission.getSubmissionId(),
                clientTestSubmission.getSubmissionDate(),
                clientTestSubmission.getTestId(),
                clientTestSubmission.getTestName(),
                clientTestSubmission.getCreatedAt(),
                overall,
                profiles
        );
    }

    private ScoringOverallSummary buildOverallSummary(List<ClientTestAnswer> answers) {
        Map<String, Long> buckets = answers.stream()
                .collect(Collectors.groupingBy(
                        answer -> String.valueOf(answer.scoring()),
                        Collectors.counting()
                ));

        int totalScore = answers.stream()
                .mapToInt(ClientTestAnswer::scoring)
                .sum();

        return new ScoringOverallSummary(answers.size(), totalScore, buckets);
    }

    private ScoringProfileBlock buildProfileBlock(StatementProfile profile, List<ClientTestAnswer> answers) {
        int totalScore = answers.stream().mapToInt(ClientTestAnswer::scoring).sum();
        int totalAnswers = answers.size();
        double avgScore = totalAnswers == 0 ? 0.0 : (double) totalScore / totalAnswers;
        double scorePercent = StatementProfile.computePercent(totalScore, totalAnswers);
        String computedLabel = profile.computeLabel(scorePercent);

        List<ScoringStatementPair> pairs = answers.stream()
                .map(answer -> new ScoringStatementPair(
                        answer.statementKey(),
                        answer.limitingDescription(),
                        answer.supportingDescription(),
                        answer.scoring()
                ))
                .sorted(Comparator.comparingInt(ScoringStatementPair::getScoring))
                .toList();

        return new ScoringProfileBlock(
                profile.name(),
                profile.getPlName(),
                computedLabel,
                Math.round(scorePercent * 10.0) / 10.0,
                totalAnswers,
                totalScore,
                Math.round(avgScore * 100.0) / 100.0,
                pairs
        );
    }

    private ScoringProfiledShort mapToScoringProfilerShort(ClientTestSubmission clientTestSubmission) {
        int totalScore = buildOverallSummary(clientTestSubmission.getClientTestAnswerList()).getTotalScore();
        return new ScoringProfiledShort(
                clientTestSubmission.getTestSubmissionPublicId(),
                clientTestSubmission.getClientName(),
                clientTestSubmission.getClientId(),
                clientTestSubmission.getCreatedAt(),
                clientTestSubmission.getSubmissionId(),
                clientTestSubmission.getSubmissionDate(),
                clientTestSubmission.getTestName(),
                totalScore,
                clientTestSubmission.getClientTestAnswerList().size()
        );
    }
}
