package com.emat.reapi.statement.port;

import com.emat.reapi.statement.ProfileStateException;
import com.emat.reapi.statement.domain.Profile;
import com.emat.reapi.statement.infra.ProfileDocument;
import com.emat.reapi.statement.infra.ProfileRepository;
import com.emat.reapi.statement.infra.StatementDefinitionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@Slf4j
@AllArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ProfileRepository profileRepository;
    private final StatementDefinitionRepository statementDefinitionRepository;

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

    @Override
    public Mono<Profile> createProfile(Profile profile) {
        Instant now = Instant.now();
        Profile toCreate = profile.toBuilder()
                .id(null) // let Mongo generate the identifier
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        log.info("Creating new profile: {}", toCreate.getPlName());
        return profileRepository.save(ProfileDocument.toDocument(toCreate))
                .map(ProfileDocument::toDomain);
    }

    @Override
    public Mono<Profile> updateProfile(String id, Profile updatedProfile) {
        log.info("Updating profile: {}", id);
        return profileRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found: " + id)))
                .flatMap(existing -> {
                    existing.setPlName(updatedProfile.getPlName());
                    existing.setBlockingName(updatedProfile.getBlockingName());
                    existing.setTransitionalName(updatedProfile.getTransitionalName());
                    existing.setResourcesName(updatedProfile.getResourcesName());
                    existing.setOrder(updatedProfile.getOrder());
                    existing.setUpdatedAt(Instant.now());
                    return profileRepository.save(existing);
                })
                .map(ProfileDocument::toDomain);
    }

    @Override
    public Mono<Void> softDeleteProfile(String id) {
        log.info("Soft-deleting profile: {}", id);
        return profileRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found: " + id)))
                .flatMap(existing -> statementDefinitionRepository.existsByProfileId(id)
                        .flatMap(inUse -> {
                            if (Boolean.TRUE.equals(inUse)) {
                                return Mono.error(new ProfileStateException(
                                        "Profile is referenced by definitions and cannot be deleted: " + id,
                                        ProfileStateException.ProfileErrorType.PROFILE_IN_USE));
                            }
                            existing.setDeleted(true);
                            existing.setUpdatedAt(Instant.now());
                            return profileRepository.save(existing);
                        }))
                .then();
    }
}
