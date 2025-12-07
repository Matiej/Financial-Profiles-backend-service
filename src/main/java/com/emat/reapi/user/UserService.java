package com.emat.reapi.user;

import com.emat.reapi.api.dto.user.CalculatorUserDto;
import com.emat.reapi.user.domain.KeycloakUser;
import com.emat.reapi.user.domain.KeycloakUserRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<KeycloakUser> getAllCalculatorUsers();
    Mono<String> createCalculatorUser(KeycloakUserRequest request);
    Mono<KeycloakUser> updateCalculatorUser(CalculatorUserDto calculatorUserDto);
}
