package com.emat.reapi.api.dto.insightreport;

public record DominantCategoryDto(
        String categoryId,
        double balanceIndex,
        String why
) {
}
