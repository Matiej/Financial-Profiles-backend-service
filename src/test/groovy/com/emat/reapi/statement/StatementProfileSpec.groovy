package com.emat.reapi.statement

import com.emat.reapi.statement.domain.StatementProfile
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Pure-logic unit tests for the {@code StatementProfile} enum.
 *
 * Pins the threshold boundaries of {@code computeLabel} and the division-by-zero guard
 * of {@code computePercent} — the exact edges are not all covered by ProfilerControllerSpec.
 */
class StatementProfileSpec extends Specification {

    @Unroll
    def "computePercent(#score, #answers) == #expected"() {
        expect:
        StatementProfile.computePercent(score, answers) == expected

        where:
        score | answers || expected
        0     | 5       || 0.0d
        10    | 5       || 100.0d   // 10 / (5*2) * 100
        5     | 5       || 50.0d
        0     | 0       || 0.0d     // guard: division by zero
        -2    | 2       || -50.0d   // negative scores allowed
    }

    @Unroll
    def "computeLabel(#percent) on PROFIL_1 -> #expected"() {
        expect:
        StatementProfile.PROFIL_1.computeLabel(percent) == expected

        where:
        percent || expected
        -10.0d  || "Strażniczka Braku (blokada)"            // <= 0
        0.0d    || "Strażniczka Braku (blokada)"            // boundary <= 0
        0.1d    || "Strażniczka Braku (strefa przejściowa)" // just above 0
        67.9d   || "Strażniczka Braku (strefa przejściowa)" // just below 68
        68.0d   || "Zakorzeniona w Obfitości (zasoby)"      // boundary >= 68
        100.0d  || "Zakorzeniona w Obfitości (zasoby)"
    }

    def "getPlName returns the base profile name"() {
        expect:
        StatementProfile.PROFIL_1.plName == "Strażniczka Braku"
        StatementProfile.PROFIL_8.plName == "Idealistka Skromności"
    }

    def "all eight profiles are defined"() {
        expect:
        StatementProfile.values().length == 8
    }
}
