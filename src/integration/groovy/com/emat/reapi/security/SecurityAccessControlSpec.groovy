package com.emat.reapi.security

import com.emat.reapi.BaseIntegrationSpec
import org.springframework.http.MediaType
import spock.lang.Unroll

/**
 * Cross-cutting access-control tests pinning the authorization rules defined in
 * {@code SecurityConfiguration} — before any security refactor.
 *
 * These tests are intentionally seedless: no documents are inserted.
 * Business-layer responses (200 empty list, 404 not found, 5xx from external calls)
 * are all acceptable — what matters is whether the auth layer granted or denied
 * access (401 / 403 vs. anything else). Security rejections carry no JSON body,
 * so these specs assert on the HTTP status only.
 *
 * Rule summary (from SecurityConfiguration):
 *  - {@code /api/client/test/**}   — permitAll
 *  - {@code /actuator/health/**}   — permitAll
 *  - {@code /api/ncalculator/**}   — BUSINESS_ADMIN | TECH_ADMIN | CALCULATOR_USER
 *  - {@code /api/account/**}       — BUSINESS_ADMIN | TECH_ADMIN | CALCULATOR_USER
 *  - {@code /api/**}  (catch-all)  — BUSINESS_ADMIN | TECH_ADMIN
 *  - {@code anyExchange()}         — authenticated (any valid JWT, no role check)
 */
class SecurityAccessControlSpec extends BaseIntegrationSpec {

    // ---- 1. Public endpoints — no auth required ----

    @Unroll
    def "should reach public GET #uri without a token (status #expectedStatus)"() {
        when:
        def response = webTestClient.get().uri(uri).exchange()

        then:
        response.expectStatus().isEqualTo(expectedStatus)

        where:
        uri                          | expectedStatus
        "/actuator/health"           | 200
        "/api/client/test/any_token" | 404   // no seed data — but NOT 401
    }

    def "should reach public POST /api/client/test without a token"() {
        when:
        def response = webTestClient.post().uri("/api/client/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([submissionId: "sub_missing", publicToken: "pt_any", clientTestAnswers: []])
                .exchange()

        then:
        response.expectStatus().value({ assert it != 401 && it != 403 })
    }

    // ---- 2. Protected endpoints — no auth → 401 ----

    @Unroll
    def "should return 401 for GET #uri without a token"() {
        when:
        def response = webTestClient.get().uri(uri).exchange()

        then:
        response.expectStatus().isUnauthorized()

        where:
        uri << [
                "/api/submission",
                "/api/pftest",
                "/api/definition",
                "/api/profiler/scoring",
                "/api/analysis/sub_x/status",
                "/api/users",
                "/api/account",
                "/api/appdata/version"
        ]
    }

    def "should return 401 for POST /api/ncalculator/phrase without a token"() {
        when:
        def response = webTestClient.post().uri("/api/ncalculator/phrase")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: "ANIA"])
                .exchange()

        then:
        response.expectStatus().isUnauthorized()
    }

    // ---- 3. CALCULATOR_USER — forbidden on admin-only /api/** endpoints ----

    @Unroll
    def "should return 403 for CALCULATOR_USER on admin endpoint #uri"() {
        when:
        def response = authenticatedGet(uri, "CALCULATOR_USER").exchange()

        then:
        response.expectStatus().isForbidden()

        where:
        uri << [
                "/api/submission",
                "/api/pftest",
                "/api/definition",
                "/api/profiler/scoring",
                "/api/analysis/sub_x/status",
                "/api/users",
                "/api/appdata/version"
        ]
    }

    // ---- 4. CALCULATOR_USER — can access ncalculator and account ----

    def "should grant CALCULATOR_USER access to /api/ncalculator/phrase"() {
        when:
        def response = authenticatedPost("/api/ncalculator/phrase", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: "ANIA"])
                .exchange()

        then:
        response.expectStatus().isOk()
    }

    def "should grant CALCULATOR_USER access to /api/account"() {
        // AccountController calls Keycloak — without a stub returns 5xx, but NOT 401/403
        when:
        def response = authenticatedGet("/api/account", "CALCULATOR_USER").exchange()

        then:
        response.expectStatus().value({ assert it != 401 && it != 403 })
    }

    // ---- 5. BUSINESS_ADMIN and TECH_ADMIN — access to all protected endpoints ----

    @Unroll
    def "should return 200 for #role on GET #uri"() {
        when:
        def response = authenticatedGet(uri, role).exchange()

        then:
        response.expectStatus().isOk()

        where:
        role             | uri
        "BUSINESS_ADMIN" | "/api/submission"
        "TECH_ADMIN"     | "/api/submission"
        "BUSINESS_ADMIN" | "/api/pftest"
        "TECH_ADMIN"     | "/api/pftest"
        "BUSINESS_ADMIN" | "/api/definition"
        "TECH_ADMIN"     | "/api/definition"
        "BUSINESS_ADMIN" | "/api/appdata/version"
        "TECH_ADMIN"     | "/api/appdata/version"
    }

    @Unroll
    def "should grant #role access to #uri"() {
        // These endpoints depend on Keycloak or require seed data — response varies,
        // but must not be an auth rejection.
        when:
        def response = authenticatedGet(uri, role).exchange()

        then:
        response.expectStatus().value({ assert it != 401 && it != 403 })

        where:
        role             | uri
        "BUSINESS_ADMIN" | "/api/profiler/scoring"
        "TECH_ADMIN"     | "/api/profiler/scoring"
        "BUSINESS_ADMIN" | "/api/analysis/sub_x/status"
        "TECH_ADMIN"     | "/api/analysis/sub_x/status"
        "BUSINESS_ADMIN" | "/api/users"
        "TECH_ADMIN"     | "/api/users"
        "BUSINESS_ADMIN" | "/api/account"
        "TECH_ADMIN"     | "/api/account"
    }
}
