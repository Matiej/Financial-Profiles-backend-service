package com.emat.reapi.account

import com.emat.reapi.KeycloakWireMockSpec
import org.springframework.http.MediaType

/**
 * Characterization tests pinning the CURRENT behavior of {@code AccountController}.
 *
 * The account is the caller's own Keycloak user: the controller reads
 * {@code jwt.getSubject()}, which the test security config fixes to {@code "test-user"}.
 * Security (401 / role access) is covered in {@code SecurityAccessControlSpec}.
 */
class AccountControllerSpec extends KeycloakWireMockSpec {

    private static final String SUBJECT = "test-user"

    def "should return the caller's own account"() {
        given:
        stubGetUser(SUBJECT, userJson(id: SUBJECT, username: "anna", firstName: "Anna",
                lastName: "Nowak", email: "anna@example.com"))

        when:
        def result = authenticatedGet("/api/account", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.id == SUBJECT
        result.username == "anna"
        result.firstName == "Anna"
        result.lastName == "Nowak"
        result.email == "anna@example.com"
        result.enabled == true
        result.emailVerified == true
    }

    def "should return 404 GENERIC_ERROR when the user is not found in Keycloak"() {
        given:
        stubGetUserNotFound(SUBJECT)

        when:
        def result = authenticatedGet("/api/account", "BUSINESS_ADMIN").exchange()
                .expectStatus().isNotFound()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "GENERIC_ERROR"
    }

    def "should update the caller's profile"() {
        given: "existing user is emailVerified so no verify-email action is triggered"
        stubGetUser(SUBJECT, userJson(id: SUBJECT, username: "anna", emailVerified: true))
        stubPutUser(SUBJECT)

        when:
        def result = authenticatedPut("/api/account", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([firstName: "Joanna", lastName: "Kowalczyk", email: "joanna@example.com"])
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "the response reflects the submitted profile fields"
        result.firstName == "Joanna"
        result.lastName == "Kowalczyk"
        result.email == "joanna@example.com"
    }

    def "should return 400 VALIDATION_ERROR for a blank firstName"() {
        when:
        def result = authenticatedPut("/api/account", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([firstName: "", lastName: "Kowalczyk", email: "joanna@example.com"])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
    }

    def "should accept a password reset request with 202"() {
        given:
        stubExecuteActionsEmail(SUBJECT)

        when:
        def response = authenticatedPost("/api/account/reset-password", "BUSINESS_ADMIN").exchange()

        then:
        response.expectStatus().isAccepted()
    }

    def "should delete the caller's account with 204"() {
        given:
        stubDeleteUser(SUBJECT)

        when:
        def response = authenticatedDelete("/api/account", "BUSINESS_ADMIN").exchange()

        then:
        response.expectStatus().isNoContent()
    }
}
