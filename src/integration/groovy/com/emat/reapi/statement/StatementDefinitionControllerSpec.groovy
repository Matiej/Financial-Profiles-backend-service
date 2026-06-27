package com.emat.reapi.statement

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.statement.domain.StatementProfile
import com.emat.reapi.statement.domain.StatementType
import com.emat.reapi.statement.domain.StatementTypeDefinition
import com.emat.reapi.statement.infra.StatementDefinitionDocument
import org.springframework.http.MediaType
import spock.lang.Unroll

/**
 * Characterization tests pinning the CURRENT behavior of
 * {@code StatementDefinitionController} before any refactor.
 */
class StatementDefinitionControllerSpec extends BaseIntegrationSpec {

    private void seedDefinition(String statementId, StatementProfile category, String statementKey) {
        def doc = new StatementDefinitionDocument(statementId, statementKey, category, [
                new StatementTypeDefinition(StatementType.LIMITING, "ograniczajace " + statementId),
                new StatementTypeDefinition(StatementType.SUPPORTING, "wspierajace " + statementId)
        ])
        mongoTemplate.insert(doc).block()
    }

    private static Map definitionPayload(String statementId, String category, String statementKey) {
        [
                statementId            : statementId,
                category               : category,
                statementKey           : statementKey,
                statementTypeDefinitions: [
                        [statementType: "LIMITING", statementDescription: "ograniczajace " + statementId],
                        [statementType: "SUPPORTING", statementDescription: "wspierajace " + statementId]
                ]
        ]
    }

    def "should create a statement definition and persist it"() {
        when:
        def result = authenticatedPost("/api/definition", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(definitionPayload("p1_q1", "PROFIL_1", "p1_q1"))
                .exchange()

        then: "the controller echoes the saved definition with 201"
        result.expectStatus().isCreated()
                .expectBody()
                .jsonPath('$.statementId').isEqualTo("p1_q1")
                .jsonPath('$.category').isEqualTo("PROFIL_1")
                .jsonPath('$.statementKey').isEqualTo("p1_q1")
                .jsonPath('$.statementTypeDefinitions[0].statementType').isEqualTo("LIMITING")
                .jsonPath('$.statementTypeDefinitions[1].statementType').isEqualTo("SUPPORTING")

        and: "exactly one document is stored"
        def saved = mongoTemplate.findAll(StatementDefinitionDocument).collectList().block()
        saved.size() == 1
        saved[0].statementKey == "p1_q1"
        saved[0].category.name() == "PROFIL_1"
    }

    def "should return all statement definitions"() {
        given:
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")
        seedDefinition("p2_q1", StatementProfile.PROFIL_2, "p2_q1")

        when:
        def result = authenticatedGet("/api/definition", "BUSINESS_ADMIN").exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.length()').isEqualTo(2)
                .jsonPath('$[?(@.statementKey == "p1_q1")]').exists()
                .jsonPath('$[?(@.statementKey == "p2_q1")]').exists()
    }

    def "should return only the requested category, ordered by statementId ascending"() {
        given: "two PROFIL_1 definitions inserted out of order, plus an unrelated PROFIL_2 one"
        seedDefinition("p1_q2", StatementProfile.PROFIL_1, "p1_q2")
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")
        seedDefinition("p2_q1", StatementProfile.PROFIL_2, "p2_q1")

        when:
        def result = authenticatedGet("/api/definition/category?category=PROFIL_1", "BUSINESS_ADMIN")
                .exchange()

        then: "only PROFIL_1 is returned, sorted by statementId asc (PROFIL_2 excluded)"
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.length()').isEqualTo(2)
                .jsonPath('$[0].statementId').isEqualTo("p1_q1")
                .jsonPath('$[1].statementId').isEqualTo("p1_q2")
                .jsonPath('$[0].category').isEqualTo("PROFIL_1")
                .jsonPath('$[1].category').isEqualTo("PROFIL_1")
    }

    def "should return an empty array for a category with no definitions"() {
        when:
        def result = authenticatedGet("/api/definition/category?category=PROFIL_8", "BUSINESS_ADMIN")
                .exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.length()').isEqualTo(0)
    }

    def "should return 400 for an unknown category enum value"() {
        expect:
        authenticatedGet("/api/definition/category?category=NOT_A_PROFILE", "BUSINESS_ADMIN")
                .exchange()
                .expectStatus().isBadRequest()
    }

    def "should return 400 for an empty body"() {
        expect:
        authenticatedPost("/api/definition", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([:])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.code').isEqualTo("VALIDATION_ERROR")
    }

    def "should return 401 for GET /api/definition without a token"() {
        expect:
        webTestClient.get().uri("/api/definition")
                .exchange()
                .expectStatus().isUnauthorized()
    }

    @Unroll
    def "should map role #role to status #status on the definition endpoints"() {
        expect:
        authenticatedGet("/api/definition", role)
                .exchange()
                .expectStatus().isEqualTo(status)

        where:
        role              | status
        "CALCULATOR_USER" | 403
        "BUSINESS_ADMIN"  | 200
        "TECH_ADMIN"      | 200
    }
}
