package com.emat.reapi.global;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${app.security.swagger-token}")
    private String tokenUrl;
    @Value("${app.security.swagger-url}")
    private String swaggerUrl;

    @Bean
    public OpenAPI apiInfo() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("""
                        Keycloak logging (password flow).
                        Enter:
                        • username / password (eg tech.admin)
                        • swagger uses client_id/client_secret from configuration
                        and retrieves access_token, then will use as Bearer JWT.
                        """)
                .flows(new OAuthFlows()
                        .password(new OAuthFlow()
                                .tokenUrl(tokenUrl)
                                .scopes(new Scopes()
                                        .addString("openid", "OpenID scope")
                                        .addString("profile", "Profil użytkownika")
                                )
                        )
                );

        return new OpenAPI()
                .servers(List.of(new Server().url(swaggerUrl)))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme)
                )
                .info(new Info()
                        .title("PROFILER BACKEND")
                        .description("Finance profiler backend app")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .email("myEmail@email.com")
                                .name("Maciek")));

    }
}
