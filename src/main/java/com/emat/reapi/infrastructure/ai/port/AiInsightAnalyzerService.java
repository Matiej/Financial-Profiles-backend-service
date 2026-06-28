package com.emat.reapi.ai.port;

import com.emat.reapi.profileanalysis.domain.InsightReportAiResponse;
import com.emat.reapi.profileanalysis.domain.MinimizedPayload;
import reactor.core.publisher.Mono;

public interface AiInsightAnalyzerService {
    Mono<InsightReportAiResponse> analyze(MinimizedPayload payload);
}
