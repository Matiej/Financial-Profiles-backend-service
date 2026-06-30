package com.emat.reapi.statement.domain;

import org.springframework.stereotype.Component;

/**
 * Resolves the scoring-zone label for a given percent using global thresholds.
 * Replaced the former per-enum {@code StatementProfile.computeLabel}; labels now come from
 * a {@link ProfileLabels} triple (live profile or historical snapshot).
 */
@Component
public class ScoringLabelResolver {
    private final ScoringThresholds thresholds;

    public ScoringLabelResolver(ScoringThresholds thresholds) {
        this.thresholds = thresholds;
    }

    public String computeLabel(double percent, ProfileLabels labels) {
        if (percent <= thresholds.blocking()) return labels.blocking();
        if (percent < thresholds.resources()) return labels.transitional();
        return labels.resources();
    }
}
