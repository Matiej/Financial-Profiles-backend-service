package com.emat.reapi.statement

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.statement.domain.StatementType
import com.emat.reapi.statement.domain.StatementTypeDefinition
import com.emat.reapi.statement.infra.ProfileDocument
import com.emat.reapi.statement.infra.StatementDefinitionDocument
import org.springframework.http.MediaType
import spock.lang.Unroll

import java.time.Instant

/**
 * Integration tests for {@code ProfileController} (F7): CRUD over editable scoring profiles,
 * soft-delete with the {@code PROFILE_IN_USE} 409 guard (only ACTIVE definitions block deletion),
 * and role-based access control. BaseIntegrationSpec drops all collections before each test,
 * so every spec seeds exactly what it needs.
 */
class ProfileControllerSpec extends BaseIntegrationSpec {

    def "should create a profile with server-owned id, isDeleted=false and timestamps"() {
        when:
        def result = authenticatedPost("/api/definition/profile", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(profilePayload())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "the response carries the editable fields plus server-owned metadata"
        result.id != null
        result.plName == "Nowy Profil"
        result.blockingName == "Nowy Profil (blokada)"
        result.transitionalName == "Nowy Profil (strefa przejściowa)"
        result.resourcesName == "Nowy Profil (zasoby)"
        result.order == 5
        result.isDeleted == false
        result.createdAt != null
        result.updatedAt != null

        and: "exactly one document is stored"
        def saved = mongoTemplate.findAll(ProfileDocument).collectList().block()
        saved.size() == 1
        saved[0].id == result.id
        !saved[0].isDeleted
    }

    def "should return 400 when creating a profile with a blank required field"() {
        when:
        def result = authenticatedPost("/api/definition/profile", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(profilePayload(plName: ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"

        and: "nothing is persisted"
        mongoTemplate.findAll(ProfileDocument).collectList().block().isEmpty()
    }

    def "should return active profiles ordered by order, hiding soft-deleted ones"() {
        given:
        seedProfile("profil_2", 2)
        seedProfile("profil_1", 1)
        seedProfile("profil_9", 9, true)

        when:
        def result = authenticatedGet("/api/definition/profile", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then: "soft-deleted profile is excluded and the rest come back ordered by order asc"
        result.size() == 2
        result[0].id == "profil_1"
        result[1].id == "profil_2"
    }

    def "should return a single profile by id"() {
        given:
        seedProfile("profil_1", 1)

        when:
        def result = authenticatedGet("/api/definition/profile/profil_1", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.id == "profil_1"
        result.plName == "Profil profil_1"
    }

    def "should return 404 when fetching an unknown profile id"() {
        when:
        def result = authenticatedGet("/api/definition/profile/not_a_profile", "BUSINESS_ADMIN").exchange()
                .expectStatus().isNotFound()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "GENERIC_STATUS_ERROR"
    }

    def "should update a profile's editable fields"() {
        given:
        seedProfile("profil_1", 1)

        when:
        def result = authenticatedPut("/api/definition/profile/profil_1", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(profilePayload(plName: "Zmieniony", order: 7))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "the response reflects the new values while keeping the id"
        result.id == "profil_1"
        result.plName == "Zmieniony"
        result.order == 7

        and: "the change is persisted"
        def saved = mongoTemplate.findById("profil_1", ProfileDocument).block()
        saved.plName == "Zmieniony"
        saved.order == 7
    }

    def "should return 404 when updating an unknown profile id"() {
        when:
        def result = authenticatedPut("/api/definition/profile/not_a_profile", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(profilePayload())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "GENERIC_STATUS_ERROR"
    }

    def "should soft-delete a profile with no active definitions"() {
        given:
        seedProfile("profil_1", 1)

        when:
        authenticatedDelete("/api/definition/profile/profil_1", "BUSINESS_ADMIN").exchange()
                .expectStatus().isAccepted()

        then: "the document is flagged deleted, not removed"
        def saved = mongoTemplate.findById("profil_1", ProfileDocument).block()
        saved != null
        saved.isDeleted

        and: "it disappears from the active list"
        def active = authenticatedGet("/api/definition/profile", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody
        active.isEmpty()
    }

    def "should return 404 when soft-deleting an unknown profile id"() {
        when:
        def result = authenticatedDelete("/api/definition/profile/not_a_profile", "BUSINESS_ADMIN").exchange()
                .expectStatus().isNotFound()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "GENERIC_STATUS_ERROR"
    }

    def "should return 409 PROFILE_IN_USE when an active definition references the profile"() {
        given:
        seedProfile("profil_1", 1)
        seedDefinition("profil_1", "p1_q1")

        when:
        def result = authenticatedDelete("/api/definition/profile/profil_1", "BUSINESS_ADMIN").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "PROFILE_IN_USE"

        and: "the profile is left untouched"
        !mongoTemplate.findById("profil_1", ProfileDocument).block().isDeleted
    }

    def "should allow soft-delete when the only referencing definition is itself soft-deleted"() {
        given: "a profile referenced only by an already soft-deleted definition"
        seedProfile("profil_1", 1)
        seedDefinition("profil_1", "p1_q1", true)

        when:
        authenticatedDelete("/api/definition/profile/profil_1", "BUSINESS_ADMIN").exchange()
                .expectStatus().isAccepted()

        then: "soft-deleted definitions do not block deletion"
        mongoTemplate.findById("profil_1", ProfileDocument).block().isDeleted
    }

    def "should return 401 for GET /api/definition/profile without a token"() {
        when:
        def response = webTestClient.get().uri("/api/definition/profile").exchange()

        then:
        response.expectStatus().isUnauthorized()
    }

    @Unroll
    def "should map role #role to status #status on the profile endpoints"() {
        when:
        def response = authenticatedGet("/api/definition/profile", role).exchange()

        then:
        response.expectStatus().isEqualTo(status)

        where:
        role              | status
        "CALCULATOR_USER" | 403
        "BUSINESS_ADMIN"  | 200
        "TECH_ADMIN"      | 200
    }

    private static Map profilePayload(Map overrides = [:]) {
        [
                plName          : "Nowy Profil",
                blockingName    : "Nowy Profil (blokada)",
                transitionalName: "Nowy Profil (strefa przejściowa)",
                resourcesName   : "Nowy Profil (zasoby)",
                order           : 5
        ] + overrides
    }

    private ProfileDocument seedProfile(String id, int order = 1, boolean isDeleted = false) {
        def now = Instant.now()
        def doc = ProfileDocument.builder()
                .id(id)
                .plName("Profil " + id)
                .blockingName("Profil " + id + " (blokada)")
                .transitionalName("Profil " + id + " (strefa przejściowa)")
                .resourcesName("Profil " + id + " (zasoby)")
                .order(order)
                .isDeleted(isDeleted)
                .createdAt(now)
                .updatedAt(now)
                .build()
        mongoTemplate.insert(doc).block()
    }

    private StatementDefinitionDocument seedDefinition(String profileId, String statementKey, boolean isDeleted = false) {
        def now = Instant.now()
        def doc = new StatementDefinitionDocument(
                statementKey: statementKey,
                profileId: profileId,
                statementTypeDefinitions: [
                        new StatementTypeDefinition(StatementType.LIMITING, "ograniczajace " + statementKey),
                        new StatementTypeDefinition(StatementType.SUPPORTING, "wspierajace " + statementKey)
                ],
                isDeleted: isDeleted,
                createdAt: now,
                updatedAt: now
        )
        mongoTemplate.insert(doc).block()
    }
}
