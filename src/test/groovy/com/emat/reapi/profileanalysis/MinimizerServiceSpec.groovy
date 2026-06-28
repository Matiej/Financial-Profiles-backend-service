package com.emat.reapi.profileanalysis

import com.emat.reapi.profileanalysis.domain.PayloadMode
import com.emat.reapi.profiler.domain.ProfileCategory
import com.emat.reapi.profiler.domain.ProfiledCategoryClientStatements
import com.emat.reapi.profiler.domain.ProfiledClientAnswerDetails
import com.emat.reapi.profiler.domain.ProfiledStatement
import com.emat.reapi.statement.domain.StatementType
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

/**
 * Pure-logic unit tests for {@code MinimizerServiceImpl}. No Spring, no DB.
 *
 * This logic feeds the AI payload. The end-to-end flow is covered by
 * {@code ProfileAnalysisAiFlowSpec}; here we pin the minimizer rules and edge cases
 * in isolation.
 *
 * Pinned behaviors:
 *  - per-type caps by mode: FULL = all, MINIMAL = 2, ENRICHED = 4 for the worst K=2
 *    categories (by balanceIndex) and 2 for the rest.
 *  - "worst" = lowest balanceIndex; tie broken by higher activity (limiting+supporting).
 *  - evidence filtering: only status==TRUE, matching type, non-blank, trimmed, distinct.
 *  - balanceIndex = supporting / (limiting + supporting), or 0.5 when total is 0.
 */
class MinimizerServiceSpec extends Specification {

    def service = new MinimizerServiceImpl()

    // ---- fixtures ----

    private static ProfiledStatement stmt(String desc, StatementType type, Boolean status) {
        new ProfiledStatement(desc, type, status)
    }

    private static ProfiledCategoryClientStatements category(String name, int totalLimiting, int totalSupporting,
                                                             List<ProfiledStatement> statements) {
        new ProfiledCategoryClientStatements(new ProfileCategory(name, name + " label"), totalLimiting, totalSupporting, statements)
    }

    private static ProfiledClientAnswerDetails details(List<ProfiledCategoryClientStatements> categories) {
        new ProfiledClientAnswerDetails("Anna", "client-1", "sub-1", Instant.parse("2025-06-01T10:00:00Z"), "Test 1", categories)
    }

    private static List<ProfiledStatement> nLimiting(int n) {
        (1..n).collect { stmt("lim-" + it, StatementType.LIMITING, true) }
    }

    // ---- mode caps ----

    def "FULL mode returns all matching evidence"() {
        given:
        def cat = category("A", 3, 2, [
                stmt("l1", StatementType.LIMITING, true),
                stmt("l2", StatementType.LIMITING, true),
                stmt("l3", StatementType.LIMITING, true),
                stmt("s1", StatementType.SUPPORTING, true),
                stmt("s2", StatementType.SUPPORTING, true)
        ])

        when:
        def result = service.minimize(details([cat]), PayloadMode.FULL).block()

        then:
        def block = result.categories[0]
        block.limitingEvidence == ["l1", "l2", "l3"]
        block.supportingEvidence == ["s1", "s2"]
    }

    def "MINIMAL mode caps each type at 2"() {
        given:
        def cat = category("A", 4, 4, nLimiting(4) + [
                stmt("s1", StatementType.SUPPORTING, true),
                stmt("s2", StatementType.SUPPORTING, true),
                stmt("s3", StatementType.SUPPORTING, true)
        ])

        when:
        def result = service.minimize(details([cat]), PayloadMode.MINIMAL).block()

        then:
        def block = result.categories[0]
        block.limitingEvidence == ["lim-1", "lim-2"]
        block.supportingEvidence == ["s1", "s2"]
    }

    def "ENRICHED mode gives the worst 2 categories 4 evidence and the rest 2"() {
        given: "balances: A=0.0 (worst), B=0.25, C=0.75 (best); each has 5 limiting statements"
        def a = category("A", 5, 0, nLimiting(5))
        def b = category("B", 3, 1, nLimiting(5))
        def c = category("C", 1, 3, nLimiting(5))

        when:
        def result = service.minimize(details([a, b, c]), PayloadMode.ENRICHED).block()

        then: "A and B enriched to 4, C stays at 2"
        result.categories.find { it.categoryId == "A" }.limitingEvidence.size() == 4
        result.categories.find { it.categoryId == "B" }.limitingEvidence.size() == 4
        result.categories.find { it.categoryId == "C" }.limitingEvidence.size() == 2
    }

    def "ENRICHED tie-break: equal balance picks the category with higher activity"() {
        given: "R balance 0.4 (worst); P and Q both 0.5 but P has more activity"
        def r = category("R", 3, 2, nLimiting(5))   // balance 0.4
        def p = category("P", 2, 2, nLimiting(5))   // balance 0.5, activity 4
        def q = category("Q", 1, 1, nLimiting(5))   // balance 0.5, activity 2

        when:
        def result = service.minimize(details([p, q, r]), PayloadMode.ENRICHED).block()

        then: "worst 2 = R then P (higher activity wins the tie); Q stays minimal"
        result.categories.find { it.categoryId == "R" }.limitingEvidence.size() == 4
        result.categories.find { it.categoryId == "P" }.limitingEvidence.size() == 4
        result.categories.find { it.categoryId == "Q" }.limitingEvidence.size() == 2
    }

    // ---- evidence filtering ----

    def "evidence keeps only status TRUE of the matching type, trimmed, distinct and non-blank"() {
        given:
        def cat = category("A", 9, 9, [
                stmt("keep1", StatementType.LIMITING, true),
                stmt("skip-false", StatementType.LIMITING, false),
                stmt("supporting-one", StatementType.SUPPORTING, true),
                stmt(null, StatementType.LIMITING, true),
                stmt("   ", StatementType.LIMITING, true),
                stmt("keep1", StatementType.LIMITING, true),       // duplicate
                stmt("  keep3  ", StatementType.LIMITING, true),    // trimmed
                stmt("nullStatus", StatementType.LIMITING, null)
        ])

        when:
        def result = service.minimize(details([cat]), PayloadMode.FULL).block()

        then:
        def block = result.categories[0]
        block.limitingEvidence == ["keep1", "keep3"]
        block.supportingEvidence == ["supporting-one"]
    }

    @Unroll
    def "balanceIndex = supporting/(limiting+supporting): #limiting/#supporting -> #expected"() {
        given:
        def cat = category("A", limiting, supporting, [])

        when:
        def result = service.minimize(details([cat]), PayloadMode.MINIMAL).block()

        then:
        result.categories[0].balanceIndex == expected

        where:
        limiting | supporting || expected
        3        | 1          || 0.25d
        1        | 3          || 0.75d
        2        | 2          || 0.5d
        0        | 0          || 0.5d
        5        | 0          || 0.0d
    }

    // ---- top-level mapping & edge cases ----

    def "maps top-level payload fields from the source"() {
        when:
        def result = service.minimize(details([category("A", 1, 1, [])]), PayloadMode.MINIMAL).block()

        then:
        result.clientName == "Anna"
        result.clientId == "client-1"
        result.submissionId == "sub-1"
        result.testName == "Test 1"
        result.submissionDate == Instant.parse("2025-06-01T10:00:00Z")
    }

    def "returns an empty payload when the category list is null"() {
        when:
        def result = service.minimize(details(null), PayloadMode.MINIMAL).block()

        then:
        result.categories.isEmpty()
        result.clientName == "Anna"
        result.submissionId == "sub-1"
    }

    def "a category with fewer statements than the cap returns what it has"() {
        given:
        def cat = category("A", 1, 0, [stmt("only-one", StatementType.LIMITING, true)])

        when:
        def result = service.minimize(details([cat]), PayloadMode.MINIMAL).block()

        then:
        result.categories[0].limitingEvidence == ["only-one"]
        result.categories[0].supportingEvidence.isEmpty()
    }
}
