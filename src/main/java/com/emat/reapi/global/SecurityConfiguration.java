package com.emat.reapi.global;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfiguration {
    @Value("${app.security.swagger-public:false}")
    private boolean swaggerPublic;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(exchanges -> {
                            var spec = exchanges
                                    // public client test (link with token)
                                    .pathMatchers("/api/client/test/**").permitAll()
                                    .pathMatchers("/actuator/health/").permitAll()
                                    .pathMatchers("/actuator/health/**").permitAll();

                            // swagger only for tech - TECH_ADMIN
                            if (swaggerPublic) {
                                spec.pathMatchers(
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/docs",
                                        "/docs/**"
                                ).permitAll();
                            } else {
                                spec.pathMatchers(
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/docs",
                                        "/docs/**"
                                ).hasRole("TECH_ADMIN");
                            }
                            spec.pathMatchers("/api/ncalculator/**")
                                    .hasAnyRole("BUSINESS_ADMIN", "TECH_ADMIN", "CALCULATOR_USER");
                            // rest api for business and tech
                            spec.pathMatchers("/api/**")
                                    .hasAnyRole("BUSINESS_ADMIN", "TECH_ADMIN");
                            spec.anyExchange().authenticated();
                        }
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
                );

        return http.build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthoritiesFromRealmRoles);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    private Collection<GrantedAuthority> extractAuthoritiesFromRealmRoles(Jwt jwt) {
        Object realmAccessClaim = jwt.getClaims().get("realm_access");

        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return List.of();
        }

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(roleName -> "ROLE_" + roleName)   // TECH_ADMIN -> ROLE_TECH_ADMIN
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
