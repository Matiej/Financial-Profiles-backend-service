package com.emat.reapi.api;

import com.emat.reapi.account.AccountService;
import com.emat.reapi.api.dto.account.AccountResponse;
import com.emat.reapi.api.dto.account.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/account")
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name = "Account", description = "Endpoints for managing own account (self-service)")
public class AccountController {

    private final AccountService accountService;

    @Operation(
            summary = "Get own account",
            description = "Retrieves current user's account data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account retrieved successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    Mono<AccountResponse> getAccount(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Received request: GET '/api/account' for userId: {}", userId);
        return accountService.getAccount(userId)
                .map(AccountResponse::fromDomain);
    }

    @Operation(
            summary = "Update own profile",
            description = "Updates current user's profile data (firstName, lastName, email)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    Mono<AccountResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        String userId = jwt.getSubject();
        log.info("Received request: PUT '/api/account' for userId: {}", userId);
        return accountService.updateProfile(userId, request.toCommand())
                .map(AccountResponse::fromDomain);
    }

    @Operation(
            summary = "Request password reset",
            description = "Sends password reset email to current user",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Password reset email sent"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> requestPasswordReset(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Received request: POST '/api/account/reset-password' for userId: {}", userId);
        return accountService.requestPasswordReset(userId);
    }

    @Operation(
            summary = "Delete own account",
            description = "Permanently deletes current user's account",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Received request: DELETE '/api/account' for userId: {}", userId);
        return accountService.deleteAccount(userId);
    }
}
