package com.emat.reapi

import com.emat.reapi.clienttest.infra.ClientTestAnswerDocument
import com.emat.reapi.clienttest.infra.ClientTestDocument
import com.emat.reapi.fptest.infra.FpTestDocument
import com.emat.reapi.fptest.infra.FpTestStatementDocument
import com.emat.reapi.statement.domain.StatementProfile
import com.emat.reapi.statement.domain.StatementType
import com.emat.reapi.statement.domain.StatementTypeDefinition
import com.emat.reapi.statement.infra.StatementDefinitionDocument
import com.emat.reapi.submission.domain.SubmissionStatus
import com.emat.reapi.submission.infra.SubmissionDocument

import java.time.Instant

/**
 * Extends {@link BaseIntegrationSpec} with reusable seed helpers for the common
 * document types (definition, fptest, submission, clientTest).
 *
 * Intended for specs that need to compose multiple document types in a single test
 * (e.g. ProfilerControllerSpec, ProfileAnalysisControllerSpec).
 *
 * Existing specs keep their own local helpers to avoid touching green tests.
 */
abstract class BaseSeededIntegrationSpec extends BaseIntegrationSpec {

    protected void seedDefinition(String statementId, StatementProfile category, String statementKey) {
        def doc = new StatementDefinitionDocument(statementId, statementKey, category, [
                new StatementTypeDefinition(StatementType.LIMITING, "ograniczajace " + statementId),
                new StatementTypeDefinition(StatementType.SUPPORTING, "wspierajace " + statementId)
        ])
        mongoTemplate.insert(doc).block()
    }

    protected FpTestDocument seedFpTest(String testId, List<String> statementKeys) {
        def doc = new FpTestDocument()
        doc.testId = testId
        doc.testName = "Test " + testId
        doc.fpTestStatementDocuments = statementKeys.collect { key ->
            new FpTestStatementDocument(key, "opis " + key, StatementProfile.PROFIL_1.plName)
        }
        mongoTemplate.insert(doc).block()
        return doc
    }

    protected SubmissionDocument seedSubmission(String submissionId, String testId, String publicToken,
                                                SubmissionStatus status = SubmissionStatus.OPEN,
                                                Instant expireAt = Instant.now().plusSeconds(7 * 24 * 60 * 60)) {
        def doc = new SubmissionDocument()
        doc.submissionId = submissionId
        doc.testId = testId
        doc.publicToken = publicToken
        doc.clientId = "client-1"
        doc.clientName = "Jan Kowalski"
        doc.clientEmail = "jan@example.com"
        doc.orderId = "order-1_" + UUID.randomUUID()
        doc.status = status
        doc.durationDays = 7
        doc.expireAt = expireAt
        mongoTemplate.insert(doc).block()
        return doc
    }

    /**
     * Seeds a completed client test (the record created after a client submits answers).
     * This is the document read by ProfilerController scoring endpoints.
     */
    protected ClientTestDocument seedClientTest(String testSubmissionPublicId,
                                                String submissionId,
                                                String testId,
                                                List<ClientTestAnswerDocument> answers) {
        def doc = new ClientTestDocument()
        doc.testSubmissionPublicId = testSubmissionPublicId
        doc.submissionId = submissionId
        doc.clientId = "client-1"
        doc.clientName = "Anna Testowa"
        doc.clientEmail = "anna@example.com"
        doc.testId = testId
        doc.testName = "Test " + testId
        doc.submissionDate = Instant.now()
        doc.publicToken = "pt_" + testSubmissionPublicId
        doc.answers = answers
        mongoTemplate.insert(doc).block()
        return doc
    }

    protected static ClientTestAnswerDocument answer(String questionKey, StatementProfile category, int scoring) {
        new ClientTestAnswerDocument(
                questionKey,
                category,
                "ograniczajace " + questionKey,
                "wspierajace " + questionKey,
                scoring
        )
    }
}
