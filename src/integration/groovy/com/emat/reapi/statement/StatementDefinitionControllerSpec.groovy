package com.emat.reapi.statement

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.migrations.v008profilesinit.ProfilesSeed
import com.emat.reapi.statement.domain.StatementType
import com.emat.reapi.statement.domain.StatementTypeDefinition
import com.emat.reapi.statement.infra.ProfileDocument
import com.emat.reapi.statement.infra.StatementDefinitionDocument
import org.springframework.http.MediaType
import spock.lang.Unroll

import java.time.Instant

/**
 * Integration tests for {@code StatementDefinitionController} on the F8 contract:
 * server-generated immutable statementKey ("sk_" + UUID), split request/response DTOs,
 * soft-delete with isDeleted filtering and createdAt ordering.
 */
class StatementDefinitionControllerSpec extends BaseIntegrationSpec {

    // POST/PUT validate that profileId points to an existing, active profile —
    // seed the standard profiles (BaseIntegrationSpec drops all collections).
    def setup() {
        ProfilesSeed.ALL.each { mongoTemplate.insert(ProfileDocument.toDocument(it)).block() }
    }

    private int seedCounter = 0

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
}
