package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.profiler.domain.ScoringProfileBlock;

import java.util.List;

public record ProfileBlockNotification(
        String profileId,
        String profileName,
        String computedLabel,
        double scorePercent,
        int totalAnswers,
        int totalScore,
        double avgScore,
        List<StatementPairNotification> answersBySeverity
) {
    public static ProfileBlockNotification of(ScoringProfileBlock block) {
        return new ProfileBlockNotification(
                block.getProfileId(),
                block.getProfileName(),
                block.getComputedLabel(),
                block.getScorePercent(),
                block.getTotalAnswers(),
                block.getTotalScore(),
                block.getAvgScore(),
                block.getAnswersBySeverity().stream().map(StatementPairNotification::of).toList()
        );
    }
}
