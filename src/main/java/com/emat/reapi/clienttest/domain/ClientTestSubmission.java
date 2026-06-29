package com.emat.reapi.clienttest.domain;

import com.emat.reapi.statement.domain.ProfileSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientTestSubmission {
    private String id;
    private String testSubmissionPublicId;
    private String clientId;
    private String clientName;
    private String clientEmail;
    private String submissionId;
    private Instant submissionDate;
    private String testId;
    private String testName;
    private String publicToken;
    private List<ClientTestAnswer> clientTestAnswerList;
    private Instant createdAt;
    // Profile labels frozen at save time (profileId -> snapshot). Scoring/AI read labels
    // from here so later edits/deletes of the live profile never rewrite history.
    private Map<String, ProfileSnapshot> profileSnapshots;

    public ClientTestSubmission(String testSubmissionPublicId, String clientId, String clientName, String clientEmail, String submissionId, Instant submissionDate, String testId, String testName, String publicToken, List<ClientTestAnswer> clientTestAnswerList) {
        this.testSubmissionPublicId = testSubmissionPublicId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.submissionId = submissionId;
        this.submissionDate = submissionDate;
        this.testId = testId;
        this.testName = testName;
        this.publicToken = publicToken;
        this.clientTestAnswerList = clientTestAnswerList;
    }


}
