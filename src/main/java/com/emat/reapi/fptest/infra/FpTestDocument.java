package com.emat.reapi.fptest.infra;

import com.emat.reapi.fptest.domain.FpTest;
import com.emat.reapi.fptest.domain.FpTestStatement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(FpTestDocument.COLLECTION_NAME)
@TypeAlias(FpTestDocument.COLLECTION_NAME)
public class FpTestDocument {
    public static final String COLLECTION_NAME = "profiler_tests";

    @Id
    private String id;
    @Indexed(name = "testId_idx", unique = true, background = true)
    private String testId;
    private String testName;
    private String descriptionBefore;
    private String descriptionAfter;
    private List<FpTestStatementDocument> fpTestStatementDocuments;
    // Soft-delete: hidden from admin lists; resolve by testId stays unfiltered so
    // completed-history views keep working. Guarded by open, non-expired submissions.
    private boolean isDeleted;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @Version
    private Long version;

    public static FpTestDocument fromDomain(FpTest domain) {
        FpTestDocument doc = new FpTestDocument();
        doc.setId(domain.id());
        doc.setTestId(domain.testId());
        doc.setTestName(domain.testName());
        doc.setDescriptionBefore(domain.descriptionBefore());
        doc.setDescriptionAfter(domain.descriptionAfter());
        doc.setCreatedAt(domain.createdAt());
        doc.setUpdatedAt(domain.updatedAt());
        doc.setFpTestStatementDocuments(domain.fpTestStatements()
                .stream()
                .map(st -> new FpTestStatementDocument(
                                st.statementKey(),
                                st.statementsDescription(),
                                st.statementsProfile(),
                                st.profileId()
                        )
                )
                .toList());
        return doc;
    }

    public FpTest toDomain() {
        List<FpTestStatement> fpTestStatements = this.getFpTestStatementDocuments()
                .stream()
                .map(stDoc -> new FpTestStatement(
                                stDoc.statementKey(),
                                stDoc.statementsDescription(),
                                stDoc.statementsProfile(),
                                stDoc.profileId()
                        )
                )
                .toList();
        return new FpTest(
                id,
                testId,
                testName,
                descriptionBefore,
                descriptionAfter,
                fpTestStatements,
                createdAt,
                updatedAt
        );
    }
}
