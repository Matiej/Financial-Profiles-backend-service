package com.emat.reapi.profileanalysis

import com.emat.reapi.clienttest.domain.ClientTestAnswer
import com.emat.reapi.clienttest.domain.ClientTestSubmission
import com.emat.reapi.statement.domain.StatementProfile
import com.emat.reapi.statement.domain.StatementType
import spock.lang.Specification

import java.time.Instant

/**
 * Pins the scoring -> evidence mapping that feeds the AI pipeline after the Tally removal.
 *
 * Rules (the single semantic decision of the refactor):
 *  - scoring < 0  -> one active LIMITING statement (limitingDescription)
 *  - scoring > 0  -> one active SUPPORTING statement (supportingDescription)
 *  - scoring == 0 -> no evidence
 *  - only categories actually present in the answers are emitted.
 */
class ClientTestProfiledMapperSpec extends Specification {

    private static ClientTestAnswer answer(String key, StatementProfile category, int scoring) {
        new ClientTestAnswer(key, category, "lim-" + key, "sup-" + key, scoring)
    }

    private static ClientTestSubmission submission(List<ClientTestAnswer> answers) {
        def s = new ClientTestSubmission()
        s.clientName = "Anna"
        s.clientId = "client-1"
        s.submissionId = "sub-1"
        s.submissionDate = Instant.parse("2025-06-01T10:00:00Z")
        s.testId = "fpt-1"
        s.testName = "Test 1"
        s.clientTestAnswerList = answers
        return s
    }

    def "negative scoring becomes an active LIMITING statement"() {
        when:
        def result = ClientTestProfiledMapper.toProfiled(submission([
                answer("p1_q1", StatementProfile.PROFIL_1, -2)
        ]))

        then:
        def block = result.profiledCategoryClientStatementsList[0]
        block.totalLimiting == 1
        block.totalSupporting == 0
        block.profiledStatementList.size() == 1
        block.profiledStatementList[0].description == "lim-p1_q1"
        block.profiledStatementList[0].type == StatementType.LIMITING
        block.profiledStatementList[0].status == true
    }

    def "positive scoring becomes an active SUPPORTING statement"() {
        when:
        def result = ClientTestProfiledMapper.toProfiled(submission([
                answer("p1_q1", StatementProfile.PROFIL_1, 2)
        ]))

        then:
        def block = result.profiledCategoryClientStatementsList[0]
        block.totalSupporting == 1
        block.totalLimiting == 0
        block.profiledStatementList.size() == 1
        block.profiledStatementList[0].description == "sup-p1_q1"
        block.profiledStatementList[0].type == StatementType.SUPPORTING
        block.profiledStatementList[0].status == true
    }

    def "zero scoring contributes no evidence"() {
        when:
        def result = ClientTestProfiledMapper.toProfiled(submission([
                answer("p1_q1", StatementProfile.PROFIL_1, 0)
        ]))

        then:
        def block = result.profiledCategoryClientStatementsList[0]
        block.totalLimiting == 0
        block.totalSupporting == 0
        block.profiledStatementList.isEmpty()
    }

    def "answers are grouped by category with correct per-category counts"() {
        when:
        def result = ClientTestProfiledMapper.toProfiled(submission([
                answer("p1_q1", StatementProfile.PROFIL_1, -1),
                answer("p1_q2", StatementProfile.PROFIL_1, 2),
                answer("p1_q3", StatementProfile.PROFIL_1, 0),
                answer("p2_q1", StatementProfile.PROFIL_2, -2)
        ]))

        then:
        result.profiledCategoryClientStatementsList.size() == 2

        and:
        def p1 = result.profiledCategoryClientStatementsList.find { it.category.categoryName == "PROFIL_1" }
        p1.totalLimiting == 1
        p1.totalSupporting == 1
        p1.profiledStatementList.size() == 2   // the scoring==0 answer adds nothing

        and:
        def p2 = result.profiledCategoryClientStatementsList.find { it.category.categoryName == "PROFIL_2" }
        p2.totalLimiting == 1
        p2.totalSupporting == 0
    }

    def "only categories present in the answers are emitted (not all eight profiles)"() {
        when:
        def result = ClientTestProfiledMapper.toProfiled(submission([
                answer("p1_q1", StatementProfile.PROFIL_1, -1)
        ]))

        then:
        result.profiledCategoryClientStatementsList.size() == 1
        result.profiledCategoryClientStatementsList[0].category.categoryName == "PROFIL_1"
        result.profiledCategoryClientStatementsList[0].category.description == "Strażniczka Braku"
    }

    def "maps top-level fields from the submission"() {
        when:
        def result = ClientTestProfiledMapper.toProfiled(submission([
                answer("p1_q1", StatementProfile.PROFIL_1, -1)
        ]))

        then:
        result.clientName == "Anna"
        result.clientId == "client-1"
        result.submissionId == "sub-1"
        result.testName == "Test 1"
        result.submissionDate == Instant.parse("2025-06-01T10:00:00Z")
    }

    def "handles a null answer list as an empty payload"() {
        when:
        def result = ClientTestProfiledMapper.toProfiled(submission(null))

        then:
        result.profiledCategoryClientStatementsList.isEmpty()
        result.clientName == "Anna"
    }
}
