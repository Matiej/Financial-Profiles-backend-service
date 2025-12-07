package com.emat.reapi.user;

import com.emat.reapi.api.dto.user.CalculatorUserDto;
import com.emat.reapi.user.domain.KeycloakUser;
import com.emat.reapi.user.domain.KeycloakUserRequest;
import com.emat.reapi.user.infra.KeyCloakClient;
import com.emat.reapi.user.infra.KeycloakUserSummary;
import com.nimbusds.jose.KeyLengthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final KeyCloakClient keyCloakClient;

    @Override
    public Flux<KeycloakUser> getAllCalculatorUsers() {
        return keyCloakClient.listCalculatorUsers()
                .map(KeycloakUserSummary::toDomain)
                .doOnError(err -> log.warn("Error fetched calculator users"));
    }

    @Override
    public Mono<String> createCalculatorUser(KeycloakUserRequest request) {
        return keyCloakClient.createCalculatorUser(request)
                .doOnSuccess(suc -> log.info("User {}, created successfully", request.username()));
    }

    @Override
    public Mono<KeycloakUser> updateCalculatorUser(CalculatorUserDto calculatorUserDto) {
        return null;
    }
}
