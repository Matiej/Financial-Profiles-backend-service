package com.emat.reapi.api.dto.InsightReportDto;

import java.util.List;

public record CategoryInsightDto(
        String categoryId,
        String categoryLabelPl,
        List<String>strengthsPl,
        List<String> risksPl,
        List<String> contradictionsPl,
        List<RecommendedInterventionDto> recommendedInterventionsDto
) {
}
