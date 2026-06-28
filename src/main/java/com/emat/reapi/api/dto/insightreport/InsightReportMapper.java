package com.emat.reapi.api.dto.InsightReportDto;

import com.emat.reapi.profileanalysis.domain.*;

public class InsightReportMapper {
    private InsightReportMapper() {
    }

    public static InsightReportDto toDto(InsightReport insightReport) {
        if (insightReport == null) return null;
        return new InsightReportDto(
                insightReport.getSubmissionId(),
                insightReport.getClientId(),
                insightReport.getClientName(),
                insightReport.getTestName(),
                insightReport.getModel(),
                insightReport.getPayloadMode(),
                insightReport.getSchemaName(),
                insightReport.getSchemaVersion(),
                insightReport.getCreatedAt(),
                insightReport.getRawJson(),
                toDto(insightReport.getInsightReportStructuredAiResponse())
        );
    }

    private static InsightReportStructuredAiResponseDto toDto(InsightReportStructuredAiResponse insightReportStructuredAiResponse) {
        if (insightReportStructuredAiResponse == null) return null;
        return new InsightReportStructuredAiResponseDto(
                toDto(insightReportStructuredAiResponse.getClientSummary()),
                insightReportStructuredAiResponse.getCategoryInsights() == null ?
                        null : insightReportStructuredAiResponse.getCategoryInsights()
                        .stream()
                        .map(InsightReportMapper::toDto).toList(),
                insightReportStructuredAiResponse.getNextSteps()
        );
    }

    private static ClientSummaryDto toDto(ClientSummary clientSummary) {
        if (clientSummary == null) return null;
        return new ClientSummaryDto(
                clientSummary.getKeyThemes(),
                clientSummary.getDominantCategories() == null ?
                        null : clientSummary.getDominantCategories()
                        .stream()
                        .map(InsightReportMapper::toDto).toList(),
                clientSummary.getOverallNarrativePl()
        );
    }

    private static DominantCategoryDto toDto(DominantCategory dominantCategory) {
        if (dominantCategory == null) return null;
        return new DominantCategoryDto(dominantCategory.getCategoryId(), dominantCategory.getBalanceIndex(), dominantCategory.getWhy());
    }

    private static CategoryInsightDto toDto(CategoryInsight categoryInsight) {
        if (categoryInsight == null) return null;
        return new CategoryInsightDto(
                categoryInsight.getCategoryId(),
                categoryInsight.getCategoryLabelPl(),
                categoryInsight.getStrengthsPl(),
                categoryInsight.getRisksPl(),
                categoryInsight.getContradictionsPl(),
                categoryInsight.getRecommendedInterventions() == null ?
                        null : categoryInsight.getRecommendedInterventions()
                        .stream()
                        .map(InsightReportMapper::toDto)
                        .toList()
        );
    }

    private static RecommendedInterventionDto toDto(RecommendedIntervention recommendedIntervention) {
        if (recommendedIntervention == null) return null;
        return new RecommendedInterventionDto(
                recommendedIntervention.getType(),
                recommendedIntervention.getTitlePl(),
                recommendedIntervention.getWhyPl(),
                recommendedIntervention.getHowPl());
    }


    public static InsightReport toDomain(InsightReportDto insightReportDto) {
        if (insightReportDto == null) return null;
        return InsightReport.of(
                insightReportDto.submissionId(),
                insightReportDto.clientId(),
                insightReportDto.clientName(),
                insightReportDto.testName(),
                insightReportDto.model(),
                insightReportDto.payloadMode(),
                insightReportDto.schemaName(),
                insightReportDto.schemaVersion(),
                insightReportDto.rawJson(),
                insightReportDto.createdAt(),
                toDomain(insightReportDto.insightReportStructuredAiResponseDto())
        );
    }

    private static InsightReportStructuredAiResponse toDomain(InsightReportStructuredAiResponseDto insightReportStructuredAiResponseDto) {
        if (insightReportStructuredAiResponseDto == null) return null;
        return InsightReportStructuredAiResponse.builder()
                .clientSummary(toDomain(insightReportStructuredAiResponseDto.clientSummary()))
                .categoryInsights(insightReportStructuredAiResponseDto.categoryInsights() == null ?
                        null : insightReportStructuredAiResponseDto.categoryInsights()
                        .stream()
                        .map(InsightReportMapper::toDomain)
                        .toList())
                .nextSteps(insightReportStructuredAiResponseDto.nextSteps())
                .build();
    }

    private static ClientSummary toDomain(ClientSummaryDto clientSummaryDto) {
        if (clientSummaryDto == null) return null;
        return ClientSummary.builder()
                .keyThemes(clientSummaryDto.keyThemes())
                .dominantCategories(clientSummaryDto.dominantCategories() == null ?
                        null : clientSummaryDto.dominantCategories()
                        .stream()
                        .map(InsightReportMapper::toDomain)
                        .toList())
                .overallNarrativePl(clientSummaryDto.overallNarrativePl())
                .build();
    }

    private static DominantCategory toDomain(DominantCategoryDto dominantCategoryDto) {
        if (dominantCategoryDto == null) return null;
        return DominantCategory.builder()
                .categoryId(dominantCategoryDto.categoryId())
                .balanceIndex(dominantCategoryDto.balanceIndex())
                .why(dominantCategoryDto.why())
                .build();
    }

    private static CategoryInsight toDomain(CategoryInsightDto categoryInsightDto) {
        if (categoryInsightDto == null) return null;
        return CategoryInsight.builder()
                .categoryId(categoryInsightDto.categoryId())
                .categoryLabelPl(categoryInsightDto.categoryLabelPl())
                .strengthsPl(categoryInsightDto.strengthsPl())
                .risksPl(categoryInsightDto.risksPl())
                .contradictionsPl(categoryInsightDto.contradictionsPl())
                .recommendedInterventions(categoryInsightDto.recommendedInterventionsDto() == null ?
                        null : categoryInsightDto.recommendedInterventionsDto()
                        .stream()
                        .map(InsightReportMapper::toDomain)
                        .toList())
                .build();
    }

    private static RecommendedIntervention toDomain(RecommendedInterventionDto recommendedInterventionDto) {
        if (recommendedInterventionDto == null) return null;
        return RecommendedIntervention.builder()
                .type(recommendedInterventionDto.type())
                .titlePl(recommendedInterventionDto.titlePl())
                .whyPl(recommendedInterventionDto.whyPl())
                .howPl(recommendedInterventionDto.howPl())
                .build();
    }
}
