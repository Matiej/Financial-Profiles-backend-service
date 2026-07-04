package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.profiler.domain.ScoringOverallSummary;

import java.util.Map;

public record OverallSummaryNotification(
        int totalAnswers,
        int totalScore,
        Map<String, Long> scoreBuckets
) {
    public static OverallSummaryNotification of(ScoringOverallSummary summary) {
        return new OverallSummaryNotification(
                summary.getTotalAnswers(),
                summary.getTotalScore(),
                summary.getScoreBuckets()
        );
    }
}
