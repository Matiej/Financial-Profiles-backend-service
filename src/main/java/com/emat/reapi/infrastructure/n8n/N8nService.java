package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import reactor.core.publisher.Mono;

public interface N8nService {
    Mono<Void> sendScoringTestNotification(ClientTestSubmission clientTestSubmission);
}
