package com.emat.reapi.statement.port;

import com.emat.reapi.statement.domain.Profile;
import com.emat.reapi.statement.infra.ProfileDocument;
import com.emat.reapi.statement.infra.ProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ProfileRepository profileRepository;

    @Override
    public Flux<Profile> getActiveProfiles() {
        log.info("Retrieving all active profiles");
        return profileRepository.findAllByIsDeletedFalseOrderByOrderAsc()
                .map(ProfileDocument::toDomain);
    }

    @Override
    public Mono<Profile> getProfileById(String id) {
        log.info("Retrieving profile by id: {}", id);
        return profileRepository.findById(id)
                .map(ProfileDocument::toDomain);
    }
}
