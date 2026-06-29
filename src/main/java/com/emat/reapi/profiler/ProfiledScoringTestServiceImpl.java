package com.emat.reapi.profiler;

import com.emat.reapi.clienttest.ClientTestSubmissionService;
import com.emat.reapi.clienttest.domain.ClientTestAnswer;
import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import com.emat.reapi.profiler.domain.*;
import com.emat.reapi.statement.domain.ProfileLabels;
import com.emat.reapi.statement.domain.ProfileSnapshot;
import com.emat.reapi.statement.domain.ScoringLabelResolver;
import com.emat.reapi.statement.domain.ScoringMath;
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
    private final ScoringLabelResolver scoringLabelResolver;

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
        Map<String, List<ClientTestAnswer>> byProfile =
                clientTestSubmission.getClientTestAnswerList().stream()
                        .collect(Collectors.groupingBy(ClientTestAnswer::profileId));

        Map<String, ProfileSnapshot> snapshots = clientTestSubmission.getProfileSnapshots() == null
                ? Map.of()
                : clientTestSubmission.getProfileSnapshots();

        ScoringOverallSummary overall = buildOverallSummary(clientTestSubmission.getClientTestAnswerList());

        List<ScoringProfileBlock> profiles = byProfile.entrySet().stream()
                .map(entry -> buildProfileBlock(entry.getKey(), entry.getValue(), snapshots.get(entry.getKey())))
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

    private ScoringProfileBlock buildProfileBlock(String profileId, List<ClientTestAnswer> answers, ProfileSnapshot snapshot) {
        int totalScore = answers.stream().mapToInt(ClientTestAnswer::scoring).sum();
        int totalAnswers = answers.size();
        double avgScore = totalAnswers == 0 ? 0.0 : (double) totalScore / totalAnswers;
        double scorePercent = ScoringMath.computePercent(totalScore, totalAnswers);

        // Labels come from the snapshot frozen at test save time. Defensive fallback to the
        // raw profileId when a snapshot is missing (e.g. profile not found at save time).
        String profileName = snapshot != null ? snapshot.plName() : profileId;
        ProfileLabels labels = snapshot != null
                ? ProfileLabels.of(snapshot)
                : new ProfileLabels(profileId, profileId, profileId);
        String computedLabel = scoringLabelResolver.computeLabel(scorePercent, labels);

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
                profileId,
                profileName,
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
