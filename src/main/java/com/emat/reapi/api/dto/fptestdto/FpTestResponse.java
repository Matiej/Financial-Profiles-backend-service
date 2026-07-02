package com.emat.reapi.api.dto.fptestdto;

import com.emat.reapi.fptest.domain.FpTest;

import java.util.List;

public record FpTestResponse(
        String testId,
        String testName,
        String descriptionBefore,
        String descriptionAfter,
        List<FpTestStatementDto> fpTestStatementDtoList,
        List<String> submissionIds,
        // Submission breakdown for the FE delete-confirmation dialog: deleting a test with
        // DONE/expired submissions succeeds (only a live OPEN token blocks — 409), so the FE
        // must warn the admin first. Computed from the submission list, no extra queries.
        long submissionsDone,
        long submissionsOpenActive,
        long submissionsOpenExpired,
        String createdAt,
        String updatedAt
) {

    public static FpTestResponse toResponse(
            FpTest domain,
            List<String> submissions,
            long submissionsDone,
            long submissionsOpenActive,
            long submissionsOpenExpired
    ) {
        String createdAt = domain.createdAt() != null ? domain.createdAt().toString() : null;
        String updatedAt = domain.updatedAt() != null ? domain.updatedAt().toString() : null;

        return new FpTestResponse(
                domain.testId(),
                domain.testName(),
                domain.descriptionBefore(),
                domain.descriptionAfter(),
                domain.fpTestStatements()
                        .stream()
                        .map(FpTestStatementDto::fromDomain)
                        .toList(),
                submissions,
                submissionsDone,
                submissionsOpenActive,
                submissionsOpenExpired,
                createdAt,
                updatedAt
        );
    }
}
