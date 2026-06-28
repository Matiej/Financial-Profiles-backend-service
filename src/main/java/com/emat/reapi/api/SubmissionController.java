package com.emat.reapi.api;

import com.emat.reapi.api.dto.submission.SubmissionDto;
import com.emat.reapi.api.dto.submission.SubmissionResponse;
import com.emat.reapi.api.dto.submission.SubmissionUpdateDto;
import com.emat.reapi.submission.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/submission")
@Slf4j
@AllArgsConstructor
@Validated
@Tag(name = "Submission", description = "Endpoints for managing test submissions.")
public class SubmissionController {
    private final SubmissionService submissionService;

    @Operation(
            summary = "Get all submissions",
            description = "Retrieves all submissions for clients ordered by createdAt date",
            responses = @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    )
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    Flux<SubmissionResponse> findAll() {
        log.info("Received request: GET '/api/submission' to retrieve all submissions");
        return submissionService.findAllByOrderByCreatedAtDesc()
                .map(SubmissionResponse::fromDomain);
    }

    @Operation(
            summary = "Get all submissions",
            description = "Retrieves all submissions for clients ordered by createdAt date",
            responses = @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    )
    @GetMapping("/{submissionId}")
    @ResponseStatus(HttpStatus.OK)
    Mono<SubmissionResponse> findBySubmissionId(@PathVariable String submissionId) {
        log.info("Received request: GET '/api/submission/{submissionsId}' to retrieve for submissionId: {}", submissionId);
        return submissionService.findBySubmissionId(submissionId)
                .map(SubmissionResponse::fromDomain);
    }

    @Operation(
            summary = "Create new submission",
            description = "Creates a new submission session for a given client and test",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Submission created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Mono<SubmissionResponse> createSubmission(@RequestBody @Valid SubmissionDto request) {
        log.info("Received request: POST '/api/submission' to create submission for clientId={}", request.clientId());
        return submissionService.createSubmission(request)
                .map(SubmissionResponse::fromDomain);
    }

    @Operation(
            summary = "Update submission",
            description = "Update submission session for a given submissionId",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Submission updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload")
            }
    )
    @PutMapping("/{submissionId}")
    @ResponseStatus(HttpStatus.OK)
    Mono<SubmissionResponse> updateSubmission(
            @RequestBody @Valid SubmissionUpdateDto update,
            @PathVariable String submissionId
    ) {
        log.info("Received request: PUT '/api/submission/{submissionId}' update for submissionId: {}", submissionId);
        return submissionService.updateSubmission(update, submissionId)
                .map(SubmissionResponse::fromDomain);
    }

    @Operation(
            summary = "Delete submission",
            description = "Delete submission session for a given submissionId",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Accepted"),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload")
            }
    )
    @DeleteMapping("/{submissionId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> deleteSubmission(
            @PathVariable String submissionId
    ) {
        log.info("Received request: DELETE '/api/submission/{submissionId}' delete for submissionId: {}", submissionId);
        return submissionService.deleteSubmission(submissionId);
    }

    @PutMapping("/{submissionId}/close")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<SubmissionResponse> closeSubmissions(@PathVariable String submissionId) {
        log.info("Received request: PUT '/api/submission/{submissionId}/close' close  submissionId: {}", submissionId);
        return submissionService.closeSubmission(submissionId)
                .map(SubmissionResponse::fromDomain);
    }
}
