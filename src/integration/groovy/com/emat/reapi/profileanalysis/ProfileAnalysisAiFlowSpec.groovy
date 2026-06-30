package com.emat.reapi.profileanalysis

import com.emat.reapi.BaseIntegrationSpec
import com.emat.reapi.clienttest.infra.ClientTestAnswerDocument
import com.emat.reapi.clienttest.infra.ClientTestDocument
import com.emat.reapi.profileanalysis.domain.reportjob.ReportJobStatus
import com.emat.reapi.profileanalysis.infra.InsightReportDocument
import com.emat.reapi.profileanalysis.infra.ReportJobDocument
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Shared
import spock.util.concurrent.PollingConditions

import java.time.Instant

import static com.github.tomakehurst.wiremock.client.WireMock.*

/**
 * End-to-end happy path of the AI analysis flow, AFTER repointing the input from Tally
 * to the FpTest/ClientTest data (Faza A of the Tally removal).
 *
 * Flow: POST /api/analysis/{submissionId} (202) → async pipeline reads the ClientTest by
 * submissionId → ClientTestProfiledMapper → MinimizerService → OpenAI (WireMock) →
 * InsightReport persisted + job DONE.
 *
 * The analysis runs fire-and-forget ({@code .subscribe()}), so we poll with PollingConditions.
 * WireMock stubs the OpenAI chat-completions call; its content is a schema-valid InsightReport.
 */
class ProfileAnalysisAiFlowSpec extends BaseIntegrationSpec {

    private static final ObjectMapper MAPPER = new ObjectMapper()

    @Shared
    static WireMockServer openAi = new WireMockServer(WireMockConfiguration.options().dynamicPort())

    static {
        openAi.start()
    }

    @DynamicPropertySource
    static void openAiProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url") { "http://localhost:${openAi.port()}/v1" }
    }

    def setup() {
        openAi.resetAll()
        stubOpenAiOk()
    }

    private static void stubOpenAiOk() {
        // content must be a schema-valid InsightReport (ai/schemas/open_ai_json_schema_v1.json)
        def insight = MAPPER.writeValueAsString([
                clientSummary   : [
                        keyThemes         : ["temat finansowy"],
                        dominantCategories: [[categoryId: "PROFIL_1", balanceIndex: 0.0, why: "dominuje blokada"]],
                        overallNarrativePl: "Ogólna narracja klienta."
                ],
                categoryInsights: [[
                        categoryId              : "PROFIL_1",
                        categoryLabelPl         : "Strażniczka Braku",
                        strengthsPl             : [],
                        risksPl                 : ["ryzyko nadmiernej kontroli"],
                        contradictionsPl        : [],
                        recommendedInterventions: [[type: "rytuał", titlePl: "Tytuł", whyPl: "Dlaczego", howPl: "Jak"]]
                ]],
                nextSteps       : ["pierwszy krok"]
        ])
        def chatCompletion = MAPPER.writeValueAsString([
                id     : "chatcmpl-test",
                object : "chat.completion",
                created: 1_700_000_000,
                model  : "gpt-4o-mini",
                choices: [[
                        index        : 0,
                        message      : [role: "assistant", content: insight],
                        finish_reason: "stop"
                ]],
                usage  : [prompt_tokens: 1, completion_tokens: 1, total_tokens: 2]
        ])
        openAi.stubFor(post(urlPathMatching(".*/chat/completions"))
                .willReturn(okJson(chatCompletion)))
    }

    private void seedClientTest(String submissionId) {
        def doc = new ClientTestDocument()
        doc.testSubmissionPublicId = "tspi_" + submissionId
        doc.submissionId = submissionId
        doc.clientId = "client-1"
        doc.clientName = "Anna Testowa"
        doc.clientEmail = "anna@example.com"
        doc.testId = "fpt-1"
        doc.testName = "Test finansowy"
        doc.submissionDate = Instant.now()
        doc.publicToken = "pt_" + submissionId
        doc.answers = [
                new ClientTestAnswerDocument("p1_q1", "profil_1", "lim1", "sup1", -2),
                new ClientTestAnswerDocument("p1_q2", "profil_1", "lim2", "sup2", 2)
        ]
        mongoTemplate.insert(doc).block()
    }

    def "should run the full AI analysis flow and persist an InsightReport"() {
        given:
        seedClientTest("sub_ai")

        when: "the analysis is enqueued"
        authenticatedPost("/api/analysis/sub_ai", "BUSINESS_ADMIN").exchange()
                .expectStatus().isAccepted()

        then: "the async pipeline eventually persists the report and marks the job DONE"
        new PollingConditions(timeout: 20, delay: 0.3).eventually {
            def reports = mongoTemplate.findAll(InsightReportDocument).collectList().block()
            reports.size() == 1
            reports[0].submissionId == "sub_ai"
            reports[0].testName == "Test finansowy"

            def job = mongoTemplate.findAll(ReportJobDocument).collectList().block()
                    .find { it.submissionId == "sub_ai" }
            job?.status == ReportJobStatus.DONE
        }

        and: "OpenAI was called"
        openAi.verify(postRequestedFor(urlPathMatching(".*/chat/completions")))
    }

    def "should mark the job FAILED when no client test exists for the submission"() {
        when: "enqueue for a submission with no ClientTest seeded"
        authenticatedPost("/api/analysis/sub_missing", "BUSINESS_ADMIN").exchange()
                .expectStatus().isAccepted()

        then: "the job ends FAILED and OpenAI is never called"
        new PollingConditions(timeout: 10, delay: 0.3).eventually {
            def job = mongoTemplate.findAll(ReportJobDocument).collectList().block()
                    .find { it.submissionId == "sub_missing" }
            job?.status == ReportJobStatus.FAILED
        }

        and:
        openAi.verify(0, postRequestedFor(urlPathMatching(".*/chat/completions")))
    }
}
