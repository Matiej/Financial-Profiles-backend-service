package com.emat.reapi.profiler.port;

import com.emat.reapi.profiler.domain.ProfiledClientAnswerDetails;
import com.emat.reapi.profiler.domain.ProfiledClientAnswerShort;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProfiledService {

    Mono<ProfiledClientAnswerDetails> getClientProfiledStatement(String submissionId);
    Mono<List<ProfiledClientAnswerShort>> getProfiledShort(Sort sort);
}
