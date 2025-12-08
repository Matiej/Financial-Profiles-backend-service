package com.emat.reapi.profileanalysis;

import com.emat.reapi.profileanalysis.domain.MinimizedPayload;
import com.emat.reapi.profileanalysis.domain.PayloadMode;
import com.emat.reapi.profiler.domain.ProfiledClientAnswerDetails;
import reactor.core.publisher.Mono;

public interface MinimizerService {
    Mono<MinimizedPayload> minimize(ProfiledClientAnswerDetails profiled, PayloadMode mode);
}
