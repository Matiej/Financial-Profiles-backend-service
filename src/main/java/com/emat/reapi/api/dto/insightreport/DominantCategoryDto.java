package com.emat.reapi.api.dto.InsightReportDto;

public record DominantCategoryDto(
        String categoryId,
        double balanceIndex,
        String why
) {
}
