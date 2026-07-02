package com.emat.reapi.api;

import com.emat.reapi.api.dto.statement.StatementDefinitionRequest;
import com.emat.reapi.api.dto.statement.StatementDefinitionResponse;
import com.emat.reapi.statement.port.StatementDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/definition")
@Slf4j
@AllArgsConstructor
@Validated
@Tag(name = "Statement", description = "Endpoints for managing client statement definitions")
public class StatementDefinitionController {
    private final StatementDefinitionService statementService;

    @Operation(
            summary = "Create statement definition",
            description = "Adds a new statement definition; statementKey is generated server-side",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Statement definition created"),
                    @ApiResponse(responseCode = "400", description = "Invalid input or unknown profileId")
            }
    )
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<StatementDefinitionResponse> createStatement(
            @Valid @RequestBody StatementDefinitionRequest request
    ) {
        log.info("Received request: POST /api/definition, profileId: {}", request.profileId());
        return statementService.createStatementDefinition(request.toDomain())
                .map(StatementDefinitionResponse::toResponse);
    }

    @Operation(
            summary = "Get all active statement definitions",
            description = "Retrieves all active (not soft-deleted) statement definitions",
            responses = @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    )
    @GetMapping()
    public Flux<StatementDefinitionResponse> getAllStatements() {
        log.info("Received request: GET /api/definition");
        return statementService.getActiveStatementDefinitions()
                .map(StatementDefinitionResponse::toResponse);
    }

    @Operation(
            summary = "Get statement definitions by profile",
            description = "Retrieves active statement definitions filtered by profileId",
            responses = @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    )
    @GetMapping(params = "profileId")
    public Flux<StatementDefinitionResponse> getStatementsByProfileId(
            @Parameter(description = "Profile id to filter definitions")
            @RequestParam String profileId
    ) {
        log.info("Received request: GET /api/definition with 'profileId': {}", profileId);
        return statementService.getStatementDefinitionsByProfileId(profileId)
                .map(StatementDefinitionResponse::toResponse);
    }

    @Operation(
            summary = "Update statement definition",
            description = "Edits the profile assignment and texts; statementKey is immutable",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Statement definition updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid input or unknown profileId"),
                    @ApiResponse(responseCode = "404", description = "Statement definition not found"),
                    @ApiResponse(responseCode = "409", description = "Profile change rejected — definition used in a submitted test")
            }
    )
    @PutMapping("/{id}")
    public Mono<StatementDefinitionResponse> updateStatement(
            @PathVariable String id,
            @Valid @RequestBody StatementDefinitionRequest request
    ) {
        log.info("Received request: PUT /api/definition/{}", id);
        return statementService.updateStatementDefinition(id, request.toDomain())
                .map(StatementDefinitionResponse::toResponse);
    }

    @Operation(
            summary = "Soft-delete statement definition",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Statement definition soft-deleted"),
                    @ApiResponse(responseCode = "404", description = "Statement definition not found"),
                    @ApiResponse(responseCode = "409", description = "Definition still referenced by an existing test")
            }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> deleteStatement(@PathVariable String id) {
        log.info("Received request: DELETE /api/definition/{}", id);
        return statementService.softDeleteStatementDefinition(id);
    }
}
