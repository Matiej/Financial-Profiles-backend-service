package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class N8nServiceImpl implements N8nService {
    private final N8nClient client;

    @Override
    public Mono<Void> sendScoringTestNotification(ClientTestSubmission clientTestSubmission) {
        return client.sendClientScoringTestNotification(ClientScoreTestNotification.fromDomain(clientTestSubmission))
                .doOnSubscribe(s -> log.info("Sending scoring test notification to n8n for submissionId={}", clientTestSubmission.getSubmissionId()));
    }
}
