package com.emat.reapi

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Shared

import static com.github.tomakehurst.wiremock.client.WireMock.*

/**
 * Base spec for controllers backed by the Keycloak Admin API ({@code AccountController},
 * {@code UserController}). Stubs Keycloak with WireMock and overrides
 * {@code keycloak.admin.base-url} with the dynamic WireMock port.
 *
 * Every Keycloak call is preceded by an admin-token fetch
 * ({@code POST /realms/{realm}/protocol/openid-connect/token}); {@code setup()} stubs it
 * once per test. The admin REST calls live under {@code /admin/realms/{realm}}.
 *
 * Security (401/403 and the role matrix) for these endpoints is covered centrally in
 * {@code SecurityAccessControlSpec}, so subclasses focus only on the Keycloak interaction.
 */
abstract class KeycloakWireMockSpec extends BaseIntegrationSpec {

    static final String REALM = "test-realm"
    static final String ADMIN = "/admin/realms/" + REALM
    static final String ROLE = "CALCULATOR_USER"

    private static final ObjectMapper MAPPER = new ObjectMapper()

    @Shared
    static WireMockServer keycloak = new WireMockServer(WireMockConfiguration.options().dynamicPort())

    static {
        keycloak.start()
    }

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("keycloak.admin.base-url") { "http://localhost:${keycloak.port()}" }
    }

    def setup() {
        keycloak.resetAll()
        stubToken()
    }

    // ---- stub helpers ----

    protected static void stubToken() {
        keycloak.stubFor(post(urlPathEqualTo("/realms/${REALM}/protocol/openid-connect/token"))
                .willReturn(okJson('{"access_token":"fake-admin-token"}')))
    }

    protected static String userJson(Map overrides = [:]) {
        def user = [
                id              : "u-1",
                username        : "jan.kowalski",
                firstName       : "Jan",
                lastName        : "Kowalski",
                email           : "jan@example.com",
                enabled         : true,
                emailVerified   : true,
                createdTimestamp: 1_700_000_000_000L
        ] + overrides
        MAPPER.writeValueAsString(user)
    }

    protected static String usersArrayJson(List<Map> users) {
        MAPPER.writeValueAsString(users.collect { [
                id              : "u-1",
                username        : "jan.kowalski",
                firstName       : "Jan",
                lastName        : "Kowalski",
                email           : "jan@example.com",
                enabled         : true,
                emailVerified   : true,
                createdTimestamp: 1_700_000_000_000L
        ] + it })
    }

    protected static void stubGetUser(String userId, String body) {
        keycloak.stubFor(get(urlPathEqualTo("${ADMIN}/users/${userId}"))
                .willReturn(okJson(body)))
    }

    protected static void stubGetUserNotFound(String userId) {
        keycloak.stubFor(get(urlPathEqualTo("${ADMIN}/users/${userId}"))
                .willReturn(notFound()))
    }

    protected static void stubListUsers(String arrayBody) {
        keycloak.stubFor(get(urlPathEqualTo("${ADMIN}/roles/${ROLE}/users"))
                .willReturn(okJson(arrayBody)))
    }

    protected static void stubPutUser(String userId) {
        keycloak.stubFor(put(urlPathEqualTo("${ADMIN}/users/${userId}"))
                .willReturn(noContent()))
    }

    protected static void stubDeleteUser(String userId) {
        keycloak.stubFor(delete(urlPathEqualTo("${ADMIN}/users/${userId}"))
                .willReturn(noContent()))
    }

    protected static void stubCreateUser(String newUserId) {
        keycloak.stubFor(post(urlPathEqualTo("${ADMIN}/users"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Location", "${ADMIN}/users/${newUserId}")))
        keycloak.stubFor(post(urlPathMatching("${ADMIN}/users/.*/role-mappings/realm"))
                .willReturn(noContent()))
        keycloak.stubFor(put(urlPathMatching("${ADMIN}/users/.*/execute-actions-email"))
                .willReturn(noContent()))
    }

    protected static void stubCreateUserConflict() {
        keycloak.stubFor(post(urlPathEqualTo("${ADMIN}/users"))
                .willReturn(aResponse().withStatus(409)))
    }

    protected static void stubExecuteActionsEmail(String userId) {
        keycloak.stubFor(put(urlPathEqualTo("${ADMIN}/users/${userId}/execute-actions-email"))
                .willReturn(noContent()))
    }
}
