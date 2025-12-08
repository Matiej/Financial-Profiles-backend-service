package com.emat.reapi.user.infra;

import com.emat.reapi.user.keycloakconfing.KeycloakAdminProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminTokenProvider {
    private final KeycloakAdminProperties keycloakAdminProperties;
    private final WebClient.Builder webClientBuilder;

    Mono<String> getToken() {
        return webClientBuilder.build()
                .post()
                .uri(keycloakAdminProperties.baseUrl() + "/realms/" + keycloakAdminProperties.realm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", "client_credentials")
                        .with("client_id", keycloakAdminProperties.clientId())
                        .with("client_secret", keycloakAdminProperties.clientSecret()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("access_token").asText())
                .doOnSuccess(suc -> log.info("Token retrieved successful"));
    }
}
