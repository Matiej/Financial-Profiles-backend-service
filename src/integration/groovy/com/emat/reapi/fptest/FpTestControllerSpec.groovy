package com.emat.reapi.fptest

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.fptest.infra.FpTestDocument
import com.emat.reapi.fptest.infra.FpTestStatementDocument
import com.emat.reapi.statement.domain.StatementProfile
import com.emat.reapi.statement.domain.StatementType
import com.emat.reapi.statement.domain.StatementTypeDefinition
import com.emat.reapi.statement.infra.StatementDefinitionDocument
import com.emat.reapi.submission.domain.SubmissionStatus
import com.emat.reapi.submission.infra.SubmissionDocument
import org.springframework.http.MediaType
import spock.lang.Unroll

import java.time.Instant

/**
 * Characterization tests pinning the CURRENT behavior of
 * {@code FpTestController} before any refactor.
 *
 * Notable behaviors pinned here:
 *  - testId is generated as {@code fpt_<UUID>}; the response carries the resolved
 *    statement descriptions ({@code limiting + "-" + supporting}) and the profile's plName.
 *  - Unknown statementKeys on create/update yield 400 ({@code GENERIC_STATUS_ERROR}) via
 *    {@code ResponseStatusException(BAD_REQUEST)}.
 *  - Unknown testId on GET/PUT/DELETE yields 404 ({@code GENERIC_STATUS_ERROR}).
 *  - The integrity guard ({@code FpTestStateException}) only fires when statementKeys CHANGE
 *    while submissions already exist for the test; editing other fields, or re-sending the
 *    same keys, is allowed.
 */
class FpTestControllerSpec extends BaseIntegrationSpec {

    private void seedDefinition(String statementId, StatementProfile category, String statementKey) {
        def doc = new StatementDefinitionDocument(statementId, statementKey, category, [
                new StatementTypeDefinition(StatementType.LIMITING, "ograniczajace " + statementId),
                new StatementTypeDefinition(StatementType.SUPPORTING, "wspierajace " + statementId)
        ])
        mongoTemplate.insert(doc).block()
    }

    private FpTestDocument seedFpTest(String testId, List<String> statementKeys) {
        def doc = new FpTestDocument()
        doc.testId = testId
        doc.testName = "Test " + testId
        doc.descriptionBefore = "before"
        doc.descriptionAfter = "after"
        doc.fpTestStatementDocuments = statementKeys.collect {
                new FpTestStatementDocument(it, "desc " + it, "PROFIL")
        }
        mongoTemplate.insert(doc).block()
        return doc
    }

    private void seedSubmissionForTest(String testId) {
        def doc = new SubmissionDocument()
        doc.submissionId = "sub_" + UUID.randomUUID()
        doc.orderId = "order_" + UUID.randomUUID()
        doc.clientId = "client-1"
        doc.clientName = "Jan Kowalski"
        doc.clientEmail = "jan@example.com"
        doc.testId = testId
        doc.status = SubmissionStatus.OPEN
        doc.durationDays = 7
        doc.publicToken = "pt_" + UUID.randomUUID()
        doc.expireAt = Instant.now().plusSeconds(7 * 24 * 60 * 60)
        mongoTemplate.insert(doc).block()
    }

    private static Map fpTestPayload(Map overrides = [:]) {
        ([
                testName         : "Finanse 1",
                descriptionBefore: "opis przed",
                descriptionAfter : "opis po",
                statementKeys    : ["p1_q1"]
        ] + overrides)
    }

    def "should create a test, generate fpt_ testId and resolve statement descriptions"() {
        given:
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")

        when:
        def result = authenticatedPost("/api/pftest", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fpTestPayload())
                .exchange()

        then:
        result.expectStatus().isCreated()
                .expectBody()
                .jsonPath('$.testId').value({ it.startsWith("fpt_") })
                .jsonPath('$.testName').isEqualTo("Finanse 1")
                .jsonPath('$.fpTestStatementDtoList.length()').isEqualTo(1)
                .jsonPath('$.fpTestStatementDtoList[0].statementKey').isEqualTo("p1_q1")
                .jsonPath('$.fpTestStatementDtoList[0].statementsDescription').isEqualTo("ograniczajace p1_q1-wspierajace p1_q1")
                .jsonPath('$.fpTestStatementDtoList[0].statementsCategory').isEqualTo("Strażniczka Braku")
                .jsonPath('$.submissionIds.length()').isEqualTo(0)

        and:
        def saved = mongoTemplate.findAll(FpTestDocument).collectList().block()
        saved.size() == 1
        saved[0].testId.startsWith("fpt_")
    }

    def "should return 400 when creating a test with an unknown statementKey"() {
        when:
        def result = authenticatedPost("/api/pftest", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fpTestPayload(statementKeys: ["does_not_exist"]))
                .exchange()

        then:
        result.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.code').isEqualTo("GENERIC_STATUS_ERROR")

        and: "nothing is persisted"
        mongoTemplate.findAll(FpTestDocument).collectList().block().isEmpty()
    }

    def "should return 400 when creating a test with a blank testName"() {
        given:
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")

        expect:
        authenticatedPost("/api/pftest", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fpTestPayload(testName: ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.code').isEqualTo("VALIDATION_ERROR")
    }

    def "should return all tests"() {
        given:
        seedFpTest("fpt_a", ["p1_q1"])
        seedFpTest("fpt_b", ["p2_q1"])

        when:
        def result = authenticatedGet("/api/pftest", "BUSINESS_ADMIN").exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.length()').isEqualTo(2)
                .jsonPath('$[?(@.testId == "fpt_a")]').exists()
                .jsonPath('$[?(@.testId == "fpt_b")]').exists()
    }

    def "should return a test by testId"() {
        given:
        seedFpTest("fpt_find", ["p1_q1"])

        when:
        def result = authenticatedGet("/api/pftest/fpt_find", "BUSINESS_ADMIN").exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.testId').isEqualTo("fpt_find")
    }

    def "should return 404 for an unknown testId"() {
        expect:
        authenticatedGet("/api/pftest/fpt_missing", "BUSINESS_ADMIN").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath('$.code').isEqualTo("GENERIC_STATUS_ERROR")
    }

    def "should update a test without submissions"() {
        given:
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")
        seedDefinition("p2_q1", StatementProfile.PROFIL_2, "p2_q1")
        seedFpTest("fpt_upd", ["p1_q1"])

        when: "the statement set is changed and there are no submissions"
        def result = authenticatedPut("/api/pftest/fpt_upd", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fpTestPayload(testName: "Zmieniony", statementKeys: ["p1_q1", "p2_q1"]))
                .exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.testId').isEqualTo("fpt_upd")
                .jsonPath('$.testName').isEqualTo("Zmieniony")
                .jsonPath('$.fpTestStatementDtoList.length()').isEqualTo(2)
    }

    def "should return 409 when changing statements of a test that already has submissions"() {
        given:
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")
        seedDefinition("p2_q1", StatementProfile.PROFIL_2, "p2_q1")
        seedFpTest("fpt_guard", ["p1_q1"])
        seedSubmissionForTest("fpt_guard")

        expect:
        authenticatedPut("/api/pftest/fpt_guard", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fpTestPayload(statementKeys: ["p1_q1", "p2_q1"]))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath('$.code').isEqualTo("FP_TEST_EDIT_ERROR")
    }

    def "should allow re-sending the same statements when submissions exist"() {
        given:
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")
        seedFpTest("fpt_same", ["p1_q1"])
        seedSubmissionForTest("fpt_same")

        expect: "same statement keys -> integrity guard does not fire"
        authenticatedPut("/api/pftest/fpt_same", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fpTestPayload(testName: "Inny tytul", statementKeys: ["p1_q1"]))
                .exchange()
                .expectStatus().isOk()
    }

    def "should return 404 when updating an unknown testId"() {
        given:
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")

        expect:
        authenticatedPut("/api/pftest/fpt_missing", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fpTestPayload())
                .exchange()
                .expectStatus().isNotFound()
    }

    def "should delete a test without submissions"() {
        given:
        seedFpTest("fpt_del", ["p1_q1"])

        when:
        def result = authenticatedDelete("/api/pftest/fpt_del", "BUSINESS_ADMIN").exchange()

        then:
        result.expectStatus().isEqualTo(202)

        and:
        mongoTemplate.findAll(FpTestDocument).collectList().block().isEmpty()
    }

    def "should return 409 when deleting a test that already has submissions"() {
        given:
        seedFpTest("fpt_del_guard", ["p1_q1"])
        seedSubmissionForTest("fpt_del_guard")

        expect:
        authenticatedDelete("/api/pftest/fpt_del_guard", "BUSINESS_ADMIN").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath('$.code').isEqualTo("FP_TEST_DELETE_ERROR")

        and: "the test is still present"
        mongoTemplate.findAll(FpTestDocument).collectList().block().size() == 1
    }

    def "should return 404 when deleting an unknown testId"() {
        expect:
        authenticatedDelete("/api/pftest/fpt_missing", "BUSINESS_ADMIN").exchange()
                .expectStatus().isNotFound()
    }

    def "should return all available statements derived from the definitions"() {
        given:
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")
        seedDefinition("p2_q1", StatementProfile.PROFIL_2, "p2_q1")

        when:
        def result = authenticatedGet("/api/pftest/statements", "BUSINESS_ADMIN").exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.length()').isEqualTo(2)
                .jsonPath('$[?(@.statementKey == "p1_q1")]').exists()
                .jsonPath('$[?(@.statementKey == "p2_q1")]').exists()
    }

    def "should return 401 for GET /api/pftest without a token"() {
        expect:
        webTestClient.get().uri("/api/pftest")
                .exchange()
                .expectStatus().isUnauthorized()
    }

    @Unroll
    def "should map role #role to status #status on the pftest endpoints"() {
        expect:
        authenticatedGet("/api/pftest", role)
                .exchange()
                .expectStatus().isEqualTo(status)

        where:
        role              | status
        "CALCULATOR_USER" | 403
        "BUSINESS_ADMIN"  | 200
        "TECH_ADMIN"      | 200
    }
}
