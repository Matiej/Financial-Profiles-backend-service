package com.emat.reapi.api.dto.InsightReportDto;

import java.util.List;

public record InsightReportStructuredAiResponseDto(
        ClientSummaryDto clientSummary,
        List<CategoryInsightDto> categoryInsights,
        List<String> nextSteps
) {
}
