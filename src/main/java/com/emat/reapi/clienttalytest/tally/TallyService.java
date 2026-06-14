package com.emat.reapi.clienttalytest.tally;

import com.emat.reapi.api.tally.TallyWebhookEvent;
import reactor.core.publisher.Mono;

@Deprecated
public interface TallyService {
    Mono<Void> processTallyEvent(TallyWebhookEvent event);
}
