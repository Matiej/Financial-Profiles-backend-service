package com.emat.reapi.submission

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.submission.domain.SubmissionStatus
import com.emat.reapi.submission.infra.SubmissionDocument
import org.springframework.http.MediaType
import spock.lang.Unroll

import java.time.Instant

/**
 * Characterization tests for {@code SubmissionController}.
 *
 * Notable design decisions:
 *  - {@code clientEmail} is optional on both create and update; when provided it must be a valid e-mail.
 *  - {@code GET /{id}} of a missing submission returns 404 ({@code SUBMISSION_NOT_FOUND}).
 *  - {@code DELETE /{id}} of a missing submission is a no-op returning 202 (no error path).
 *  - The "duplicate orderId" conflict cannot occur: the service always appends a fresh
 *    {@code _UUID} suffix to the incoming orderId, so stored orderIds are globally unique.
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
                .expectStatus().isCreated()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "the controller echoes the created submission"
        result.submissionId.startsWith("sub_")
        result.publicToken.startsWith("pt_")
        result.clientId == "client-1"
        result.clientName == "Jan Kowalski"
        result.clientEmail == "jan@example.com"
        result.testId == "test-1"
        result.status == "OPEN"
        result.orderId.startsWith("order-1_")
        (result.remainingSeconds as long) > 0

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
        when:
        def result = authenticatedPost("/api/submission", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(submissionPayload(overrides))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "VALIDATION_ERROR"

        where:
        scenario                | overrides
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
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then: "newest first"
        result.size() == 2
        result[0].submissionId == "sub_newer"
        result[1].submissionId == "sub_older"
    }

    def "should return a submission by submissionId"() {
        given:
        seedSubmission("sub_find_me", SubmissionStatus.OPEN)

        when:
        def result = authenticatedGet("/api/submission/sub_find_me", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.submissionId == "sub_find_me"
        result.status == "OPEN"
    }

    def "should return 404 SUBMISSION_NOT_FOUND for an unknown submissionId"() {
        when:
        def result = authenticatedGet("/api/submission/sub_missing", "BUSINESS_ADMIN").exchange()
                .expectStatus().isNotFound()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "SUBMISSION_NOT_FOUND"
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
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "clientName, clientEmail, testId and durationDays are updated"
        result.clientName == "Anna Nowak"
        result.testId == "test-2"

        and: "the stored document reflects all updated fields including clientEmail"
        def saved = mongoTemplate.findAll(SubmissionDocument).collectList().block()
        saved.size() == 1
        saved[0].clientName == "Anna Nowak"
        saved[0].testId == "test-2"
        saved[0].durationDays == 14
        saved[0].clientEmail == "anna@example.com"
    }

    def "should return 409 when updating a DONE submission"() {
        given:
        seedSubmission("sub_done", SubmissionStatus.DONE)

        when:
        def result = authenticatedPut("/api/submission/sub_done", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        clientName  : "Anna Nowak",
                        clientEmail : "anna@example.com",
                        testId      : "test-2",
                        durationDays: 14
                ])
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "SUBMISSION_UPDATE_ERROR"
    }

    def "should return 404 when updating an unknown submission"() {
        when:
        def result = authenticatedPut("/api/submission/sub_missing", "BUSINESS_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue([
                        clientName  : "Anna Nowak",
                        clientEmail : "anna@example.com",
                        testId      : "test-2",
                        durationDays: 14
                ])
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "SUBMISSION_NOT_FOUND"
    }

    def "should close an OPEN submission, returning 202 and status DONE"() {
        given:
        seedSubmission("sub_close", SubmissionStatus.OPEN)

        when:
        def result = authenticatedPut("/api/submission/sub_close/close", "BUSINESS_ADMIN").exchange()
                .expectStatus().isEqualTo(202)
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.status == "DONE"
        result.remainingSeconds == 0

        and:
        def saved = mongoTemplate.findAll(SubmissionDocument).collectList().block()
        saved.size() == 1
        saved[0].status == SubmissionStatus.DONE
    }

    def "should return 404 when closing an unknown submission"() {
        when:
        def result = authenticatedPut("/api/submission/sub_missing/close", "BUSINESS_ADMIN").exchange()
                .expectStatus().isNotFound()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "SUBMISSION_NOT_FOUND"
    }

    def "should delete an OPEN submission, returning 202"() {
        given:
        seedSubmission("sub_delete", SubmissionStatus.OPEN)

        when:
        authenticatedDelete("/api/submission/sub_delete", "BUSINESS_ADMIN").exchange()
                .expectStatus().isEqualTo(202)

        then:
        mongoTemplate.findAll(SubmissionDocument).collectList().block().isEmpty()
    }

    def "should return 409 when deleting a DONE submission"() {
        given:
        seedSubmission("sub_delete_done", SubmissionStatus.DONE)

        when:
        def result = authenticatedDelete("/api/submission/sub_delete_done", "BUSINESS_ADMIN").exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.code == "SUBMISSION_DELETE_ERROR"

        and: "the document is still present"
        mongoTemplate.findAll(SubmissionDocument).collectList().block().size() == 1
    }

    def "should treat deleting an unknown submission as a no-op returning 202"() {
        when:
        def response = authenticatedDelete("/api/submission/sub_missing", "BUSINESS_ADMIN").exchange()

        then:
        response.expectStatus().isEqualTo(202)
    }

    def "should return 401 for GET /api/submission without a token"() {
        when:
        def response = webTestClient.get().uri("/api/submission").exchange()

        then:
        response.expectStatus().isUnauthorized()
    }

    @Unroll
    def "should map role #role to status #status on the submission endpoints"() {
        when:
        def response = authenticatedGet("/api/submission", role).exchange()

        then:
        response.expectStatus().isEqualTo(status)

        where:
        role              | status
        "CALCULATOR_USER" | 403
        "BUSINESS_ADMIN"  | 200
        "TECH_ADMIN"      | 200
    }
}
