package com.emat.reapi.profiler;

import com.emat.reapi.profiler.domain.ScoringProfiledClientDetails;
import com.emat.reapi.profiler.domain.ScoringProfiledShort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProfiledScoringTestService {
    Mono<ScoringProfiledClientDetails> getScoringProfile(String testSubmissionPublicId);
    Flux<ScoringProfiledShort> getScoringShortProfiles();
}
