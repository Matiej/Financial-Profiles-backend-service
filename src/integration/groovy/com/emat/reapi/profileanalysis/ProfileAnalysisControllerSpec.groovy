package com.emat.reapi.profileanalysis

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.profileanalysis.domain.PayloadMode
import com.emat.reapi.profileanalysis.domain.reportjob.ReportJobStatus
import com.emat.reapi.profileanalysis.infra.InsightReportDocument
import com.emat.reapi.profileanalysis.infra.InsightReportStructuredAiDocument
import com.emat.reapi.profileanalysis.infra.ReportJobDocument
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import spock.lang.Unroll

import java.time.Instant

/**
 * Characterization tests pinning the CURRENT behavior of
 * {@code ProfileAnalysisReportController} before any refactor.
 *
 * Scope note — the full happy path (enqueue → OpenAI → DONE) is intentionally NOT
 * covered here: the async pipeline reads the client profile from the Tally-based
 * {@code ProfiledService.getClientProfiledStatement}, which is slated for removal.
 * Exercising it would require seeding throwaway Tally data plus a WireMock OpenAI
 * stub and polling a fire-and-forget {@code .subscribe()}. Instead we pin the
 * deterministic, synchronous behaviors by seeding {@code ReportJobDocument} and
 * {@code InsightReportDocument} directly.
 *
 * Notable behaviors pinned here:
 *  - {@code POST /{submissionId}} always returns 202 with a Location header. The
 *    enqueue persists the initial job (status PENDING) SYNCHRONOUSLY as part of the
 *    request, so the job is in the DB before 202 returns — that is why the test can
 *    find it. The analysis itself then runs async (fire-and-forget) and may already
 *    have flipped PENDING→RUNNING (and, with no Tally data, on to FAILED), so we
 *    assert the job's existence and uniqueness, never its persisted status.
 *  - {@code GET /{submissionId}/status} returns 200 with the job status DTO, or
 *    204 No Content when no job exists.
 *  - {@code GET /status} returns the job with the longest remaining lock, or 204.
 *  - {@code GET /{submissionId}} returns the stored reports newest-first.
 */
class ProfileAnalysisControllerSpec extends BaseIntegrationSpec {

    // ---- seed helpers ----

    private ReportJobDocument seedJob(String submissionId, ReportJobStatus status,
                                      Instant expireAt = null, PayloadMode mode = PayloadMode.MINIMAL) {
        def doc = ReportJobDocument.builder()
                .submissionId(submissionId)
                .status(status)
                .mode(mode)
                .expireAt(expireAt)
                .build()
        mongoTemplate.insert(doc).block()
        return doc
    }

    /**
     * Seeds a report and forces its {@code createdAt} to the given instant.
     * {@code @CreatedDate} auditing overwrites the value on insert (it ignores the
     * injectable {@link java.time.Clock}), so we set it explicitly with a follow-up
     * update — {@code @CreatedDate} only fires on creation, not on update.
     */
    private InsightReportDocument seedReport(String submissionId, Instant createdAt) {
        def doc = new InsightReportDocument()
        doc.submissionId = submissionId
        doc.clientId = "client-1"
        doc.clientName = "Anna Testowa"
        doc.testName = "Test " + submissionId
        doc.aiModel = "gpt-test"
        doc.payloadMode = PayloadMode.MINIMAL
        doc.schemaName = "insight"
        doc.schemaVersion = "1"
        doc.reportJson = "{}"
        doc.insightReportStructuredAiDocument = new InsightReportStructuredAiDocument()
        def saved = mongoTemplate.insert(doc).block()
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(saved.id)),
                Update.update("createdAt", createdAt),
                InsightReportDocument).block()
        saved.createdAt = createdAt
        return saved
    }

    // ---- GET /api/analysis/{submissionId} — stored reports ----

    def "should return an empty list when no reports exist for the submission"() {
        when:
        def result = authenticatedGet("/api/analysis/sub_none", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then:
        result.isEmpty()
    }

    def "should return stored reports newest-first for a submission"() {
        given:
        seedReport("sub_reportsId", Instant.parse("2025-01-01T10:00:00Z"))
        seedReport("sub_reportsId", Instant.parse("2025-06-01T10:00:00Z"))
        seedReport("sub_otherId", Instant.parse("2025-03-01T10:00:00Z"))

        when:
        def result = authenticatedGet("/api/analysis/sub_reportsId", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBodyList(Map)
                .returnResult()
                .responseBody

        then: "only the two sub_reportsId reports, newest createdAt first"
        result.size() == 2
        result.every { it.submissionId == "sub_reportsId" }
        result[0].createdAt == "2025-06-01T10:00:00Z"
        result[1].createdAt == "2025-01-01T10:00:00Z"
    }

    // ---- POST /api/analysis/{submissionId} — enqueue ----

    def "should accept an analysis request with 202 and a Location header"() {
        when:
        def result = authenticatedPost("/api/analysis/sub_enqueue", "BUSINESS_ADMIN")
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .returnResult()

        then: "Location points to the job and a job now exists for the submission"
        result.responseHeaders.location.toString().contains("/api/profiler/analysis/jobs/")

        and:
        def jobs = mongoTemplate.findAll(ReportJobDocument).collectList().block()
        jobs.find { it.submissionId == "sub_enqueue" } != null
    }

    def "should remain idempotent when re-enqueuing an active job without force"() {
        given: "an already active (PENDING) job"
        seedJob("sub_active", ReportJobStatus.PENDING)

        when:
        authenticatedPost("/api/analysis/sub_active", "BUSINESS_ADMIN").exchange()
                .expectStatus().isAccepted()

        then: "the unique index keeps a single job for the submission"
        def jobs = mongoTemplate.findAll(ReportJobDocument).collectList().block()
        jobs.count { it.submissionId == "sub_active" } == 1
    }

    // ---- GET /api/analysis/{submissionId}/status ----

    def "should return 204 No Content when no job exists for the submission"() {
        when:
        def response = authenticatedGet("/api/analysis/sub_no_job/status", "BUSINESS_ADMIN").exchange()

        then:
        response.expectStatus().isNoContent()
    }

    def "should return 200 with the job status for an existing job"() {
        given:
        seedJob("sub_done", ReportJobStatus.DONE)

        when:
        def result = authenticatedGet("/api/analysis/sub_done/status", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.submissionId == "sub_done"
        result.status == "DONE"
        result.mode == "MINIMAL"
    }

    def "should report a job as locked with remaining seconds while active with a future expiry"() {
        given: "a PENDING job locked for the next 5 minutes"
        seedJob("sub_locked", ReportJobStatus.PENDING, Instant.now().plusSeconds(300))

        when:
        def result = authenticatedGet("/api/analysis/sub_locked/status", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then:
        result.isLocked == true
        (result.remainingLockSeconds as long) > 0
    }

    // ---- GET /api/analysis/status — longest remaining lock ----

    def "should return 204 No Content for the global status when no locked jobs exist"() {
        when:
        def response = authenticatedGet("/api/analysis/status", "BUSINESS_ADMIN").exchange()

        then:
        response.expectStatus().isNoContent()
    }

    def "should return the job with the longest remaining lock for the global status"() {
        given:
        seedJob("sub_short_lock", ReportJobStatus.PENDING, Instant.now().plusSeconds(60))
        seedJob("sub_long_lock", ReportJobStatus.PENDING, Instant.now().plusSeconds(600))

        when:
        def result = authenticatedGet("/api/analysis/status", "BUSINESS_ADMIN").exchange()
                .expectStatus().isOk()
                .expectBody(Map)
                .returnResult()
                .responseBody

        then: "the job expiring latest wins"
        result.submissionId == "sub_long_lock"
    }

    // ---- security ----

    def "should return 401 for GET /api/analysis/status without a token"() {
        when:
        def response = webTestClient.get().uri("/api/analysis/status").exchange()

        then:
        response.expectStatus().isUnauthorized()
    }

    @Unroll
    def "should map role #role to status #status on the analysis endpoints"() {
        when:
        def response = authenticatedGet("/api/analysis/status", role).exchange()

        then:
        response.expectStatus().isEqualTo(status)

        where:
        role              | status
        "CALCULATOR_USER" | 403
        "BUSINESS_ADMIN"  | 204
        "TECH_ADMIN"      | 204
    }
}
