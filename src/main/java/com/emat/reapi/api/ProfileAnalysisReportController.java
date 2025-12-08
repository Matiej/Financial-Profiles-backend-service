package com.emat.reapi.api;


import com.emat.reapi.api.dto.InsightReportDto.InsightReportDto;
import com.emat.reapi.api.dto.InsightReportDto.InsightReportMapper;
import com.emat.reapi.api.dto.InsightReportDto.ReportJobStatusDto;
import com.emat.reapi.profileanalysis.ProfileAnalysisService;
import com.emat.reapi.profileanalysis.domain.PayloadMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@Slf4j
@AllArgsConstructor
@Tag(name = "Profiler analysis", description = "Endpoints for AI profile analysis")
public class ProfileAnalysisReportController {
    private final ProfileAnalysisService profileAnalysisService;

    @Operation(
            summary = "Get all AI analysis by submissionID",
            description = "Retrieves all profiled clients analysis",
            responses = @ApiResponse(responseCode = "200", description = "Retrieved successfully")
    )
    @GetMapping("/{submissionId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<List<InsightReportDto>> findProfiledClientAnalysisBySubmissionId(@PathVariable String submissionId) {
        log.info("Received request: GET /api/analysis/{submissionId} to retrieve profiled client reports for submission ID: {}", submissionId);
        return profileAnalysisService.getAnalysis(submissionId)
                .map(results -> results
                        .stream()
                        .map(InsightReportMapper::toDto)
                        .toList());
    }

    @Operation(
            summary = "Send profiled statement to AI analyze",
            description = "Get and send profiled statement by submission ID to AI analyze ",
            responses = @ApiResponse(responseCode = "201", description = "Statement sent to analyze")
    )
    @PostMapping(value = "/{submissionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> analyze(
            @PathVariable String submissionId,
            @RequestParam(defaultValue = "false") boolean force,
            @RequestParam(defaultValue = "MINIMAL") PayloadMode mode,
            @RequestParam(defaultValue = "1") int retry
    ) {
        log.info("Received request: POST /api/analysis/{submissionId} to retrieve profiled client reports for submission ID: {}", submissionId);
        return profileAnalysisService.enqueueStatementToAnalyze(submissionId, mode, force, retry)
                .map(job -> ResponseEntity.accepted()
                        .location(URI.create("/api/profiler/analysis/jobs/" + job.getId()))
                        .build()
                );
    }

    @Operation(
            summary = "Get current latest AI analyze status",
            description = "Get latest AI analyze status by submissionId for UI functions display",
            responses = @ApiResponse(responseCode = "200", description = "Retrieved successfully")
    )
    @GetMapping("/{submissionId}/status")
    public Mono<ResponseEntity<ReportJobStatusDto>> latestStatus(@PathVariable String submissionId) {
        log.info("Received request: GET /api/analysis/{submissionId}/status to retrieve profiled analyze status for submission ID: {}", submissionId);
        return profileAnalysisService.getLatestAnalysisStatus(submissionId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @Operation(
            summary = "Get current latest AI longest remaining seconds status ",
            description = "Get latest AI analyze status by submissionId for UI functions display",
            responses = @ApiResponse(responseCode = "200", description = "Retrieved successfully")
    )
    @GetMapping("/status")
    public Mono<ResponseEntity<ReportJobStatusDto>> latestStatus() {
        log.info("Received request: GET /api/analysis/status to retrieve longest remaing time status");
        return profileAnalysisService.getLatestAnalysisStatus()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

}
