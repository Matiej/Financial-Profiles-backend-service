package com.emat.reapi.clienttest

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.fptest.infra.FpTestDocument
import com.emat.reapi.fptest.infra.FpTestStatementDocument
import com.emat.reapi.statement.domain.StatementType
import com.emat.reapi.statement.domain.StatementTypeDefinition
import com.emat.reapi.statement.infra.StatementDefinitionDocument
import com.emat.reapi.submission.domain.SubmissionStatus
import com.emat.reapi.submission.infra.SubmissionDocument
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Shared

import java.time.Instant

import static com.github.tomakehurst.wiremock.client.WireMock.*

/**
 * Characterization tests pinning the CURRENT behavior of
 * {@code ClientTestController} before any refactor.
 *
 * Notable behaviors pinned here:
 *  - Both endpoints ({@code GET /{token}}, {@code POST /}) are public (permitAll) — no JWT required.
 *  - {@code GET /{token}} finds only OPEN, non-expired submissions; DONE or expired tokens yield 404.
 *  - {@code POST /} validates publicToken match, answer key existence and answer count vs test size
 *    before persisting; on success the submission transitions to DONE.
 *  - n8n notification ({@code POST /score-test/email}) is fire-and-forget — errors are swallowed;
 *    WireMock stubs it to assert it was called without blocking the test on n8n availability.
 */
class ClientTestControllerSpec extends BaseIntegrationSpec {

    private static final ObjectMapper MAPPER = new ObjectMapper()

    @Shared
    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort())

    static {
        wireMock.start()
    }

    @DynamicPropertySource
    static void wireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("app.client.n8n.base-url") { "http://localhost:${wireMock.port()}" }
    }

    def setup() {
        wireMock.resetAll()
    }

    def "should return 200 with questions for a valid public token"() {
        given:
        seedDefinition("profil_1", "p1_q1")
        seedDefinition("profil_1", "p1_q2")
        seedFpTest("fpt_get-1", ["p1_q1", "p1_q2"])
        seedSubmission("sub_get-1", "fpt_get-1", "pt_valid")

        when:
        def result = webTestClient.get().uri("/api/client/test/pt_valid").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.publicToken == "pt_valid"
        result.submissionId == "sub_get-1"
        result.testName == "Test fpt_get-1"
        result.clientQuestions.size() == 2
        result.clientQuestions[0].statementKey != null
        result.clientQuestions[0].supportingStatement != null
        result.clientQuestions[0].limitingStatement != null
    }

    def "should return 404 for an unknown public token"() {
        when:
        def response = webTestClient.get().uri("/api/client/test/pt_missing").exchange()

        then:
        response.expectStatus().isNotFound()
    }

    def "should return 404 for a DONE submission token"() {
        given:
        seedDefinition("profil_1", "p1_q1")
        seedFpTest("fpt_done", ["p1_q1"])
        seedSubmission("sub_done", "fpt_done", "pt_done-token", SubmissionStatus.DONE)

        when:
        def response = webTestClient.get().uri("/api/client/test/pt_done-token").exchange()

        then:
        response.expectStatus().isNotFound()
    }

    def "should return 404 for an expired submission token"() {
        given:
        seedDefinition("profil_1", "p1_q1")
        seedFpTest("fpt_expired", ["p1_q1"])
        seedSubmission("sub_expired", "fpt_expired", "pt_expired-token",
                SubmissionStatus.OPEN, Instant.now().minusSeconds(60))

        when:
        def response = webTestClient.get().uri("/api/client/test/pt_expired-token").exchange()

        then:
        response.expectStatus().isNotFound()
    }

    def "should return 404 (not 401) for a missing token since the endpoint is public"() {
        when:
        def response = webTestClient.get().uri("/api/client/test/pt_no-auth").exchange()

        then:
        response.expectStatus().isNotFound()
    }

    def "should save answers, close submission to DONE and notify n8n"() {
        given:
        stubN8nOk()
        seedDefinition("profil_1", "p1_q1")
        seedDefinition("profil_1", "p1_q2")
        seedFpTest("fpt_post-1", ["p1_q1", "p1_q2"])
        seedSubmission("sub_post-1", "fpt_post-1", "pt_post-token")

        when:
        webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : "sub_post-1",
                        publicToken      : "pt_post-token",
                        clientTestAnswers: [
                                [statementKey: "p1_q1", scoring: 2],
                                [statementKey: "p1_q2", scoring: -1]
                        ]
                ])
                .exchange()
                .expectStatus().isCreated()

        then: "submission closed"
        def sub = mongoTemplate.findAll(SubmissionDocument).collectList().block()
        sub.find { it.submissionId == "sub_post-1" }.status == SubmissionStatus.DONE

        and: "n8n notified exactly once"
        wireMock.verify(1, postRequestedFor(urlEqualTo("/score-test/email")))
    }

    def "should send n8n the v2 computed-scoring payload, not raw answers (jira 2026-005)"() {
        given:
        stubN8nOk()
        seedDefinition("profil_1", "p1_q1")
        seedDefinition("profil_1", "p1_q2")
        seedFpTest("fpt_n8n-shape", ["p1_q1", "p1_q2"])
        seedSubmission("sub_n8n-shape", "fpt_n8n-shape", "pt_n8n-shape-token")

        when:
        webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : "sub_n8n-shape",
                        publicToken      : "pt_n8n-shape-token",
                        clientTestAnswers: [
                                [statementKey: "p1_q1", scoring: 2],
                                [statementKey: "p1_q2", scoring: -1]
                        ]
                ])
                .exchange()
                .expectStatus().isCreated()

        then: "n8n received exactly one call, carrying the computed-scoring (v2) shape"
        def sent = wireMock.findAll(postRequestedFor(urlEqualTo("/score-test/email")))
        sent.size() == 1
        def body = MAPPER.readValue(sent[0].bodyAsString, Map)

        and: "envelope: recipient + identifiers, matching ClientScoreTestNotification"
        body.clientEmail == "jan@example.com"
        body.testSubmissionPublicId != null
        body.submissionId == "sub_n8n-shape"
        body.testId == "fpt_n8n-shape"

        and: "overallSummary present (computed, not raw answers)"
        body.overallSummary != null
        body.overallSummary.totalAnswers == 2

        and: "profiles[] grouped by profile, with computed label + per-statement severity"
        body.profiles instanceof List
        body.profiles.size() == 1
        body.profiles[0].profileId == "profil_1"
        body.profiles[0].computedLabel != null
        body.profiles[0].answersBySeverity.size() == 2
        body.profiles[0].answersBySeverity.every { it.statementKey && it.limitingDescription && it.supportingDescription }

        and: "the old v1 raw-answers field is gone"
        !body.containsKey("clientTestAnswerNotificationList")
    }

    def "should return 400 when publicToken does not match submission"() {
        given:
        stubN8nOk()
        seedDefinition("profil_1", "p1_q1")
        seedFpTest("fpt_token-mismatch", ["p1_q1"])
        seedSubmission("sub_token-mismatch", "fpt_token-mismatch", "pt_real-token")

        when:
        def response = webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : "sub_token-mismatch",
                        publicToken      : "pt_WRONG-token",
                        clientTestAnswers: [[statementKey: "p1_q1", scoring: 1]]
                ])
                .exchange()

        then:
        response.expectStatus().isBadRequest()
    }

    def "should return 400 when number of answers does not match test size"() {
        given:
        stubN8nOk()
        seedDefinition("profil_1", "p1_q1")
        seedDefinition("profil_1", "p1_q2")
        seedFpTest("fpt_size-mismatch", ["p1_q1", "p1_q2"])
        seedSubmission("sub_size-mismatch", "fpt_size-mismatch", "pt_size-token")

        when: "test has 2 statements but only 1 answer provided"
        def response = webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : "sub_size-mismatch",
                        publicToken      : "pt_size-token",
                        clientTestAnswers: [[statementKey: "p1_q1", scoring: 1]]
                ])
                .exchange()

        then:
        response.expectStatus().isBadRequest()
    }

    def "should return 404 when answer contains an unknown statementKey"() {
        given:
        stubN8nOk()
        seedDefinition("profil_1", "p1_q1")
        seedFpTest("fpt_bad-key", ["p1_q1"])
        seedSubmission("sub_bad-key", "fpt_bad-key", "pt_bad-key-token")

        when:
        def response = webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : "sub_bad-key",
                        publicToken      : "pt_bad-key-token",
                        clientTestAnswers: [[statementKey: "p1_NONEXISTENT", scoring: 1]]
                ])
                .exchange()

        then:
        response.expectStatus().isNotFound()
    }

    def "should return 404 when submissionId does not exist"() {
        when:
        def response = webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : "sub_missing",
                        publicToken      : "pt_any",
                        clientTestAnswers: [[statementKey: "p1_q1", scoring: 1]]
                ])
                .exchange()

        then:
        response.expectStatus().isNotFound()
    }

    def "should return 400 VALIDATION_ERROR for blank submissionId"() {
        when:
        def result = webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : "",
                        publicToken      : "pt_token",
                        clientTestAnswers: [[statementKey: "p1_q1", scoring: 1]]
                ])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
    }

    def "should accept a public POST without an auth header"() {
        given:
        stubN8nOk()
        seedDefinition("profil_1", "p1_q1")
        seedFpTest("fpt_public", ["p1_q1"])
        seedSubmission("sub_public", "fpt_public", "pt_public-token")

        when:
        def response = webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : "sub_public",
                        publicToken      : "pt_public-token",
                        clientTestAnswers: [[statementKey: "p1_q1", scoring: 2]]
                ])
                .exchange()

        then:
        response.expectStatus().isCreated()
    }

    def "should complete full E2E flow: GET test, POST answers, verify submission DONE"() {
        given:
        stubN8nOk()
        seedDefinition("profil_1", "p1_q1")
        seedDefinition("profil_1", "p1_q2")
        seedFpTest("fpt_e2e", ["p1_q1", "p1_q2"])
        seedSubmission("sub_e2e", "fpt_e2e", "pt_e2e-token")

        when: "client fetches the test"
        def test = webTestClient.get().uri("/api/client/test/pt_e2e-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "test contains 2 shuffled questions"
        test.clientQuestions.size() == 2
        test.publicToken == "pt_e2e-token"
        test.submissionId == "sub_e2e"

        when: "client submits answers using the returned keys (order may differ due to shuffle)"
        def answers = (test.clientQuestions as List).collect { q -> [statementKey: q.statementKey, scoring: 1] }
        webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : "sub_e2e",
                        publicToken      : "pt_e2e-token",
                        clientTestAnswers: answers
                ])
                .exchange()
                .expectStatus().isCreated()

        then: "submission is now DONE"
        def submissions = mongoTemplate.findAll(SubmissionDocument).collectList().block()
        submissions.find { it.submissionId == "sub_e2e" }.status == SubmissionStatus.DONE

        and: "n8n was notified exactly once"
        wireMock.verify(1, postRequestedFor(urlEqualTo("/score-test/email")))
    }

    private StatementDefinitionDocument seedDefinition(String profileId, String statementKey) {
        def now = Instant.now()
        def doc = new StatementDefinitionDocument(
                statementKey: statementKey,
                profileId: profileId,
                statementTypeDefinitions: [
                        new StatementTypeDefinition(StatementType.LIMITING, "ograniczajace " + statementKey),
                        new StatementTypeDefinition(StatementType.SUPPORTING, "wspierajace " + statementKey)
                ],
                isDeleted: false,
                createdAt: now,
                updatedAt: now
        )
        mongoTemplate.insert(doc).block()
    }

    private FpTestDocument seedFpTest(String testId, List<String> statementKeys) {
        def doc = new FpTestDocument()
        doc.testId = testId
        doc.testName = "Test " + testId
        doc.fpTestStatementDocuments = statementKeys.collect { key ->
            new FpTestStatementDocument(key, "desc-" + key, "Strażniczka Braku", "profil_1")
        }
        mongoTemplate.insert(doc).block()
    }

    private SubmissionDocument seedSubmission(String submissionId, String testId, String publicToken,
                                              SubmissionStatus status = SubmissionStatus.OPEN,
                                              Instant expireAt = Instant.now().plusSeconds(7 * 24 * 60 * 60)) {
        def doc = new SubmissionDocument()
        doc.submissionId = submissionId
        doc.testId = testId
        doc.publicToken = publicToken
        doc.clientId = "client-1"
        doc.clientName = "Jan Kowalski"
        doc.clientEmail = "jan@example.com"
        doc.orderId = "order-1_" + UUID.randomUUID()
        doc.status = status
        doc.durationDays = 7
        doc.expireAt = expireAt
        mongoTemplate.insert(doc).block()
    }

    private static void stubN8nOk() {
        wireMock.stubFor(post(urlEqualTo("/score-test/email")).willReturn(ok()))
    }
}
