package com.emat.reapi.api.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record ApplicationData(
        String name,
        String version
) {

}
