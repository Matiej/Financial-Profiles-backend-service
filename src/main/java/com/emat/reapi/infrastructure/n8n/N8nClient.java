package com.emat.reapi.infrastructure.n8n;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@AllArgsConstructor
public class N8nClient {
    private final WebClient n8nWebClient;
    private final static String CLIENT_SCORE_TEST_EMAIL_URI = "/score-test/email";

    public Mono<Void> sendClientScoringTestNotification(ClientScoreTestNotification clientScoreTestNotification) {
        return n8nWebClient
                .post()
                .uri(CLIENT_SCORE_TEST_EMAIL_URI)
                .bodyValue(clientScoreTestNotification)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSubscribe(it -> log.info("Calling n8n email notifier: {}", CLIENT_SCORE_TEST_EMAIL_URI))
                .doOnSuccess(suc -> log.info("n8n email notifier responded with 2xx (no body)."))
                .doOnError(err -> log.error("Error calling n8n email notifier", err));
    }
}
