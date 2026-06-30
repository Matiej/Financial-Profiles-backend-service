package com.emat.reapi.statement.domain;

/**
 * Pure scoring math, extracted from the former StatementProfile enum.
 */
public final class ScoringMath {

    private ScoringMath() {
    }

    /**
     * Percent of the maximum achievable score, where each answer contributes up to 2 points.
     * Returns 0 when there are no answers (division-by-zero guard).
     */
    public static double computePercent(int totalScore, int totalAnswers) {
        if (totalAnswers == 0) return 0;
        return (double) totalScore / (totalAnswers * 2) * 100;
    }
}
