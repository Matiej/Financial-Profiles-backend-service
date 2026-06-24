package com.emat.reapi.configuration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Mono

import java.time.Instant

@TestConfiguration
class TestSecurityConfiguration {

    /**
     * Test decoder that bypasses Keycloak. The bearer token value is a comma-separated
     * list of realm roles (e.g. "TECH_ADMIN" or "TECH_ADMIN,BUSINESS_ADMIN"), which we
     * expose under the standard {@code realm_access.roles} claim. The production
     * SecurityConfiguration converter then maps them to ROLE_* authorities, so the real
     * authorization logic is exercised end-to-end.
     */
    @Bean
    @Primary
    ReactiveJwtDecoder reactiveJwtDecoder() {
        return (token) -> {
            def roles = (token == null || token.isBlank()) ? [] :
                    token.split(",").collect { it.trim() }.findAll { it }
            Mono.just(
                    Jwt.withTokenValue(token)
                            .header("alg", "none")
                            .claim("sub", "test-user")
                            .claim("realm_access", [roles: roles])
                            .issuedAt(Instant.now())
                            .expiresAt(Instant.now().plusSeconds(3600))
                            .build()
            )
        }
    }
}
