package com.emat.reapi.statement.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Global label thresholds shared by all profiles (not stored per-profile).
 * {@code blocking}: percent at/below this maps to the blocking label.
 * {@code resources}: percent at/above this maps to the resources label.
 * Anything in between is transitional.
 */
@ConfigurationProperties(prefix = "app.scoring.thresholds")
public record ScoringThresholds(
        double blocking,
        double resources
) {
}
