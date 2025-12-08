package com.emat.reapi.profileanalysis;

import com.emat.reapi.ai.port.AiInsightAnalyzerService;
import com.emat.reapi.profileanalysis.domain.InsightReport;
import com.emat.reapi.profileanalysis.domain.PayloadMode;
import com.emat.reapi.profiler.domain.ProfiledClientAnswerDetails;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class ProfileAiAnalysisProcessor {
    private final MinimizerService minimizerService;
    private final AiInsightAnalyzerService aiAnalyzer;

    public Mono<InsightReport> sendSubmissionToAI(ProfiledClientAnswerDetails profiledClientAnswerDetails,  PayloadMode mode) {
        return minimizerService.minimize(profiledClientAnswerDetails, mode)
                .flatMap(aiAnalyzer::analyze)
                .map(response -> InsightReport.of(
                        profiledClientAnswerDetails.getSubmissionId(),
                        response.getClientId(),
                        response.getClientName(),
                        response.getTestName(),
                        response.getModel(),
                        mode,
                        response.getSchemaName(),
                        response.getSchemaVersion(),
                        response.getRawJson(),
                        response.getDate(),
                        response.getInsightReportStructuredAiResponse()
                ));
    }
}
