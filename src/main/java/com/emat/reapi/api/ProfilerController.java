package com.emat.reapi.api;

import com.emat.reapi.clienttest.ClientTestSubmissionService;
import com.emat.reapi.profiler.ProfiledScoringTestService;
import com.emat.reapi.profiler.domain.ScoringProfiledClientDetails;
import com.emat.reapi.profiler.domain.ScoringProfiledShort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/profiler")
@Slf4j
@AllArgsConstructor
@Validated
@Tag(name = "Profiler", description = "Endpoints for profiled client answers")
public class ProfilerController {
    private final ProfiledScoringTestService profiledScoringTestService;
    private final ClientTestSubmissionService clientTestSubmissionService;

    @Operation(
            summary = "Get client profiled scoring test",
            description = "Retrieves client profiled scoring test details by testSubmissionPublicId.",
            responses = @ApiResponse(responseCode = "200", description = "Retrieved successfully")
    )
    @GetMapping("/{testSubmissionPublicId}/scoring")
    public Mono<ScoringProfiledClientDetails> scoringClientTestBySubmissionId(
            @PathVariable String testSubmissionPublicId
    ) {
        log.info("Received request: GET '/{testSubmissionPublicId}/scoring' to retrieve profiled client answer for testSubmissionPublicId: {}", testSubmissionPublicId);
        return profiledScoringTestService.getScoringProfile(testSubmissionPublicId);
    }

    @Operation(
            summary = "Get all profiled client scoring tests",
            description = "Retrieves all shorten profiled scoring clients tests by testSubmissionPublicId. Short version of all tests",
            responses = @ApiResponse(responseCode = "200", description = "Retrieved successfully")
    )
    @GetMapping("/scoring")
    public Flux<ScoringProfiledShort> scoringClientTests() {
        log.info("Received request: GET '/scoring' to retrieve all profiled client short tests.");
        return profiledScoringTestService.getScoringShortProfiles();
    }

    @Operation(
            summary = "Soft-delete a client scoring test",
            description = "Soft-deletes the stored client test by testSubmissionPublicId so it is excluded from scoring listings.",
            responses = @ApiResponse(responseCode = "204", description = "Soft-deleted successfully")
    )
    @DeleteMapping("/{testSubmissionPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> softDeleteClientTest(
            @PathVariable String testSubmissionPublicId
    ) {
        log.info("Received request: DELETE '/{testSubmissionPublicId}' to soft-delete client test for testSubmissionPublicId: {}", testSubmissionPublicId);
        return clientTestSubmissionService.softDeleteByTestSubmissionPublicId(testSubmissionPublicId);
    }
}
