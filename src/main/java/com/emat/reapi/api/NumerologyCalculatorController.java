package com.emat.reapi.api;

import com.emat.reapi.api.dto.ncalculator.DatesCalculatorResponse;
import com.emat.reapi.api.dto.ncalculator.NCalculatorDateDto;
import com.emat.reapi.api.dto.ncalculator.NCalculatorPhraseDto;
import com.emat.reapi.api.dto.ncalculator.PhraseCalculatorResponse;
import com.emat.reapi.ncalculator.port.NCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ncalculator")
@Slf4j
@AllArgsConstructor
@Validated
public class NumerologyCalculatorController {
    private final NCalculatorService nCalculatorService;

    @Operation(
            summary = "Calculate numerology vibration for phrase",
            description = "Calculates numerological sums for vowels and consonants in a given phrase (including Polish letters). Digits are treated 1:1 and added to total vibration.",
            responses = @ApiResponse(responseCode = "200", description = "Calculated successfully")
    )
    @PostMapping("/phrase")
    @ResponseStatus(HttpStatus.OK)
    public Mono<PhraseCalculatorResponse> calculatePhrase(
            @Valid @RequestBody NCalculatorPhraseDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userName = resolveUserName(jwt);
        log.info("Received request: POST '/api/ncalculator/phrase' with phrase='{}' by user='{}'", request.phrase(), userName);
        return nCalculatorService.calculatePhrase(request.phrase(), userName)
                .map(PhraseCalculatorResponse::fromDomain);
    }

    @Operation(
            summary = "Calculate numerology vibration for dates",
            description = "Calculates numerological magic numbers for a given dates. ",
            responses = @ApiResponse(responseCode = "200", description = "Calculated successfully")
    )
    @PostMapping("/dates")
    @ResponseStatus(HttpStatus.OK)
    public Mono<DatesCalculatorResponse> calculatePhrase(
            @Valid @RequestBody NCalculatorDateDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userName = resolveUserName(jwt);
        log.info("Received request: POST '/api/ncalculator/dates' with birthDate:{}, reference date:{} by user='{}'.",
                request.birthDate(), request.referenceDate(), userName);
        return nCalculatorService.calculateDates(request.birthDate(), request.referenceDate(), userName)
                .map(DatesCalculatorResponse::fromDomain);
    }

    /**
     * Resolves the human-readable user name from the Keycloak-validated JWT,
     * preferring {@code preferred_username} and falling back to the subject id.
     */
    private static String resolveUserName(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String preferred = jwt.getClaimAsString("preferred_username");
        return (preferred != null && !preferred.isBlank()) ? preferred : jwt.getSubject();
    }
}
