package com.emat.reapi.infrastructure.keycloak;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminProperties(
        String baseUrl,
        String realm,
        String clientId,
        String clientSecret,
        String calculatorRoleId,
        String calculatorRoleName,
        String frontendClientId
) {
}
