package com.emat.reapi.profiler

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.clienttest.infra.ClientTestAnswerDocument
import com.emat.reapi.clienttest.infra.ClientTestDocument
import com.emat.reapi.statement.domain.StatementProfile
import spock.lang.Unroll

import java.time.Instant

/**
 * Characterization tests pinning the CURRENT behavior of {@code ProfilerController}
 * before any refactor.
 *
 * Notable behaviors pinned here:
 *  - {@code GET /api/profiler} and {@code GET /api/profiler/{submissionId}} are
 *    Tally-based (read from the {@code clienttalytest} domain). They are included
 *    here only as structural smoke tests — both endpoints are planned for removal
 *    with the Tally refactor.
 *  - {@code GET /api/profiler/scoring} and {@code GET /api/profiler/{id}/scoring}
 *    read from {@code ClientTestDocument} (the FpTest-based flow) and are the
 *    primary targets of these tests.
 *  - Profile blocks are sorted ascending by {@code totalScore}; the block with the
 *    lowest score appears first.
 *  - {@code scorePercent} formula: {@code totalScore / (totalAnswers * 2) * 100}.
 *    Label thresholds: {@code <=0} → blokada, {@code <68} → strefa przejściowa,
 *    {@code >=68} → zasoby.
 *
 * Seed used in scoring tests (via the local {@code seedClientTest}/{@code answer} helpers):
 *  - 2 PROFIL_1 answers scoring 2 each  → totalScore=4, percent=100.0 → zasoby
 *  - 2 PROFIL_2 answers scoring 0 each  → totalScore=0, percent=0.0   → blokada
 */
class ProfilerControllerSpec extends BaseIntegrationSpec {

    // ---- seed helpers ----

    /**
     * Seeds a completed client test (the record created after a client submits answers).
     * This is the document read by the ProfilerController scoring endpoints.
     */
    private ClientTestDocument seedClientTest(String testSubmissionPublicId,
                                              String submissionId,
                                              String testId,
                                              List<ClientTestAnswerDocument> answers) {
        def doc = new ClientTestDocument()
        doc.testSubmissionPublicId = testSubmissionPublicId
        doc.submissionId = submissionId
        doc.clientId = "client-1"
        doc.clientName = "Anna Testowa"
        doc.clientEmail = "anna@example.com"
        doc.testId = testId
        doc.testName = "Test " + testId
        doc.submissionDate = Instant.now()
        doc.publicToken = "pt_" + testSubmissionPublicId
        doc.answers = answers
        mongoTemplate.insert(doc).block()
        return doc
    }

    private static ClientTestAnswerDocument answer(String questionKey, StatementProfile category, int scoring) {
        new ClientTestAnswerDocument(
                questionKey,
                category,
                "ograniczajace " + questionKey,
                "wspierajace " + questionKey,
                scoring
        )
    }

    // ---- GET /api/profiler — Tally-based (smoke only, planned for removal) ----

    def "should return 200 with an empty list when no Tally data exists"() {
        when:
        def result = authenticatedGet("/api/profiler", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then:
        result.isEmpty()
    }

    def "should return 404 for an unknown Tally submissionId"() {
        when:
        def response = authenticatedGet("/api/profiler/sub_tally_missing", "BUSINESS_ADMIN").exchange()

        then:
        response.expectStatus().isNotFound()
    }

    // ---- GET /api/profiler/scoring — short list of all ClientTest results ----

    def "should return 200 with an empty scoring list when no client tests exist"() {
        when:
        def result = authenticatedGet("/api/profiler/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then:
        result.isEmpty()
    }

    def "should return one short scoring entry per seeded client test"() {
        given:
        seedClientTest("tspi_short-1", "sub_short-1", "fpt_short", [
                answer("p1_q1", StatementProfile.PROFIL_1, 2),
                answer("p2_q1", StatementProfile.PROFIL_2, 0)
        ])
        seedClientTest("tspi_short-2", "sub_short-2", "fpt_short", [
                answer("p1_q1", StatementProfile.PROFIL_1, 1)
        ])

        when:
        def result = authenticatedGet("/api/profiler/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then:
        result.size() == 2
    }

    def "should return correct totals in a short scoring entry"() {
        given:
        seedClientTest("tspi_totals", "sub_totals", "fpt_totals", [
                answer("p1_q1", StatementProfile.PROFIL_1, 2),
                answer("p1_q2", StatementProfile.PROFIL_1, 2),
                answer("p2_q1", StatementProfile.PROFIL_2, 0),
                answer("p2_q2", StatementProfile.PROFIL_2, 0)
        ])

        when:
        def result = authenticatedGet("/api/profiler/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then:
        result[0].testSubmissionPublicId == "tspi_totals"
        result[0].clientName == "Anna Testowa"
        result[0].totalScoring == 4
        result[0].numberOfStatements == 4
    }

    // ---- GET /api/profiler/{id}/scoring — detailed scoring for one ClientTest ----

    def "should return 404 for an unknown testSubmissionPublicId on scoring details"() {
        when:
        def response = authenticatedGet("/api/profiler/tspi_missing/scoring", "BUSINESS_ADMIN").exchange()

        then:
        response.expectStatus().isNotFound()
    }

    def "should return 200 with structured scoring details"() {
        given:
        seedClientTest("tspi_detail", "sub_detail", "fpt_detail", [
                answer("p1_q1", StatementProfile.PROFIL_1, 2),
                answer("p1_q2", StatementProfile.PROFIL_1, 2),
                answer("p2_q1", StatementProfile.PROFIL_2, 0),
                answer("p2_q2", StatementProfile.PROFIL_2, 0)
        ])

        when:
        def result = authenticatedGet("/api/profiler/tspi_detail/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "top-level fields"
        result.testSubmissionPublicId == "tspi_detail"
        result.clientName == "Anna Testowa"
        result.submissionId == "sub_detail"
        result.testId == "fpt_detail"
    }

    def "should compute the overall scoring summary correctly"() {
        given:
        seedClientTest("tspi_overall", "sub_overall", "fpt_overall", [
                answer("p1_q1", StatementProfile.PROFIL_1, 2),
                answer("p1_q2", StatementProfile.PROFIL_1, 2),
                answer("p2_q1", StatementProfile.PROFIL_2, 0),
                answer("p2_q2", StatementProfile.PROFIL_2, 0)
        ])

        when:
        def result = authenticatedGet("/api/profiler/tspi_overall/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "overall: 4 answers, total score 4, bucket '2' has 2 entries, '0' has 2"
        result.overallSummary.totalAnswers == 4
        result.overallSummary.totalScore == 4
        result.overallSummary.scoreBuckets["2"] == 2
        result.overallSummary.scoreBuckets["0"] == 2
    }

    def "should sort profile blocks ascending by totalScore"() {
        given:
        seedClientTest("tspi_sort", "sub_sort", "fpt_sort", [
                answer("p1_q1", StatementProfile.PROFIL_1, 2),
                answer("p1_q2", StatementProfile.PROFIL_1, 2),
                answer("p2_q1", StatementProfile.PROFIL_2, 0),
                answer("p2_q2", StatementProfile.PROFIL_2, 0)
        ])

        when:
        def result = authenticatedGet("/api/profiler/tspi_sort/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "PROFIL_2 (totalScore=0) before PROFIL_1 (totalScore=4)"
        result.profiles.size() == 2
        result.profiles[0].profileId == "PROFIL_2"
        result.profiles[0].totalScore == 0
        result.profiles[1].profileId == "PROFIL_1"
        result.profiles[1].totalScore == 4
    }

    def "should assign profile labels reflecting the score thresholds"() {
        given: "PROFIL_1 at 100% → zasoby; PROFIL_2 at 0% → blokada"
        seedClientTest("tspi_labels", "sub_labels", "fpt_labels", [
                answer("p1_q1", StatementProfile.PROFIL_1, 2),
                answer("p1_q2", StatementProfile.PROFIL_1, 2),
                answer("p2_q1", StatementProfile.PROFIL_2, 0),
                answer("p2_q2", StatementProfile.PROFIL_2, 0)
        ])

        when:
        def result = authenticatedGet("/api/profiler/tspi_labels/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.profiles[0].computedLabel == "Samowystarczalna Tarcza (blokada)"
        result.profiles[0].scorePercent == 0.0
        result.profiles[1].computedLabel == "Zakorzeniona w Obfitości (zasoby)"
        result.profiles[1].scorePercent == 100.0
    }

    def "should assign the transitional label when the score is below 68 percent"() {
        given: "1 answer scoring 1 out of max 2 → 50% → strefa przejściowa"
        seedClientTest("tspi_trans", "sub_trans", "fpt_trans", [
                answer("p1_q1", StatementProfile.PROFIL_1, 1)
        ])

        when:
        def result = authenticatedGet("/api/profiler/tspi_trans/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "50% < 68 → strefa przejściowa"
        result.profiles.find { it.profileId == "PROFIL_1" }.computedLabel == "Strażniczka Braku (strefa przejściowa)"
    }

    def "should sort statements within a profile block ascending by scoring"() {
        given:
        seedClientTest("tspi_stmts", "sub_stmts", "fpt_stmts", [
                answer("p1_q1", StatementProfile.PROFIL_1, 2),
                answer("p1_q2", StatementProfile.PROFIL_1, -2)
        ])

        when:
        def result = authenticatedGet("/api/profiler/tspi_stmts/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "worst answer (scoring=-2) comes first in answersBySeverity"
        result.profiles[0].answersBySeverity[0].scoring == -2
        result.profiles[0].answersBySeverity[1].scoring == 2
    }

    // ---- security ----

    def "should return 401 for GET /api/profiler/scoring without a token"() {
        when:
        def response = webTestClient.get().uri("/api/profiler/scoring").exchange()

        then:
        response.expectStatus().isUnauthorized()
    }

    @Unroll
    def "should map role #role to status #status on the profiler endpoints"() {
        when:
        def response = authenticatedGet("/api/profiler/scoring", role).exchange()

        then:
        response.expectStatus().isEqualTo(status)

        where:
        role              | status
        "CALCULATOR_USER" | 403
        "BUSINESS_ADMIN"  | 200
        "TECH_ADMIN"      | 200
    }
}
