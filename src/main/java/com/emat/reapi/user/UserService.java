package com.emat.reapi.user;

import com.emat.reapi.user.domain.CreateUserRequest;
import com.emat.reapi.user.domain.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<User> getAllCalculatorUsers();
    Mono<String> createCalculatorUser(CreateUserRequest request);
    Mono<User> updateUser(String userId, CreateUserRequest request);
    Mono<Void> deleteUser(String userId);
    Mono<Void> changeUserStatus(String userId, boolean isEnabled);
}
