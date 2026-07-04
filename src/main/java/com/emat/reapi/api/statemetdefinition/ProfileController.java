package com.emat.reapi.api.statemetdefinition;

import com.emat.reapi.api.dto.statementdefiniton.ProfileResponse;
import com.emat.reapi.api.dto.statementdefiniton.ProfileUpdateRequest;
import com.emat.reapi.statement.port.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Admin CRUD for editable scoring profiles. Nested under {@code /api/definition} because
 * profiles are metadata of the definition domain. Secured by {@code /api/**} role rules
 * (BUSINESS_ADMIN / TECH_ADMIN).
 */
@RestController
@RequestMapping("/api/definition/profile")
@Slf4j
@AllArgsConstructor
@Validated
@Tag(name = "Profile", description = "Endpoints for managing editable scoring profiles")
public class ProfileController {
    private final ProfileService profileService;

    @Operation(
            summary = "Create profile",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Profile created"),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProfileResponse> createProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        log.info("Received request: POST /api/definition/profile");
        return profileService.createProfile(request.toDomain())
                .map(ProfileResponse::toResponse);
    }

    @Operation(
            summary = "Get active profiles",
            responses = @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    )
    @GetMapping
    public Flux<ProfileResponse> getActiveProfiles() {
        log.info("Received request: GET /api/definition/profile");
        return profileService.getActiveProfiles()
                .map(ProfileResponse::toResponse);
    }

    @Operation(
            summary = "Get profile by id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile found"),
                    @ApiResponse(responseCode = "404", description = "Profile not found")
            }
    )
    @GetMapping("/{id}")
    public Mono<ProfileResponse> getProfile(@PathVariable String id) {
        log.info("Received request: GET /api/definition/profile/{}", id);
        return profileService.getProfileById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found: " + id)))
                .map(ProfileResponse::toResponse);
    }

    @Operation(
            summary = "Update profile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Profile not found")
            }
    )
    @PutMapping("/{id}")
    public Mono<ProfileResponse> updateProfile(
            @PathVariable String id,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        log.info("Received request: PUT /api/definition/profile/{}", id);
        return profileService.updateProfile(id, request.toDomain())
                .map(ProfileResponse::toResponse);
    }

    @Operation(
            summary = "Soft-delete profile",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Profile soft-deleted"),
                    @ApiResponse(responseCode = "404", description = "Profile not found"),
                    @ApiResponse(responseCode = "409", description = "Profile still referenced by definitions")
            }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> deleteProfile(@PathVariable String id) {
        log.info("Received request: DELETE /api/definition/profile/{}", id);
        return profileService.softDeleteProfile(id);
    }
}
