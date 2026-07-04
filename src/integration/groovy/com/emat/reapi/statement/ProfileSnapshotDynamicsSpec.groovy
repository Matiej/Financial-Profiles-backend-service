package com.emat.reapi.statement

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.clienttest.infra.ClientTestAnswerDocument
import com.emat.reapi.clienttest.infra.ClientTestDocument
import com.emat.reapi.fptest.infra.FpTestDocument
import com.emat.reapi.fptest.infra.FpTestStatementDocument
import com.emat.reapi.statement.domain.ProfileSnapshot
import com.emat.reapi.statement.domain.StatementType
import com.emat.reapi.statement.domain.StatementTypeDefinition
import com.emat.reapi.statement.infra.ProfileDocument
import com.emat.reapi.statement.infra.StatementDefinitionDocument
import com.emat.reapi.submission.domain.SubmissionStatus
import com.emat.reapi.submission.infra.SubmissionDocument
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Shared

import java.time.Instant

import static com.github.tomakehurst.wiremock.client.WireMock.*

/**
 * F9 — profile snapshot + scoring dynamics.
 *
 * Proves the live-vs-snapshot rule end to end:
 *  - {@code POST /api/client/test} freezes a {@link ProfileSnapshot} per referenced profile
 *    into the {@code ClientTestDocument} at save time.
 *  - Editing or soft-deleting the live profile afterwards never changes the historical scoring
 *    label: {@code GET /api/profiler/{tspi}/scoring} keeps reading the frozen snapshot.
 *
 * Global thresholds (app.scoring.thresholds): blocking <= 0, resources >= 68. Two answers
 * scoring 2 each → 100% → the profile's resources label.
 */
class ProfileSnapshotDynamicsSpec extends BaseIntegrationSpec {

    private static final String ORIG_PL = "Profil ORIG"
    private static final String ORIG_BLOCKING = "Blokada ORIG"
    private static final String ORIG_TRANSITIONAL = "Przejściowa ORIG"
    private static final String ORIG_RESOURCES = "Zasoby ORIG"

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
        stubN8nOk()
    }

    def "POST /api/client/test freezes the live profile labels into the ClientTestDocument snapshot"() {
        given: "a live profile plus a test/submission referencing it"
        seedProfile("profil_1")
        seedDefinition("profil_1", "p1_q1")
        seedDefinition("profil_1", "p1_q2")
        seedFpTest("fpt_dyn", ["p1_q1", "p1_q2"])
        seedSubmission("sub_dyn", "fpt_dyn", "pt_dyn")

        when: "the client submits answers"
        submitAnswers("sub_dyn", "pt_dyn", ["p1_q1", "p1_q2"])

        then: "a snapshot of the profile's labels is frozen into the completed test"
        def saved = mongoTemplate.findAll(ClientTestDocument).collectList().block()
        saved.size() == 1
        def snapshot = saved[0].profileSnapshots["profil_1"]
        snapshot != null
        snapshot.plName() == ORIG_PL
        snapshot.blockingName() == ORIG_BLOCKING
        snapshot.transitionalName() == ORIG_TRANSITIONAL
        snapshot.resourcesName() == ORIG_RESOURCES
    }

    def "editing the live profile after submission does not change the historical scoring label"() {
        given: "a completed submission that froze the ORIG labels"
        seedProfile("profil_1")
        seedDefinition("profil_1", "p1_q1")
        seedDefinition("profil_1", "p1_q2")
        seedFpTest("fpt_dyn", ["p1_q1", "p1_q2"])
        seedSubmission("sub_dyn", "fpt_dyn", "pt_dyn")
        submitAnswers("sub_dyn", "pt_dyn", ["p1_q1", "p1_q2"])
        def tspi = mongoTemplate.findAll(ClientTestDocument).collectList().block()[0].testSubmissionPublicId

        when: "an admin edits the live profile's resources label afterwards"
        authenticatedPut("/api/definition/profile/profil_1", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        plName          : ORIG_PL,
                        blockingName    : ORIG_BLOCKING,
                        transitionalName: ORIG_TRANSITIONAL,
                        resourcesName   : "Zasoby CHANGED",
                        order           : 1
                ])
                .exchange()
                .expectStatus().isOk()

        then: "scoring still reports the label frozen at save time, not the edited one"
        def result = authenticatedGet("/api/profiler/${tspi}/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody
        result.profiles.size() == 1
        result.profiles[0].scorePercent == 100.0
        result.profiles[0].computedLabel == ORIG_RESOURCES
    }

    def "soft-deleting the profile after submission does not break or change the historical scoring"() {
        given: "a completed test with a frozen snapshot and a live profile with no active definitions"
        seedProfile("profil_1")
        seedClientTest("tsb_del", "sub_del", "fpt_del", [
                answer("p1_q1", "profil_1", 2),
                answer("p1_q2", "profil_1", 2)
        ])

        when: "the live profile is soft-deleted (allowed: nothing active references it)"
        authenticatedDelete("/api/definition/profile/profil_1", "BUSINESS_ADMIN").exchange()
                .expectStatus().isAccepted()

        then: "the profile is gone from the live side"
        mongoTemplate.findById("profil_1", ProfileDocument).block().isDeleted

        and: "scoring still resolves the frozen label"
        def result = authenticatedGet("/api/profiler/tsb_del/scoring", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody
        result.profiles[0].computedLabel == ORIG_RESOURCES
        result.profiles[0].scorePercent == 100.0
    }

    private void submitAnswers(String submissionId, String publicToken, List<String> statementKeys) {
        webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        submissionId     : submissionId,
                        publicToken      : publicToken,
                        clientTestAnswers: statementKeys.collect { [statementKey: it, scoring: 2] }
                ])
                .exchange()
                .expectStatus().isCreated()
    }

    private ProfileDocument seedProfile(String id) {
        def now = Instant.now()
        def doc = ProfileDocument.builder()
                .id(id)
                .plName(ORIG_PL)
                .blockingName(ORIG_BLOCKING)
                .transitionalName(ORIG_TRANSITIONAL)
                .resourcesName(ORIG_RESOURCES)
                .order(1)
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build()
        mongoTemplate.insert(doc).block()
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

    private void seedFpTest(String testId, List<String> statementKeys) {
        def doc = new FpTestDocument()
        doc.testId = testId
        doc.testName = "Test " + testId
        doc.fpTestStatementDocuments = statementKeys.collect {
                new FpTestStatementDocument(it, "desc " + it, "PROFIL", "profil_1")
        }
        mongoTemplate.insert(doc).block()
    }

    private void seedSubmission(String submissionId, String testId, String publicToken) {
        def doc = new SubmissionDocument()
        doc.submissionId = submissionId
        doc.testId = testId
        doc.publicToken = publicToken
        doc.clientId = "client-1"
        doc.clientName = "Anna Testowa"
        doc.clientEmail = "anna@example.com"
        doc.orderId = "order_" + UUID.randomUUID()
        doc.status = SubmissionStatus.OPEN
        doc.durationDays = 7
        doc.expireAt = Instant.now().plusSeconds(7 * 24 * 60 * 60)
        mongoTemplate.insert(doc).block()
    }

    // Seeds a completed client test with a frozen snapshot carrying the ORIG labels,
    // used by the delete case to avoid fighting the PROFILE_IN_USE guard.
    private void seedClientTest(String tspi, String submissionId, String testId, List<ClientTestAnswerDocument> answers) {
        def snapshot = new ProfileSnapshot("profil_1", ORIG_PL, ORIG_BLOCKING, ORIG_TRANSITIONAL, ORIG_RESOURCES)
        def doc = new ClientTestDocument()
        doc.testSubmissionPublicId = tspi
        doc.submissionId = submissionId
        doc.clientId = "client-1"
        doc.clientName = "Anna Testowa"
        doc.clientEmail = "anna@example.com"
        doc.testId = testId
        doc.testName = "Test " + testId
        doc.submissionDate = Instant.now()
        doc.publicToken = "pt_" + tspi
        doc.answers = answers
        doc.profileSnapshots = answers.collect { it.profileId }.unique().collectEntries { [(it): snapshot] }
        mongoTemplate.insert(doc).block()
    }

    private static ClientTestAnswerDocument answer(String questionKey, String profileId, int scoring) {
        new ClientTestAnswerDocument(
                questionKey,
                profileId,
                "ograniczajace " + questionKey,
                "wspierajace " + questionKey,
                scoring
        )
    }

    private static void stubN8nOk() {
        wireMock.stubFor(post(urlEqualTo("/score-test/email")).willReturn(ok()))
    }
}
