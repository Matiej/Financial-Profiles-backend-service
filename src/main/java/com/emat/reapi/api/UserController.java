package com.emat.reapi.api;

import com.emat.reapi.api.dto.user.CalculatorUserDto;
import com.emat.reapi.api.dto.user.UserResponse;
import com.emat.reapi.user.UserService;
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

import java.util.Comparator;

@RestController
@RequestMapping("/api/users")
@Slf4j
@AllArgsConstructor
@Validated
@Tag(name = "User", description = "Endpoints for create and managing users in KeyCloak service.")
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "Get all calculator users",
            description = "Retrieves all users with calculator view role",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List retrieved successful"),
                    @ApiResponse(responseCode = "500", description = "Internal server Error"),
                    @ApiResponse(responseCode = "404", description = "Bad request")
            }
    )
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    Flux<UserResponse> listAllCalculatorUsers() {
        log.info("Received request: GET '/api/users' to retrieve all keycloak calculator users");
        return userService.getAllCalculatorUsers()
                .map(UserResponse::fromDomain)
                .sort(Comparator.comparing(UserResponse::createdAt).reversed());
    }

    @Operation(
            summary = "Create calculator user",
            description = "Create user1 with calculator view role",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User created successfully"),
                    @ApiResponse(responseCode = "500", description = "Internal server Error"),
                    @ApiResponse(responseCode = "404", description = "Bad request")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Mono<String> createCalculatorUser(
            @Valid @RequestBody CalculatorUserDto calculatorUserDto) {
        log.info("Received request: POST '/api/users' to create keycloak calculator user");
        return userService.createCalculatorUser(calculatorUserDto.toRequest());
    }

    @Operation(
            summary = "Update user",
            description = "Update user for given userId ",
            responses = {
                    @ApiResponse(responseCode = "202", description = "User updated successfully"),
                    @ApiResponse(responseCode = "500", description = "Internal server Error"),
                    @ApiResponse(responseCode = "404", description = "Bad request")
            }
    )
    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<UserResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody CalculatorUserDto calculatorUserDto) {
        log.info("Received request: PUT '/api/users/{userId}' to update user for given id:{}", userId);
        return userService.updateUser(userId, calculatorUserDto.toRequest())
                .map(UserResponse::fromDomain);
    }

    @Operation(
            summary = "Delete user",
            description = "Delete user for given userId ",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User deleted successfully"),
                    @ApiResponse(responseCode = "500", description = "Internal server Error"),
                    @ApiResponse(responseCode = "404", description = "Bad request")
            }
    )
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<Void> deleteUser(
            @PathVariable String userId
    ) {
        log.info("Received request: DELETE '/api/users/{userId}' to delete user for given id:{}", userId);
        return userService.deleteUser(userId);
    }

    @Operation(
            summary = "Change user status",
            description = "Enable/Disable user",
            responses = {
                    @ApiResponse(responseCode = "202", description = "User status changed successfully"),
                    @ApiResponse(responseCode = "500", description = "Internal server Error"),
                    @ApiResponse(responseCode = "404", description = "Bad request")
            }
    )
    @PutMapping("/{userId}/status")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> changeUserState(
            @PathVariable String userId,
            @RequestParam Boolean status
    ) {
        log.info("Received request: PUT '/api/users/{userId}' to change user status to: {} for given id:{}", status.toString(), userId);
        return userService.changeUserStatus(userId, status);
    }
}
