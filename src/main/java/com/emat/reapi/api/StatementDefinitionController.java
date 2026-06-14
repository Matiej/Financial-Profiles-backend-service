package com.emat.reapi.api;

import com.emat.reapi.api.dto.StatementDefinitionDto;
import com.emat.reapi.statement.domain.StatementProfile;
import com.emat.reapi.statement.domain.StatementDefinition;
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
            summary = "Save statement definition",
            description = "Adds a new fistStatement definition to the system",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Statement definition saved successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<StatementDefinitionDto> saveStatement(
            @Valid @RequestBody StatementDefinitionDto dto
    ) {
        log.info("Received request: POST /api/fistStatement, statementsId: {}", dto.statementId());
        StatementDefinition domain = dto.toDomain();
        return statementService.saveStatementDefinition(domain)
                .map(StatementDefinitionDto::toDto);
    }

    @Operation(
            summary = "Get all statement definitions",
            description = "Retrieves all fistStatement definitions from the system",
            responses = @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    )
    @GetMapping()
    public Flux<StatementDefinitionDto> getAllStatements() {
        log.info("received request: GET /api/statement");
        return statementService.getAllStatementDefinitions()
                .map(StatementDefinitionDto::toDto);
    }

    @Operation(
            summary = "Get statement definitions by category",
            description = "Retrieves fistStatement definitions filtered by category",
            responses = @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    )
    @GetMapping(value = "/category", params = "category")
    public Flux<StatementDefinitionDto> getStatementsByCategory(
            @Parameter(description = "Category to filter definitions")
            @RequestParam StatementProfile category
    ) {
        log.info("received request: GET /api/statement/statements with 'category': {}", category);
        return statementService.getStatementDefinitionsByCategory(category)
                .map(StatementDefinitionDto::toDto);
    }

}
