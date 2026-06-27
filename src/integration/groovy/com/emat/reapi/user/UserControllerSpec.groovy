package com.emat.reapi.user

import com.emat.reapi.KeycloakWireMockSpec
import org.springframework.http.MediaType

/**
 * Characterization tests pinning the CURRENT behavior of {@code UserController}
 * (Keycloak calculator-user management).
 *
 * Security (401 / role access) is covered in {@code SecurityAccessControlSpec}.
 * Error paths for update/delete/status are NOT covered: those client calls use a plain
 * {@code .retrieve()} (no custom onStatus), so a Keycloak 4xx/5xx surfaces as a generic
 * 500, not a typed KeycloakException.
 */
class UserControllerSpec extends KeycloakWireMockSpec {

    private static Map userPayload(Map overrides = [:]) {
        ([
                username     : "jan.kowalski",
                firstName    : "Jan",
                lastName     : "Kowalski",
                email        : "jan@example.com",
                enabled      : true,
                emailVerified: true
        ] + overrides)
    }

    def "should list calculator users sorted by createdAt descending"() {
        given:
        stubListUsers(usersArrayJson([
                [id: "u-old", username: "old.user", createdTimestamp: 1_600_000_000_000L],
                [id: "u-new", username: "new.user", createdTimestamp: 1_800_000_000_000L]
        ]))

        when:
        def result = authenticatedGet("/api/users", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then: "newest createdAt first"
        result.size() == 2
        result[0].id == "u-new"
        result[1].id == "u-old"
    }

    def "should create a calculator user and return the new id"() {
        given:
        stubCreateUser("new-user-id")

        when:
        def result = authenticatedPost("/api/users", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String)
                .returnResult()
                .responseBody

        then:
        result.contains("new-user-id")
    }

    def "should return 409 USER_ALREADY_EXISTS when Keycloak reports a conflict"() {
        given:
        stubCreateUserConflict()

        when:
        def result = authenticatedPost("/api/users", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload())
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "USER_ALREADY_EXISTS"
    }

    def "should return 400 VALIDATION_ERROR for a blank username on create"() {
        when:
        def result = authenticatedPost("/api/users", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload(username: ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
    }

    def "should update a user and return 202 with the updated profile"() {
        given: "existing user is emailVerified so no verify-email action is triggered"
        stubGetUser("u-42", userJson(id: "u-42", username: "jan.kowalski", emailVerified: true))
        stubPutUser("u-42")

        when:
        def result = authenticatedPut("/api/users/u-42", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userPayload(firstName: "Janusz", emailVerified: true))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.id == "u-42"
        result.firstName == "Janusz"
    }

    def "should delete a user with 204"() {
        given:
        stubDeleteUser("u-42")

        when:
        def response = authenticatedDelete("/api/users/u-42", "BUSINESS_ADMIN").exchange()

        then:
        response.expectStatus().isNoContent()
    }

    def "should change a user's status with 202"() {
        given:
        stubGetUser("u-42", userJson(id: "u-42"))
        stubPutUser("u-42")

        when:
        def response = authenticatedPut("/api/users/u-42/status?status=false", "BUSINESS_ADMIN").exchange()

        then:
        response.expectStatus().isAccepted()
    }
}
