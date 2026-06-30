package com.emat.reapi.statement

import com.emat.reapi.statement.domain.ScoringMath
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Pure-logic unit tests for ScoringMath.computePercent,
 * including the division-by-zero guard. Extracted from the former StatementProfile enum logic.
 */
class ScoringMathSpec extends Specification {

    @Unroll
    def "computePercent(#score, #answers) == #expected"() {
        expect:
        ScoringMath.computePercent(score, answers) == expected

        where:
        score | answers || expected
        0     | 5       || 0.0d
        10    | 5       || 100.0d   // 10 / (5*2) * 100
        5     | 5       || 50.0d
        0     | 0       || 0.0d     // guard: division by zero
        -2    | 2       || -50.0d   // negative scores allowed
    }
}
