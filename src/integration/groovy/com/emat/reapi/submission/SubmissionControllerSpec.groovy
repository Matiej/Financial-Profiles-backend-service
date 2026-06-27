package com.emat.reapi.submission

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.submission.domain.SubmissionStatus
import com.emat.reapi.submission.infra.SubmissionDocument
import org.springframework.http.MediaType
import spock.lang.Unroll

import java.time.Instant

/**
 * Characterization tests pinning the CURRENT behavior of
 * {@code SubmissionController} before any refactor.
 *
 * Notable divergences from the original plan, intentionally pinned here:
 *  - {@code GET /{id}} of a missing submission returns 400 ({@code SUBMISSION_NOT_FOUND}),
 *    NOT 404 — {@code GlobalExceptionHandler} maps every {@code SubmissionException} to 400.
 *  - {@code DELETE /{id}} of a missing submission is a no-op returning 202 (no error path).
 *  - The "duplicate orderId" conflict cannot occur: the service always appends a fresh
 *    {@code _UUID} suffix to the incoming orderId, so stored orderIds are globally unique.
 *  - {@code PUT /{id}} updates clientName/testId/durationDays but IGNORES clientEmail,
 *    even though the DTO requires it.
 */
class SubmissionControllerSpec extends BaseIntegrationSpec {

    private static Map submissionPayload(Map overrides = [:]) {
        ([
                clientId    : "client-1",
                clientName  : "Jan Kowalski",
                clientEmail : "jan@example.com",
                orderId     : "order-1",
                testId      : "test-1",
                durationDays: 7
        ] + overrides)
    }

    private SubmissionDocument seedSubmission(String submissionId, SubmissionStatus status, Map overrides = [:]) {
        def doc = new SubmissionDocument()
        doc.submissionId = submissionId
        doc.orderId = (overrides.orderId ?: "order_" + submissionId) + "_" + UUID.randomUUID()
        doc.clientId = "client-1"
        doc.clientName = "Jan Kowalski"
        doc.clientEmail = "jan@example.com"
        doc.testId = overrides.testId ?: "test-1"
        doc.status = status
        doc.durationDays = (overrides.durationDays ?: 7) as int
        doc.publicToken = "pt_" + UUID.randomUUID()
        doc.expireAt = overrides.expireAt ?: Instant.now().plusSeconds(7 * 24 * 60 * 60)
        if (overrides.createdAt) {
            doc.createdAt = overrides.createdAt as Instant
        }
        mongoTemplate.insert(doc).block()
        return doc
    }

    def "should create a submission, generate ids and persist it as OPEN"() {
        when:
        def result = authenticatedPost("/api/submission", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(submissionPayload())
                .exchange()

        then: "the controller echoes the created submission with 201"
        result.expectStatus().isCreated()
                .expectBody()
                .jsonPath('$.submissionId').value({ it.startsWith("sub_") })
                .jsonPath('$.publicToken').value({ it.startsWith("pt_") })
                .jsonPath('$.clientId').isEqualTo("client-1")
                .jsonPath('$.clientName').isEqualTo("Jan Kowalski")
                .jsonPath('$.clientEmail').isEqualTo("jan@example.com")
                .jsonPath('$.testId').isEqualTo("test-1")
                .jsonPath('$.status').isEqualTo("OPEN")
                .jsonPath('$.orderId').value({ it.startsWith("order-1_") })
                .jsonPath('$.remainingSeconds').value({ (it as long) > 0 })

        and: "exactly one OPEN document is stored"
        def saved = mongoTemplate.findAll(SubmissionDocument).collectList().block()
        saved.size() == 1
        saved[0].submissionId.startsWith("sub_")
        saved[0].publicToken.startsWith("pt_")
        saved[0].orderId.startsWith("order-1_")
        saved[0].status == SubmissionStatus.OPEN
        saved[0].durationDays == 7
        saved[0].expireAt != null
    }

    @Unroll
    def "should return 400 for invalid create payload: #scenario"() {
        expect:
        authenticatedPost("/api/submission", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(submissionPayload(overrides))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.code').isEqualTo("VALIDATION_ERROR")

        where:
        scenario                | overrides
        "blank clientEmail"     | [clientEmail: ""]
        "invalid email format"  | [clientEmail: "not-an-email"]
        "blank clientId"        | [clientId: ""]
        "blank clientName"      | [clientName: ""]
        "blank orderId"         | [orderId: ""]
        "blank testId"          | [testId: ""]
        "durationDays below min"| [durationDays: 0]
        "durationDays above max"| [durationDays: 100]
    }

    def "should return all submissions ordered by createdAt descending"() {
        given:
        seedSubmission("sub_older", SubmissionStatus.OPEN, [createdAt: Instant.parse("2025-01-01T10:00:00Z")])
        seedSubmission("sub_newer", SubmissionStatus.OPEN, [createdAt: Instant.parse("2025-06-01T10:00:00Z")])

        when:
        def result = authenticatedGet("/api/submission", "BUSINESS_ADMIN").exchange()

        then: "newest first"
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.length()').isEqualTo(2)
                .jsonPath('$[0].submissionId').isEqualTo("sub_newer")
                .jsonPath('$[1].submissionId').isEqualTo("sub_older")
    }

    def "should return a submission by submissionId"() {
        given:
        seedSubmission("sub_find_me", SubmissionStatus.OPEN)

        when:
        def result = authenticatedGet("/api/submission/sub_find_me", "BUSINESS_ADMIN").exchange()

        then:
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.submissionId').isEqualTo("sub_find_me")
                .jsonPath('$.status').isEqualTo("OPEN")
    }

    def "should return 400 SUBMISSION_NOT_FOUND for an unknown submissionId (not 404)"() {
        expect:
        authenticatedGet("/api/submission/sub_missing", "BUSINESS_ADMIN").exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.code').isEqualTo("SUBMISSION_NOT_FOUND")
    }

    def "should update an OPEN submission and return 200"() {
        given:
        seedSubmission("sub_update", SubmissionStatus.OPEN)

        when:
        def result = authenticatedPut("/api/submission/sub_update", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        clientName  : "Anna Nowak",
                        clientEmail : "anna@example.com",
                        testId      : "test-2",
                        durationDays: 14
                ])
                .exchange()

        then: "clientName, testId and durationDays are updated"
        result.expectStatus().isOk()
                .expectBody()
                .jsonPath('$.clientName').isEqualTo("Anna Nowak")
                .jsonPath('$.testId').isEqualTo("test-2")

        and: "the stored document reflects the update but keeps the original clientEmail (service ignores it)"
        def saved = mongoTemplate.findAll(SubmissionDocument).collectList().block()
        saved.size() == 1
        saved[0].clientName == "Anna Nowak"
        saved[0].testId == "test-2"
        saved[0].durationDays == 14
        saved[0].clientEmail == "jan@example.com"
    }

    def "should return 409 when updating a DONE submission"() {
        given:
        seedSubmission("sub_done", SubmissionStatus.DONE)

        expect:
        authenticatedPut("/api/submission/sub_done", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        clientName  : "Anna Nowak",
                        clientEmail : "anna@example.com",
                        testId      : "test-2",
                        durationDays: 14
                ])
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath('$.code').isEqualTo("SUBMISSION_UPDATE_ERROR")
    }

    def "should return 400 when updating an unknown submission"() {
        expect:
        authenticatedPut("/api/submission/sub_missing", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        clientName  : "Anna Nowak",
                        clientEmail : "anna@example.com",
                        testId      : "test-2",
                        durationDays: 14
                ])
                .exchange()
                .expectStatus().isBadRequest()
    }

    def "should close an OPEN submission, returning 202 and status DONE"() {
        given:
        seedSubmission("sub_close", SubmissionStatus.OPEN)

        when:
        def result = authenticatedPut("/api/submission/sub_close/close", "BUSINESS_ADMIN").exchange()

        then:
        result.expectStatus().isEqualTo(202)
                .expectBody()
                .jsonPath('$.status').isEqualTo("DONE")
                .jsonPath('$.remainingSeconds').isEqualTo(0)

        and:
        def saved = mongoTemplate.findAll(SubmissionDocument).collectList().block()
        saved.size() == 1
        saved[0].status == SubmissionStatus.DONE
    }

    def "should delete an OPEN submission, returning 202"() {
        given:
        seedSubmission("sub_delete", SubmissionStatus.OPEN)

        when:
        def result = authenticatedDelete("/api/submission/sub_delete", "BUSINESS_ADMIN").exchange()

        then:
        result.expectStatus().isEqualTo(202)

        and:
        mongoTemplate.findAll(SubmissionDocument).collectList().block().isEmpty()
    }

    def "should return 409 when deleting a DONE submission"() {
        given:
        seedSubmission("sub_delete_done", SubmissionStatus.DONE)

        expect:
        authenticatedDelete("/api/submission/sub_delete_done", "BUSINESS_ADMIN").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath('$.code').isEqualTo("SUBMISSION_DELETE_ERROR")

        and: "the document is still present"
        mongoTemplate.findAll(SubmissionDocument).collectList().block().size() == 1
    }

    def "should treat deleting an unknown submission as a no-op returning 202"() {
        expect:
        authenticatedDelete("/api/submission/sub_missing", "BUSINESS_ADMIN").exchange()
                .expectStatus().isEqualTo(202)
    }

    def "should return 401 for GET /api/submission without a token"() {
        expect:
        webTestClient.get().uri("/api/submission")
                .exchange()
                .expectStatus().isUnauthorized()
    }

    @Unroll
    def "should map role #role to status #status on the submission endpoints"() {
        expect:
        authenticatedGet("/api/submission", role)
                .exchange()
                .expectStatus().isEqualTo(status)

        where:
        role              | status
        "CALCULATOR_USER" | 403
        "BUSINESS_ADMIN"  | 200
        "TECH_ADMIN"      | 200
    }
}
