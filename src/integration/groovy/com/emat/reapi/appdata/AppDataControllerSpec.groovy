package com.emat.reapi.appdata

import com.emat.reapi.BaseIntegrationSpec

class AppDataControllerSpec extends BaseIntegrationSpec {

    def "GET /api/appdata/version returns application name and version"() {
        when:
        def result = authenticatedGet("/api/appdata/version", "TECH_ADMIN")
                .exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.name').isEqualTo("profiler-service-test")
                .jsonPath('$.version').isEqualTo("0.0.1-TEST")
    }

    def "GET /api/appdata/version without auth returns 401"() {
        when:
        def result = webTestClient.get().uri("/api/appdata/version")
                .exchange()

        then:
        result.expectStatus().isUnauthorized()
    }

    def "GET /api/appdata/version with BUSINESS_ADMIN role returns 200"() {
        when:
        def result = authenticatedGet("/api/appdata/version", "BUSINESS_ADMIN")
                .exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.name').exists()
    }

    def "GET /api/appdata/version with CALCULATOR_USER role returns 403"() {
        when:
        def result = authenticatedGet("/api/appdata/version", "CALCULATOR_USER")
                .exchange()

        then:
        result.expectStatus().isForbidden()
    }
}
