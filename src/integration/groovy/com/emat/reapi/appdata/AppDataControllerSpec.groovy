package com.emat.reapi.appdata

import com.emat.reapi.BaseIntegrationSpec

class AppDataControllerSpec extends BaseIntegrationSpec {

    def "should return application name and version"() {
        when:
        def result = authenticatedGet("/api/appdata/version", "TECH_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.name == "profiler-service-test"
        result.version == "0.0.1-TEST"
    }

    def "should return 401 without authentication"() {
        when:
        def response = webTestClient.get().uri("/api/appdata/version").exchange()

        then:
        response.expectStatus().isUnauthorized()
    }

    def "should return 200 for BUSINESS_ADMIN role"() {
        when:
        def result = authenticatedGet("/api/appdata/version", "BUSINESS_ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.name != null
    }

    def "should return 403 for CALCULATOR_USER role"() {
        when:
        def response = authenticatedGet("/api/appdata/version", "CALCULATOR_USER").exchange()

        then:
        response.expectStatus().isForbidden()
    }
}
