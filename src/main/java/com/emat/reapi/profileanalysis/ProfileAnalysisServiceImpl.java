package com.emat.reapi.profileanalysis;

import com.emat.reapi.api.dto.insightreport.ReportJobStatusDto;
import com.emat.reapi.clienttest.ClientTestSubmissionService;
import com.emat.reapi.profileanalysis.domain.InsightReport;
import com.emat.reapi.profileanalysis.domain.PayloadMode;
import com.emat.reapi.profileanalysis.domain.reportjob.ReportJob;
import com.emat.reapi.profileanalysis.domain.reportjob.ReportJobStatus;
import com.emat.reapi.profileanalysis.infra.InsightReportDocument;
import com.emat.reapi.profileanalysis.infra.InsightReportRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@AllArgsConstructor
public class ProfileAnalysisServiceImpl implements ProfileAnalysisService {
    private final InsightReportRepository reportRepository;
    private final ReportJobService reportJobService;
    private final ProfileAiAnalysisProcessor profileAiAnalysisProcessor;
    private final ClientTestSubmissionService clientTestSubmissionService;

    @Override
    public Mono<Void> saveReport(InsightReport insightReport) {
        return reportRepository.save(InsightReportDocument.from(insightReport)).then();
    }

    @Override
    public Mono<ReportJobStatusDto> getLatestAnalysisStatus(String submissionId) {
        return reportJobService.getReportJobBySubmission(submissionId)
                .map(job -> ReportJobStatusDto.from(job, Instant.now()));
    }

    @Override
    public Mono<ReportJobStatusDto> getLatestAnalysisStatus() {
        return reportJobService.findAllJobs()
                .map(job -> ReportJobStatusDto.from(job, Instant.now()))
                .filter(p -> p.remainingLockSeconds() != null)
                .sort(Comparator.comparingLong(ReportJobStatusDto::remainingLockSeconds).reversed())
                .reduce((a, b) -> a.remainingLockSeconds() > b.remainingLockSeconds() ? a : b);
    }

    @Override
    public Mono<List<InsightReport>> getAnalysis(String submissionId) {
        log.info("Trying to fetch from reports for submissionId: {}", submissionId);
        return reportRepository.findBySubmissionIdOrderByCreatedAtDesc(submissionId)
                .collectList()
                .map(results -> results.
                        stream()
                        .map(InsightReportDocument::toDomain)
                        .toList())
                .doOnSuccess(result -> log.info("Fetched {} reports from data base for submissionId: {}",
                        result.isEmpty() ? 0 : result.size(),
                        submissionId));
    }

    @Override
    public Mono<ReportJob> enqueueStatementToAnalyze(String submissionId, PayloadMode mode, boolean force, int retry) {
        return reportJobService.getReportJobBySubmission(submissionId)
                .flatMap(existing -> {
                    boolean isActiveJob = existing.getStatus() == ReportJobStatus.PENDING
                            || existing.getStatus() == ReportJobStatus.RUNNING;
                    if (!force && isActiveJob) {
                        return Mono.just(existing);
                    }
                    existing.setStatus(ReportJobStatus.PENDING);
                    existing.setMode(mode);
                    existing.setError(null);
                    existing.setExpireAt(null);
                    return reportJobService.save(existing);
                })
                .switchIfEmpty(
                        reportJobService.save(ReportJob.builder()
                                .submissionId(submissionId)
                                .mode(mode)
                                .status(ReportJobStatus.PENDING)
                                .expireAt(null)
                                .build()
                        ))
                .doOnSuccess(job -> asyncAnalyzeProfiledStatement(job, retry)
                        .subscribe());
    }

    private Mono<Void> asyncAnalyzeProfiledStatement(ReportJob job, int retry) {
        var submissionId = job.getSubmissionId();
        var mode = job.getMode();
        return reportJobService.save(changeJobStatus(
                        job,
                        ReportJobStatus.RUNNING,
                        null,
                        null))
                .then(clientTestSubmissionService.findBySubmissionId(submissionId)
                        .map(ClientTestProfiledMapper::toProfiled)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No client test found for submissionId=" + submissionId))))
                .doOnSubscribe(s -> log.info("AI analysis START: submissionId={}, mode={}, retry={}",
                        submissionId,
                        mode,
                        retry))
                .flatMap(statement -> profileAiAnalysisProcessor.sendSubmissionToAI(statement, mode))
                .doOnNext(report -> log.info("AI analysis DONE: submissionId={}, model={}, schema={}/{}",
                        report.getSubmissionId(), report.getModel(), report.getSchemaName(), report.getSchemaVersion()))
                .delayUntil(report -> {
                            log.info("Saving report: submissionId={}, createdAt={}",
                                    report.getSubmissionId(),
                                    report.getCreatedAt());
                            return saveReport(report);
                        }
                )
                .doOnNext(r -> log.info("Report saved OK: submissionId={}", r.getSubmissionId()))
                .flatMap(saved -> reportJobService.save(changeJobStatus(
                        job,
                        ReportJobStatus.DONE,
                        Instant.now().plusSeconds(300),
                        null))
                )
                .retryWhen(
                        Retry.backoff(retry, Duration.ofSeconds(1))
                                .maxBackoff(Duration.ofSeconds(10))
                                .jitter(0.2)
                                .filter(this::isTransientError)
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying AI analysis: attempt={}, submissionId={}, cause={}",
                                        rs.totalRetries() + 1, submissionId, rs.failure().toString()))
                                .onRetryExhaustedThrow((spec, signal) ->
                                        new ResponseStatusException(
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                "AI analysis failed after retries for submissionId=" + submissionId,
                                                signal.failure()
                                        )
                                )
                )
                .onErrorResume(this::isNotFound, ex -> {
                    log.warn("Input not found for submissionId={}", submissionId);
                    var error = new IllegalStateException("Input not found: " + submissionId);
                    return reportJobService.save(changeJobStatus(
                            job,
                            ReportJobStatus.FAILED,
                            Instant.now().plusSeconds(300),
                            error));
                })
                .onErrorResume(ex -> {
                    log.error("`AI analysis ERROR`: submissionId={}, cause={}", submissionId, ex.toString(), ex);
                    return reportJobService.save(changeJobStatus(
                            job,
                            ReportJobStatus.FAILED,
                            Instant.now().plusSeconds(300),
                            ex));
                }).then();
    }

    private boolean isNotFound(Throwable t) {
        return t instanceof ResponseStatusException rs
                && rs.getStatusCode() == HttpStatus.NOT_FOUND;
    }

    private ReportJob changeJobStatus(ReportJob job, ReportJobStatus status, Instant expiryAt, Throwable error) {
        job.setStatus(status);
        job.setError(error != null ? error.getMessage() : null);
        job.setExpireAt(expiryAt);
        job.setUpdatedAt(Instant.now());
        return job;
    }

    private boolean isTransientError(Throwable t) {
        if (t instanceof TimeoutException) return true;
        if (t instanceof IOException) return true;
        if (t instanceof WebClientResponseException wcre) {

            int code = wcre.getStatusCode().value();
            return code >= 500 || code == 429;
        }
        return false;
    }
}

