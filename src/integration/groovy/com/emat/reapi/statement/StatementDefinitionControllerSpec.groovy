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
                .expectStatus().isCreated()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "the controller echoes the saved definition"
        result.statementId == "p1_q1"
        result.category == "PROFIL_1"
        result.statementKey == "p1_q1"
        result.statementTypeDefinitions[0].statementType == "LIMITING"
        result.statementTypeDefinitions[1].statementType == "SUPPORTING"

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
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then:
        result.size() == 2
        result.collect { it.statementKey } as Set == ["p1_q1", "p2_q1"] as Set
    }

    def "should return only the requested category, ordered by statementId ascending"() {
        given: "two PROFIL_1 definitions inserted out of order, plus an unrelated PROFIL_2 one"
        seedDefinition("p1_q2", StatementProfile.PROFIL_1, "p1_q2")
        seedDefinition("p1_q1", StatementProfile.PROFIL_1, "p1_q1")
        seedDefinition("p2_q1", StatementProfile.PROFIL_2, "p2_q1")

        when:
        def result = authenticatedGet("/api/definition/category?category=PROFIL_1", "BUSINESS_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then: "only PROFIL_1 is returned, sorted by statementId asc (PROFIL_2 excluded)"
        result.size() == 2
        result[0].statementId == "p1_q1"
        result[1].statementId == "p1_q2"
        result[0].category == "PROFIL_1"
        result[1].category == "PROFIL_1"
    }

    def "should return an empty array for a category with no definitions"() {
        when:
        def result = authenticatedGet("/api/definition/category?category=PROFIL_8", "BUSINESS_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then:
        result.isEmpty()
    }

    def "should return 400 for an unknown category enum value"() {
        when:
        def response = authenticatedGet("/api/definition/category?category=NOT_A_PROFILE", "BUSINESS_ADMIN")
                .exchange()

        then:
        response.expectStatus().isBadRequest()
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
