package com.emat.reapi.statement

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.fptest.infra.FpTestDocument
import com.emat.reapi.fptest.infra.FpTestStatementDocument
import com.emat.reapi.migrations.v008profilesinit.ProfilesSeed
import com.emat.reapi.statement.domain.StatementType
import com.emat.reapi.statement.domain.StatementTypeDefinition
import com.emat.reapi.statement.infra.ProfileDocument
import com.emat.reapi.statement.infra.StatementDefinitionDocument
import com.emat.reapi.submission.domain.SubmissionStatus
import com.emat.reapi.submission.infra.SubmissionDocument
import org.springframework.http.MediaType
import spock.lang.Unroll

import java.time.Instant

/**
 * Integration tests for {@code StatementDefinitionController} on the F8 contract:
 * server-generated immutable statementKey ("sk_" + UUID), split request/response DTOs,
 * soft-delete with isDeleted filtering and createdAt ordering.
 */
class StatementDefinitionControllerSpec extends BaseIntegrationSpec {

    def setup() {
        ProfilesSeed.ALL.each { mongoTemplate.insert(ProfileDocument.toDocument(it)).block() }
    }

    private int seedCounter = 0

    def "should create a definition with a server-generated sk_ statementKey"() {
        when:
        def result = authenticatedPost("/api/definition", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(definitionPayload("profil_1"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "the response carries server-owned fields"
        result.id != null
        result.statementKey.startsWith("sk_")
        result.profileId == "profil_1"
        result.createdAt != null
        result.updatedAt != null
        result.statementTypeDefinitions[0].statementType == "LIMITING"
        result.statementTypeDefinitions[1].statementType == "SUPPORTING"

        and: "exactly one document is stored"
        def saved = mongoTemplate.findAll(StatementDefinitionDocument).collectList().block()
        saved.size() == 1
        saved[0].statementKey == result.statementKey
        saved[0].profileId == "profil_1"
        !saved[0].isDeleted
    }

    def "should return 400 when creating a definition with an unknown profileId"() {
        when:
        def result = authenticatedPost("/api/definition", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(definitionPayload("not_a_profile"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "GENERIC_STATUS_ERROR"

        and: "nothing is persisted"
        mongoTemplate.findAll(StatementDefinitionDocument).collectList().block().isEmpty()
    }

    def "should return all active definitions ordered by createdAt, hiding soft-deleted ones"() {
        given:
        seedDefinition("profil_1", "p1_q1")
        seedDefinition("profil_2", "p2_q1")
        seedDefinition("profil_1", "p1_q9", true)

        when:
        def result = authenticatedGet("/api/definition", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then: "soft-deleted definition is excluded"
        result.size() == 2
        result[0].statementKey == "p1_q1"
        result[1].statementKey == "p2_q1"
    }

    def "should return only the requested profile, ordered by createdAt ascending"() {
        given: "two profil_1 definitions plus an unrelated profil_2 one"
        seedDefinition("profil_1", "p1_q2")
        seedDefinition("profil_1", "p1_q1")
        seedDefinition("profil_2", "p2_q1")

        when:
        def result = authenticatedGet("/api/definition?profileId=profil_1", "BUSINESS_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then: "only profil_1 is returned, in insertion (createdAt) order"
        result.size() == 2
        result[0].statementKey == "p1_q2"
        result[1].statementKey == "p1_q1"
        result[0].profileId == "profil_1"
        result[1].profileId == "profil_1"
    }

    def "should return an empty array for a profile with no definitions"() {
        when:
        def result = authenticatedGet("/api/definition?profileId=profil_8", "BUSINESS_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then:
        result.isEmpty()
    }

    def "should return an empty array for an unknown profileId (free string, no longer a 400)"() {
        when: "profileId is a free-form reference, not a constrained enum"
        def result = authenticatedGet("/api/definition?profileId=not_a_profile", "BUSINESS_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then:
        result.isEmpty()
    }

    def "should return 400 for an empty body"() {
        when:
        def result = authenticatedPost("/api/definition", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([:])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
    }

    def "should update a definition's profileId and texts while keeping the statementKey immutable"() {
        given:
        def existing = seedDefinition("profil_1", "sk_original")

        when:
        def result = authenticatedPut("/api/definition/${existing.id}", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(definitionPayload("profil_2", "zmienione"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "profileId and texts change but the statementKey stays fixed"
        result.id == existing.id
        result.statementKey == "sk_original"
        result.profileId == "profil_2"
        result.statementTypeDefinitions[0].statementDescription == "ograniczajace zmienione"

        and: "the change is persisted"
        def saved = mongoTemplate.findById(existing.id, StatementDefinitionDocument).block()
        saved.profileId == "profil_2"
        saved.statementKey == "sk_original"
    }

    def "should return 404 when updating an unknown definition id"() {
        when:
        def result = authenticatedPut("/api/definition/nope", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(definitionPayload("profil_1"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "GENERIC_STATUS_ERROR"
    }

    def "should return 400 when updating a definition to an unknown profileId"() {
        given:
        def existing = seedDefinition("profil_1", "sk_x")

        when:
        def result = authenticatedPut("/api/definition/${existing.id}", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(definitionPayload("not_a_profile"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "GENERIC_STATUS_ERROR"
    }

    def "should return 409 DEFINITION_EDIT_ERROR when reassigning profile of a definition used in a submitted test"() {
        given: "the definition sits in an FpTest that already has a submission"
        def existing = seedDefinition("profil_1", "sk_used")
        seedFpTest("fpt_1", ["sk_used"])
        seedSubmissionForTest("fpt_1")

        when:
        def result = authenticatedPut("/api/definition/${existing.id}", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(definitionPayload("profil_2"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "DEFINITION_EDIT_ERROR"

        and: "the definition keeps its original profile"
        mongoTemplate.findById(existing.id, StatementDefinitionDocument).block().profileId == "profil_1"
    }

    def "should allow reassigning profile when the test using the definition has no submissions"() {
        given: "the definition is in an FpTest but nobody has submitted yet"
        def existing = seedDefinition("profil_1", "sk_free")
        seedFpTest("fpt_free", ["sk_free"])

        when:
        def response = authenticatedPut("/api/definition/${existing.id}", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(definitionPayload("profil_2"))
                .exchange()

        then:
        response.expectStatus().isOk()
        mongoTemplate.findById(existing.id, StatementDefinitionDocument).block().profileId == "profil_2"
    }

    def "should allow editing texts of a submitted-test definition when the profile stays the same"() {
        given: "same guard, but profileId is unchanged so the reassignment guard short-circuits"
        def existing = seedDefinition("profil_1", "sk_same_profile")
        seedFpTest("fpt_same", ["sk_same_profile"])
        seedSubmissionForTest("fpt_same")

        when:
        def response = authenticatedPut("/api/definition/${existing.id}", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(definitionPayload("profil_1", "nowe teksty"))
                .exchange()

        then:
        response.expectStatus().isOk()
    }

    def "should soft-delete a definition not referenced by any test"() {
        given:
        def existing = seedDefinition("profil_1", "sk_del")

        when:
        authenticatedDelete("/api/definition/${existing.id}", "BUSINESS_ADMIN").exchange()
                .expectStatus().isAccepted()

        then: "the document is flagged deleted, not removed, and hidden from the active list"
        mongoTemplate.findById(existing.id, StatementDefinitionDocument).block().isDeleted

        and:
        def active = authenticatedGet("/api/definition", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody
        active.isEmpty()
    }

    def "should return 404 when soft-deleting an unknown definition id"() {
        when:
        def result = authenticatedDelete("/api/definition/nope", "BUSINESS_ADMIN").exchange()
                .expectStatus().isNotFound()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "GENERIC_STATUS_ERROR"
    }

    def "should return 409 DEFINITION_IN_USE when the definition key is used in an existing test"() {
        given:
        def existing = seedDefinition("profil_1", "sk_in_test")
        seedFpTest("fpt_uses", ["sk_in_test"])

        when:
        def result = authenticatedDelete("/api/definition/${existing.id}", "BUSINESS_ADMIN").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "DEFINITION_IN_USE"

        and: "the definition is left active"
        !mongoTemplate.findById(existing.id, StatementDefinitionDocument).block().isDeleted
    }

    def "should return 401 for GET /api/definition without a token"() {
        when:
        def response = webTestClient.get().uri("/api/definition").exchange()

        then:
        response.expectStatus().isUnauthorized()
    }

    @Unroll
    def "should map role #role to status #status on the definition endpoints"() {
        when:
        def response = authenticatedGet("/api/definition", role).exchange()

        then:
        response.expectStatus().isEqualTo(status)

        where:
        role              | status
        "CALCULATOR_USER" | 403
        "BUSINESS_ADMIN"  | 200
        "TECH_ADMIN"      | 200
    }

    private StatementDefinitionDocument seedDefinition(String profileId, String statementKey, boolean isDeleted = false) {
        // incremented timestamps keep list ordering (createdAt asc) deterministic
        def timestamp = Instant.now().plusMillis(seedCounter++)
        def doc = new StatementDefinitionDocument(
                statementKey: statementKey,
                profileId: profileId,
                statementTypeDefinitions: [
                        new StatementTypeDefinition(StatementType.LIMITING, "ograniczajace " + statementKey),
                        new StatementTypeDefinition(StatementType.SUPPORTING, "wspierajace " + statementKey)
                ],
                isDeleted: isDeleted,
                createdAt: timestamp,
                updatedAt: timestamp
        )
        mongoTemplate.insert(doc).block()
    }

    private static Map definitionPayload(String profileId, String marker = "x") {
        [
                profileId               : profileId,
                statementTypeDefinitions: [
                        [statementType: "LIMITING", statementDescription: "ograniczajace " + marker],
                        [statementType: "SUPPORTING", statementDescription: "wspierajace " + marker]
                ]
        ]
    }

    // Seeds an FpTest referencing the given statementKeys — used to exercise the
    // DEFINITION_IN_USE / DEFINITION_EDIT_ERROR guards.
    private void seedFpTest(String testId, List<String> statementKeys) {
        def doc = new FpTestDocument()
        doc.testId = testId
        doc.testName = "Test " + testId
        doc.descriptionBefore = "before"
        doc.descriptionAfter = "after"
        doc.fpTestStatementDocuments = statementKeys.collect {
                new FpTestStatementDocument(it, "desc " + it, "PROFIL")
        }
        mongoTemplate.insert(doc).block()
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
}
