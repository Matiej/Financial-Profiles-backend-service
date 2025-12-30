package com.emat.reapi.api;

import com.emat.reapi.api.dto.clienttestdto.ClientTestDto;
import com.emat.reapi.api.dto.clienttestdto.ClientTestSubmissionDto;
import com.emat.reapi.clienttest.ClientTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/client/test")
@Slf4j
@AllArgsConstructor
@Validated
@Tag(name = "Client Test", description = "Endpoints for managing client profile testes.")
public class ClientTestController {
    private final ClientTestService clientTestService;

    @PostMapping()
    @Operation(
            summary = "Save client test",
            description = "Save client test",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Client test saved successful"),
                    @ApiResponse(responseCode = "400", description = "Invalid public token ")
            }
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> saveTest(
            @Valid @RequestBody ClientTestSubmissionDto request
    ) {
        log.info("Received request: POST '/api/pftest' to retrieve save client tests");
        return clientTestService.saveClientTest(request);

    }

    @GetMapping("/{publicToken}")
    @Operation(
            summary = "Get client test",
            description = "Retrieves client test by public token",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Test found and prepared successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid public token ")
            }
    )
    @ResponseStatus(HttpStatus.OK)
    public Mono<ClientTestDto> getClientTest(
            @PathVariable String publicToken
    ) {
        log.info("Received request: GET '/api/client/test' to retrieve client test fo token: {}", publicToken);
        return clientTestService.getClientTestByToken(publicToken)
                .map(ClientTestDto::fromDomain);
    }
}
