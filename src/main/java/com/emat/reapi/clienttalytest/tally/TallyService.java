package com.emat.reapi.tally;

import com.emat.reapi.api.tally.TallyWebhookEvent;
import reactor.core.publisher.Mono;

public interface TallyService {
    Mono<Void> processTallyEvent(TallyWebhookEvent event);
}
