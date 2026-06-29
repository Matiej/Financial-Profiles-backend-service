package com.emat.reapi.profileanalysis;

import com.emat.reapi.clienttest.domain.ClientTestAnswer;
import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import com.emat.reapi.profiler.domain.ProfileCategory;
import com.emat.reapi.profiler.domain.ProfiledCategoryClientStatements;
import com.emat.reapi.profiler.domain.ProfiledClientAnswerDetails;
import com.emat.reapi.profiler.domain.ProfiledStatement;
import com.emat.reapi.statement.domain.ProfileSnapshot;
import com.emat.reapi.statement.domain.StatementType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a completed client test ({@link ClientTestSubmission}) into the AI input model
 * ({@link ProfiledClientAnswerDetails}) consumed by MinimizerService / ProfileAiAnalysisProcessor.
 *
 * <p>This is the single point that translates the FpTest/ClientTest scoring model into the
 * statement-based model the AI pipeline expects, replacing the old Tally-based source.
 *
 * <p>Scoring &rarr; evidence (the only semantic decision in the Tally removal):
 * <ul>
 *   <li>{@code scoring < 0} &rarr; LIMITING belief active ({@code limitingDescription}, status = TRUE)</li>
 *   <li>{@code scoring > 0} &rarr; SUPPORTING belief active ({@code supportingDescription}, status = TRUE)</li>
 *   <li>{@code scoring == 0} &rarr; neutral, contributes no evidence</li>
 * </ul>
 * Per category: {@code totalLimiting} = #answers with scoring&lt;0, {@code totalSupporting} = #answers with scoring&gt;0.
 * Only categories actually present in the answers are emitted (empty profiles are not injected).
 */
final class ClientTestProfiledMapper {

    private ClientTestProfiledMapper() {
    }

    static ProfiledClientAnswerDetails toProfiled(ClientTestSubmission submission) {
        List<ClientTestAnswer> answers = submission.getClientTestAnswerList() == null
                ? List.of()
                : submission.getClientTestAnswerList();

        Map<String, ProfileSnapshot> snapshots = submission.getProfileSnapshots() == null
                ? Map.of()
                : submission.getProfileSnapshots();

        // preserve first-seen profile order for stable output
        Map<String, List<ClientTestAnswer>> byProfile = new LinkedHashMap<>();
        for (ClientTestAnswer a : answers) {
            byProfile.computeIfAbsent(a.profileId(), k -> new ArrayList<>()).add(a);
        }

        List<ProfiledCategoryClientStatements> categories = byProfile.entrySet().stream()
                .map(e -> toCategoryBlock(e.getKey(), e.getValue(), snapshots.get(e.getKey())))
                .toList();

        return new ProfiledClientAnswerDetails(
                submission.getClientName(),
                submission.getClientId(),
                submission.getSubmissionId(),
                submission.getSubmissionDate(),
                submission.getTestName(),
                categories
        );
    }

    private static ProfiledCategoryClientStatements toCategoryBlock(String profileId,
                                                                    List<ClientTestAnswer> answers,
                                                                    ProfileSnapshot snapshot) {
        List<ProfiledStatement> statements = new ArrayList<>();
        int totalLimiting = 0;
        int totalSupporting = 0;

        for (ClientTestAnswer a : answers) {
            if (a.scoring() < 0) {
                statements.add(new ProfiledStatement(a.limitingDescription(), StatementType.LIMITING, true));
                totalLimiting++;
            } else if (a.scoring() > 0) {
                statements.add(new ProfiledStatement(a.supportingDescription(), StatementType.SUPPORTING, true));
                totalSupporting++;
            }
            // scoring == 0 -> no evidence
        }

        // Display name from the snapshot frozen at save time; fall back to the raw profileId.
        String displayName = snapshot != null ? snapshot.plName() : profileId;
        ProfileCategory profileCategory = new ProfileCategory(profileId, displayName);
        return new ProfiledCategoryClientStatements(profileCategory, totalLimiting, totalSupporting, statements);
    }
}
