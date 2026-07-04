package com.emat.reapi.infrastructure.n8n;

import com.emat.reapi.clienttest.domain.ClientTestSubmission;
import com.emat.reapi.profiler.ProfiledScoringTestService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class N8nServiceImpl implements N8nService {
    private final N8nClient client;
    private final ProfiledScoringTestService profiledScoringTestService;

    @Override
    public Mono<Void> sendScoringTestNotification(ClientTestSubmission clientTestSubmission) {
        return profiledScoringTestService.getScoringProfile(clientTestSubmission.getTestSubmissionPublicId())
                .map(details -> ClientScoreTestNotification.of(
                        details,
                        clientTestSubmission.getClientEmail(),
                        clientTestSubmission.getCreatedAt()
                ))
                .flatMap(client::sendClientScoringTestNotification)
                .doOnSubscribe(s -> log.info("Sending scoring test notification to n8n for submissionId={}", clientTestSubmission.getSubmissionId()));
    }
}
