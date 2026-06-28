package com.emat.reapi.statement

import com.emat.reapi.statement.domain.ProfileLabels
import com.emat.reapi.statement.domain.ScoringLabelResolver
import com.emat.reapi.statement.domain.ScoringThresholds
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Pure-logic unit tests for the global ScoringLabelResolver.
 *
 * Pins the threshold boundaries (blocking <= 0, resources >= 68) that previously
 * lived in StatementProfile.computeLabel, now driven by configurable thresholds.
 */
class ScoringLabelResolverSpec extends Specification {

    def labels = new ProfileLabels("blocking", "transitional", "resources")
    def resolver = new ScoringLabelResolver(new ScoringThresholds(0.0d, 68.0d))

    @Unroll
    def "computeLabel(#percent) -> #expected"() {
        expect:
        resolver.computeLabel(percent, labels) == expected

        where:
        percent || expected
        -10.0d  || "blocking"      // <= 0
        0.0d    || "blocking"      // boundary <= 0
        0.1d    || "transitional"  // just above 0
        67.9d   || "transitional"  // just below 68
        68.0d   || "resources"     // boundary >= 68
        100.0d  || "resources"
    }

    def "honours custom thresholds from config"() {
        given:
        def custom = new ScoringLabelResolver(new ScoringThresholds(10.0d, 50.0d))

        expect:
        custom.computeLabel(10.0d, labels) == "blocking"       // boundary <= 10
        custom.computeLabel(10.1d, labels) == "transitional"
        custom.computeLabel(49.9d, labels) == "transitional"
        custom.computeLabel(50.0d, labels) == "resources"      // boundary >= 50
    }
}
