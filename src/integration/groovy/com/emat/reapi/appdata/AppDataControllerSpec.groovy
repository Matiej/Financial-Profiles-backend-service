package com.emat.reapi.appdata

import com.emat.reapi.BaseIntegrationSpec

class AppDataControllerSpec extends BaseIntegrationSpec {

    def "should return application name and version"() {
        when:
        def result = authenticatedGet("/api/appdata/version", "TECH_ADMIN")
                .exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.name').isEqualTo("profiler-service-test")
                .jsonPath('$.version').isEqualTo("0.0.1-TEST")
    }

    def "should return 401 without authentication"() {
        when:
        def result = webTestClient.get().uri("/api/appdata/version")
                .exchange()

        then:
        result.expectStatus().isUnauthorized()
    }

    def "should return 200 for BUSINESS_ADMIN role"() {
        when:
        def result = authenticatedGet("/api/appdata/version", "BUSINESS_ADMIN")
                .exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.name').exists()
    }

    def "should return 403 for CALCULATOR_USER role"() {
        when:
        def result = authenticatedGet("/api/appdata/version", "CALCULATOR_USER")
                .exchange()

        then:
        result.expectStatus().isForbidden()
    }
}
