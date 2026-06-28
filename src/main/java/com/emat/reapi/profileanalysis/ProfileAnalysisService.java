package com.emat.reapi.profileanalysis;

import com.emat.reapi.api.dto.insightreport.ReportJobStatusDto;
import com.emat.reapi.profileanalysis.domain.InsightReport;
import com.emat.reapi.profileanalysis.domain.PayloadMode;
import com.emat.reapi.profileanalysis.domain.reportjob.ReportJob;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProfileAnalysisService {
    Mono<ReportJobStatusDto> getLatestAnalysisStatus(String submissionId);
    Mono<ReportJobStatusDto> getLatestAnalysisStatus();

    Mono<List<InsightReport>> getAnalysis(String submissionId);
    Mono<Void> saveReport(InsightReport insightReport);

    Mono<ReportJob> enqueueStatementToAnalyze(String submissionId, PayloadMode mode, boolean force, int retry);
}
