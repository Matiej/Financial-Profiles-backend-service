package com.emat.reapi.infrastructure.n8n;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class N8nWebClientConfig {
    @Value("${app.client.n8n.base-url}")
    private String n8nApiBaseUrl;

    @Bean
    public WebClient n8nWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(n8nApiBaseUrl)
                .defaultHeaders(it -> it.addAll(defaultHeaders()))
                .build();
    }

    private HttpHeaders defaultHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

}
