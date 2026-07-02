package com.emat.reapi.statement.port;

import com.emat.reapi.statement.domain.Profile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProfileService {

    Flux<Profile> getActiveProfiles();
    Mono<Profile> getProfileById(String id);
    Mono<Profile> createProfile(Profile profile);
    Mono<Profile> updateProfile(String id, Profile updatedProfile);
    Mono<Void> softDeleteProfile(String id);
}
