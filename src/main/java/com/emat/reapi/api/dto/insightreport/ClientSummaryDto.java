package com.emat.reapi.api.dto.insightreport;

import java.util.List;

public record ClientSummaryDto(
        List<String> keyThemes,
        List<DominantCategoryDto> dominantCategories,
        String overallNarrativePl
) {
}
