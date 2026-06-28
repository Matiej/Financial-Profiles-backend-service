package com.emat.reapi.ncalculator

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.ncalculator.infra.NumerologyDateCalculatorDocument
import com.emat.reapi.ncalculator.infra.NumerologyPhaseCalculatorDocument
import org.springframework.http.MediaType
import spock.lang.Unroll

/**
 * Phase A: characterization tests pinning the CURRENT behavior of
 * {@code NumerologyCalculatorController} before any refactor.
 *
 * The two clock-dependent fields of /dates ({@code yearOfGlobalEnergy},
 * {@code numerologyYear}) are pinned to exact values via the fixed test Clock.
 */
class NumerologyCalculatorControllerSpec extends BaseIntegrationSpec {

    // --- /phrase -------------------------------------------------------------

    def "should compute vowels, consonants and total vibration for a plain phrase"() {
        when:
        def result = authenticatedPost("/api/ncalculator/phrase", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: "ANIA"])
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.vowelsResult == "11/2"
        result.consonantsResult == "5"
        result.vibration == "16/7"
    }

    def "should compute vibrations for a phrase with Polish letters"() {
        when:
        def result = authenticatedPost("/api/ncalculator/phrase", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: "ŚĆŹ"])
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "Ś=1, Ć=3, Ź=8 -> all consonants; total 12 -> 12/3"
        result.vowelsResult == "0"
        result.consonantsResult == "12/3"
        result.vibration == "12/3"
    }

    def "should treat digits 1:1 and add them to consonants and total vibration"() {
        when:
        def result = authenticatedPost("/api/ncalculator/phrase", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: "AB12"])
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "A(1) vowel; B(2)+1+2 consonants=5; total=6"
        result.vowelsResult == "1"
        result.consonantsResult == "5"
        result.vibration == "6"
    }

    def "should reject an empty phrase with 400 validation error"() {
        when:
        def result = authenticatedPost("/api/ncalculator/phrase", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: ""])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
        result.details.phrase != null
    }

    def "should reject a null phrase with 400 illegal argument raised by the service"() {
        when: "null is valid for @Pattern, so the request reaches the service which errors"
        def result = authenticatedPost("/api/ncalculator/phrase", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: null])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "ILLEGAL_ARGUMENT"
    }

    def "should reject a phrase with disallowed characters with 400 validation error"() {
        when:
        def result = authenticatedPost("/api/ncalculator/phrase", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: "test@!"])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
    }

    def "should reject a phrase longer than 100 characters with 400 validation error"() {
        when:
        def result = authenticatedPost("/api/ncalculator/phrase", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: "A" * 101])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
    }

    def "should persist exactly one phrase calculation document"() {
        when:
        authenticatedPost("/api/ncalculator/phrase", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: "ANIA"])
                .exchange()
                .expectStatus().isOk()

        then:
        def saved = mongoTemplate.findAll(NumerologyPhaseCalculatorDocument).collectList().block()
        saved.size() == 1
        saved[0].requestedPhase == "ANIA"
        saved[0].vibration == "16/7"
        saved[0].userName == "test-user"
    }

    // --- /dates --------------------------------------------------------------

    def "should compute the deterministic date fields from the request"() {
        when:
        def result = authenticatedPost("/api/ncalculator/dates", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([birthDate: "1978-05-21", referenceDate: "2025-11-11"])
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "fields derived from the request, plus the clock-dependent ones pinned by the fixed test Clock (2025-11-11)"
        result.mainVibration == "33/6"
        result.personalYear == 8
        result.personalMonth == 1
        result.worldDayVibration == 4
        result.personalDay == 3
        // fixed clock -> MoonVirgo entry covering 2025-11-11 has yearVibration 1
        result.yearOfGlobalEnergy == 1
        // numerologyYear = reduce(reduce(mainVibrationDigits=6) + yearOfGlobalEnergy=1) = 7
        result.numerologyYear == 7
    }

    def "should reject a regex-valid but impossible date with 400 illegal argument"() {
        when: "matches the yyyy-MM-dd pattern but LocalDate.parse fails"
        def result = authenticatedPost("/api/ncalculator/dates", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([birthDate: "2026-13-99", referenceDate: "2025-11-11"])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "ILLEGAL_ARGUMENT"
    }

    def "should reject a wrongly formatted date with 400 validation error"() {
        when:
        def result = authenticatedPost("/api/ncalculator/dates", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([birthDate: "21-05-1978", referenceDate: "2025-11-11"])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
        result.details.birthDate != null
    }

    def "should reject a missing birthDate with 400 validation error"() {
        when:
        def result = authenticatedPost("/api/ncalculator/dates", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([referenceDate: "2025-11-11"])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
        result.details.birthDate != null
    }

    def "should reject a missing referenceDate with 400 validation error"() {
        when:
        def result = authenticatedPost("/api/ncalculator/dates", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([birthDate: "1978-05-21"])
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"
        result.details.referenceDate != null
    }

    def "should persist exactly one dates calculation document"() {
        when:
        authenticatedPost("/api/ncalculator/dates", "CALCULATOR_USER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([birthDate: "1978-05-21", referenceDate: "2025-11-11"])
                .exchange()
                .expectStatus().isOk()

        then:
        def saved = mongoTemplate.findAll(NumerologyDateCalculatorDocument).collectList().block()
        saved.size() == 1
        saved[0].requestedBrithDate == "1978-05-21"
        saved[0].mainVibration == "33/6"
        saved[0].userName == "test-user"
    }

    // --- security ------------------------------------------------------------

    @Unroll
    def "should return 401 for POST #uri without a token"() {
        when:
        def response = webTestClient.post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()

        then:
        response.expectStatus().isUnauthorized()

        where:
        uri                          | body
        "/api/ncalculator/phrase"    | [phrase: "ANIA"]
        "/api/ncalculator/dates"     | [birthDate: "1978-05-21", referenceDate: "2025-11-11"]
    }

    @Unroll
    def "should grant #role access to #uri"() {
        when:
        def response = authenticatedPost(uri, role)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()

        then:
        response.expectStatus().isOk()

        where:
        role             | uri                        | body
        "CALCULATOR_USER" | "/api/ncalculator/phrase" | [phrase: "ANIA"]
        "BUSINESS_ADMIN"  | "/api/ncalculator/phrase" | [phrase: "ANIA"]
        "TECH_ADMIN"      | "/api/ncalculator/phrase" | [phrase: "ANIA"]
        "CALCULATOR_USER" | "/api/ncalculator/dates"  | [birthDate: "1978-05-21", referenceDate: "2025-11-11"]
        "BUSINESS_ADMIN"  | "/api/ncalculator/dates"  | [birthDate: "1978-05-21", referenceDate: "2025-11-11"]
        "TECH_ADMIN"      | "/api/ncalculator/dates"  | [birthDate: "1978-05-21", referenceDate: "2025-11-11"]
    }

    def "should return 403 for an unrelated role"() {
        when:
        def response = authenticatedPost("/api/ncalculator/phrase", "SOME_OTHER")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([phrase: "ANIA"])
                .exchange()

        then:
        response.expectStatus().isForbidden()
    }
}
