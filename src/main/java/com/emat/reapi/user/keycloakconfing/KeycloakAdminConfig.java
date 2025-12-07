package com.emat.reapi.user.keycloakconfing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminConfig {

    @Bean
    WebClient keycloakAdminWebClient(KeycloakAdminProperties props) {
        return WebClient.builder()
                .baseUrl(props.baseUrl() + "/admin/realms/" + props.realm())
                .build();
    }
}
